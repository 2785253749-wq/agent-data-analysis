package com.agent.service;

import com.agent.entity.DatasetFieldEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks sensitive BUSINESS data (PII) before it enters context_summary / reports.
 *
 * This is distinct from {@link ErrorMessageSanitizer}, which handles exception
 * messages only. Business values from fields marked is_sensitive must never
 * appear verbatim in persisted summaries.
 *
 * Two layers:
 *  1. Generic PII format masking (phone / email / id-card / account).
 *  2. Field-name-driven masking: if a sensitive field's name/alias appears in the
 *     text (e.g. "手机号: 13812345678" or "phone = 138..."), mask the adjacent value.
 */
@Component
public class SensitiveDataMasker {

    private static final String MASK = "***";

    // 1[3-9]xxxxxxxxx
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    // email
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    // 18-digit id card (15/18)
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)|(?<!\\d)\\d{15}(?!\\d)");
    // account / bank card: 16-19 digits
    private static final Pattern ACCOUNT = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

    /**
     * Mask a business text using the dataset's sensitive fields.
     *
     * @param text            raw text (e.g. a conclusion)
     * @param sensitiveFields fields whose values are considered sensitive (isSensitive=true)
     * @return masked text
     */
    public String mask(String text, List<DatasetFieldEntity> sensitiveFields) {
        if (text == null || text.isBlank()) return text;

        String out = text;

        // Layer 1: generic PII formats — always applied regardless of field list.
        out = PHONE.matcher(out).replaceAll(MASK);
        out = EMAIL.matcher(out).replaceAll(MASK);
        out = ID_CARD.matcher(out).replaceAll(MASK);
        out = ACCOUNT.matcher(out).replaceAll(MASK);

        // Layer 2: field-driven — only for fields explicitly marked sensitive.
        if (sensitiveFields != null) {
            for (DatasetFieldEntity f : sensitiveFields) {
                if (f.getIsSensitive() == null || !f.getIsSensitive()) continue;
                out = maskFieldValue(out, f.getFieldName());
                if (f.getFieldAlias() != null && !f.getFieldAlias().isBlank()) {
                    out = maskFieldValue(out, f.getFieldAlias());
                }
            }
        }

        return out;
    }

    /** Mask the value that follows a sensitive field name/alias in the text. */
    private String maskFieldValue(String text, String fieldName) {
        if (fieldName == null || fieldName.isBlank()) return text;
        String escaped = Pattern.quote(fieldName.trim());
        // Match "fieldName : value" or "fieldName=value" or "fieldName value"
        Pattern p = Pattern.compile("(?i)(" + escaped + ")\\s*[:：=]?\\s*([A-Za-z0-9._%+@-]{4,})");
        Matcher m = p.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            // Keep the field label, mask the value.
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1)) + ": " + MASK);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Convenience: sensitive fields from a field list. */
    public static List<DatasetFieldEntity> sensitiveOf(List<DatasetFieldEntity> all) {
        if (all == null) return List.of();
        return all.stream().filter(f -> Boolean.TRUE.equals(f.getIsSensitive())).toList();
    }
}
