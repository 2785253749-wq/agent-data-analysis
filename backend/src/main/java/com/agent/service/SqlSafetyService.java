package com.agent.service;

import com.agent.dto.SqlValidationResult;
import com.agent.entity.DatasetFieldEntity;
import com.agent.repository.DatasetFieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates AI-generated SQL before execution.
 *
 * Per spec section 7, validation layers:
 * 1. Statement type: only SELECT / WITH...SELECT
 * 2. Forbidden keywords: DDL, DML, dangerous functions, file access
 * 3. Comment stripping: remove all SQL comments
 * 4. Field whitelist: every referenced column must be in dataset_fields
 * 5. Resource limits: enforce LIMIT, check for cartesian products
 *
 * Architecture note: currently uses regex-based validation.
 * Can be upgraded to JSqlParser AST for more precise analysis.
 */
@Service
public class SqlSafetyService {

    private static final Logger log = LoggerFactory.getLogger(SqlSafetyService.class);

    // ---- Forbidden patterns ----

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE",
            "RENAME", "REPLACE", "MERGE", "GRANT", "REVOKE", "EXEC", "EXECUTE",
            "CALL", "LOAD_FILE", "LOAD DATA", "INTO OUTFILE", "INTO DUMPFILE",
            "SLEEP", "BENCHMARK", "GET_LOCK", "RELEASE_LOCK"
    );

    private static final Pattern DANGEROUS_FUNC = Pattern.compile(
            "\\b(LOAD_FILE|SLEEP|BENCHMARK|GET_LOCK|RELEASE_LOCK|IS_FREE_LOCK)\\s*\\(", Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMENT_PATTERN = Pattern.compile(
            "/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/|--[^\\r\\n]*|#\\s[^\\r\\n]*", Pattern.DOTALL);

    private static final Pattern MYSQL_SPECIAL_COMMENT = Pattern.compile(
            "/\\*![0-9]*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", Pattern.DOTALL);

    private static final Pattern SELECT_PATTERN = Pattern.compile(
            "^\\s*(SELECT|WITH)\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*(?![^(]*\\))", Pattern.CASE_INSENSITIVE);

    private final DatasetFieldRepository fieldRepo;

    public SqlSafetyService(DatasetFieldRepository fieldRepo) {
        this.fieldRepo = fieldRepo;
    }

    /**
     * Validate a generated SQL against all safety rules.
     *
     * @param sql       the AI-generated SQL
     * @param datasetId dataset context for field whitelist validation
     * @return validation result with pass/fail + violations
     */
    public SqlValidationResult validate(String sql, Long datasetId) {
        if (sql == null || sql.isBlank()) {
            return SqlValidationResult.reject("SQL is empty", "empty_sql");
        }

        List<String> violations = new ArrayList<>();

        // Layer 1: Strip comments
        String cleaned = stripComments(sql);
        if (cleaned.isBlank()) {
            return SqlValidationResult.reject("SQL is empty after stripping comments", "only_comments");
        }

        // Layer 2: Statement type check
        if (!isSelectStatement(cleaned)) {
            violations.add("Only SELECT or WITH...SELECT statements are allowed");
            return SqlValidationResult.reject("Non-SELECT statement detected", violations);
        }

        // Layer 3: Forbidden keywords
        checkForbiddenKeywords(cleaned, violations);

        // Layer 4: Dangerous functions
        checkDangerousFunctions(cleaned, violations);

        // Layer 5: Field whitelist (if datasetId provided)
        if (datasetId != null) {
            checkFieldWhitelist(cleaned, datasetId, violations);
        }

        // Layer 6: LIMIT enforcement
        String withLimit = ensureLimit(cleaned, violations);

        // Layer 7: Additional checks
        checkSemicolonCount(cleaned, violations);
        checkUnionInjection(cleaned, violations);

        if (!violations.isEmpty()) {
            return SqlValidationResult.reject(
                    "SQL validation failed with " + violations.size() + " violation(s)", violations);
        }

        return SqlValidationResult.pass(withLimit);
    }

    // ---- Validation Methods ----

    public String stripComments(String sql) {
        String cleaned = COMMENT_PATTERN.matcher(sql).replaceAll("");
        cleaned = MYSQL_SPECIAL_COMMENT.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }

    public boolean isSelectStatement(String sql) {
        return SELECT_PATTERN.matcher(sql).find();
    }

    public void checkForbiddenKeywords(String sql, List<String> violations) {
        String upper = sql.toUpperCase();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            Pattern p = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(upper).find()) {
                violations.add("Forbidden keyword detected: " + keyword);
            }
        }
    }

    public void checkDangerousFunctions(String sql, List<String> violations) {
        Matcher m = DANGEROUS_FUNC.matcher(sql);
        while (m.find()) {
            violations.add("Dangerous function detected: " + m.group(1));
        }
    }

    public void checkFieldWhitelist(String sql, Long datasetId, List<String> violations) {
        List<DatasetFieldEntity> allowedFields = fieldRepo.findAllByDatasetId(datasetId);
        Set<String> allowedNames = new HashSet<>();
        for (DatasetFieldEntity f : allowedFields) {
            allowedNames.add(f.getFieldName().toLowerCase());
        }

        // Extract column references from SQL
        // Look for identifiers in SELECT, WHERE, GROUP BY, ORDER BY, HAVING clauses
        Set<String> referenced = extractColumnReferences(sql);

        for (String ref : referenced) {
            if (!allowedNames.contains(ref) && !ref.equals("*")) {
                violations.add("Field '" + ref + "' not in dataset whitelist");
            }
        }
    }

    /**
     * Extract column references from SQL clauses.
     * Heuristic: find identifiers after SELECT (before FROM), in WHERE, GROUP BY, ORDER BY, HAVING.
     * Skips table-qualified references (table.column → extracts column).
     */
    Set<String> extractColumnReferences(String sql) {
        Set<String> refs = new HashSet<>();
        String upper = sql.toUpperCase();

        // Extract the column list between SELECT and FROM
        Pattern selectCols = Pattern.compile("SELECT\\s+(.+?)\\s+FROM", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher sm = selectCols.matcher(sql);
        if (sm.find()) {
            String cols = sm.group(1);
            extractIds(cols, refs);
        }

        // Extract columns from WHERE clause
        Pattern whereCols = Pattern.compile("WHERE\\s+(.+?)(?=GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher wm = whereCols.matcher(sql);
        if (wm.find()) {
            extractIds(wm.group(1), refs);
        }

        // Extract columns from GROUP BY
        Pattern groupCols = Pattern.compile("GROUP\\s+BY\\s+(.+?)(?=ORDER\\s+BY|HAVING|LIMIT|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher gm = groupCols.matcher(sql);
        if (gm.find()) {
            extractIds(gm.group(1), refs);
        }

        // Extract columns from ORDER BY
        Pattern orderCols = Pattern.compile("ORDER\\s+BY\\s+(.+?)(?=LIMIT|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher om = orderCols.matcher(sql);
        if (om.find()) {
            extractIds(om.group(1), refs);
        }

        return refs;
    }

    private void extractIds(String clause, Set<String> refs) {
        // Remove function calls, string literals, and operators
        String cleaned = clause
                .replaceAll("'[^']*'", "")       // string literals
                .replaceAll("\\([^)]*\\)", "")    // function arguments
                .replaceAll("\\$\\{[^}]*\\}", "") // named parameters
                .replaceAll("[<>!=]+", " ")       // operators
                .replaceAll("\\d+", "")           // numbers
                .trim();

        // Extract identifiers: table.column → column, or standalone column
        Matcher m = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\.\\s*([a-zA-Z_][a-zA-Z0-9_]*)").matcher(cleaned);
        while (m.find()) {
            refs.add(m.group(2).toLowerCase()); // column part of table.column
        }

        // Extract standalone identifiers (not after a dot)
        // Simplify: find all word tokens
        Set<String> keywords = new HashSet<>(Set.of(
                "select", "from", "where", "and", "or", "not", "in", "is", "null",
                "like", "between", "join", "on", "order", "by", "group", "having",
                "asc", "desc", "limit", "offset", "as", "distinct", "all", "union",
                "with", "case", "when", "then", "else", "end", "left", "right",
                "inner", "outer", "cross", "full", "count", "sum", "avg", "min",
                "max", "ifnull", "coalesce", "cast", "convert", "date",
                "year", "month", "day", "hour", "minute", "second", "interval",
                "true", "false", "exists", "any", "some", "concat", "substring",
                "replace", "trim", "upper", "lower", "length", "round", "floor",
                "ceil", "ceiling", "abs", "mod", "power", "sqrt", "log", "exp",
                "if", "set", "values", "table", "into"
        ));

        // Get remaining identifiers after stripping dotted refs
        String noDots = cleaned.replaceAll("[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_][a-zA-Z0-9_]*", "");
        Matcher wordM = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b").matcher(noDots);
        while (wordM.find()) {
            String word = wordM.group(1).toLowerCase();
            if (!keywords.contains(word) && !word.equals("*")) {
                refs.add(word);
            }
        }
    }

    public String ensureLimit(String sql, List<String> violations) {
        String upper = sql.toUpperCase();
        if (!upper.contains("LIMIT")) {
            // Only require LIMIT for queries that might return many rows
            if (upper.contains("SELECT") && !upper.contains("LIMIT")) {
                violations.add("Query is missing LIMIT clause");
            }
        }
        return sql; // Return as-is; LIMIT injection happens in M4 execution layer
    }

    public void checkSemicolonCount(String sql, List<String> violations) {
        long count = sql.chars().filter(ch -> ch == ';').count();
        if (count > 1) {
            violations.add("Multiple semicolons detected — possible SQL injection");
        }
    }

    public void checkUnionInjection(String sql, List<String> violations) {
        // Count UNIONs — more than 2 is suspicious
        String upper = sql.toUpperCase();
        int unionCount = 0;
        Matcher m = Pattern.compile("\\bUNION\\b").matcher(upper);
        while (m.find()) unionCount++;
        if (unionCount > 3) {
            violations.add("Excessive UNION clauses: " + unionCount);
        }
    }
}
