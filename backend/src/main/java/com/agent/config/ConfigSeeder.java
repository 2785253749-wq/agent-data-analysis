package com.agent.config;

import com.agent.entity.AiModelEntity;
import com.agent.entity.PromptTemplateEntity;
import com.agent.repository.AiModelRepository;
import com.agent.repository.PromptTemplateRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Seeds initial admin config (default model + default prompts) at startup.
 *
 * Constraint 6: seed content is hard-coded here — it does NOT read prompt files
 * from the filesystem at deploy time. Runs only when the tables are empty (idempotent).
 */
@Component
public class ConfigSeeder {

    private final AiModelRepository modelRepo;
    private final PromptTemplateRepository promptRepo;

    public ConfigSeeder(AiModelRepository modelRepo, PromptTemplateRepository promptRepo) {
        this.modelRepo = modelRepo;
        this.promptRepo = promptRepo;
    }

    @PostConstruct
    public void seed() {
        seedDefaultModel();
        seedDefaultPrompts();
    }

    private void seedDefaultModel() {
        if (modelRepo.count() > 0) return;
        AiModelEntity m = new AiModelEntity();
        m.setName("DeepSeek V4 Pro");
        m.setProvider("deepseek");
        m.setBaseUrl("https://api.deepseek.com/v1");   // allowlisted
        m.setModelName("deepseek-v4-pro");
        m.setTimeoutMs(120000);
        m.setTemperature(0.0);
        m.setMaxTokens(2048);
        m.setApiKeyRef("DEEPSEEK_API_KEY");            // whitelist env var only
        m.setIsEnabled(true);
        m.setIsDefault(true);                          // global default
        modelRepo.save(m);
    }

    private void seedDefaultPrompts() {
        if (promptRepo.count() > 0) return;
        promptRepo.save(newPrompt("意图识别", PromptTemplateEntity.TYPE_INTENT, 1, INTENT_PROMPT, "识别用户分析意图"));
        promptRepo.save(newPrompt("SQL生成", PromptTemplateEntity.TYPE_SQL_GEN, 1, SQL_GEN_PROMPT, "根据意图生成只读SQL"));
        promptRepo.save(newPrompt("数据解读", PromptTemplateEntity.TYPE_INTERPRET, 1, INTERPRET_PROMPT, "基于查询结果生成解读"));
    }

    private PromptTemplateEntity newPrompt(String name, String type, int version, String content, String desc) {
        PromptTemplateEntity p = new PromptTemplateEntity();
        p.setName(name);
        p.setType(type);
        p.setVersion(version);
        p.setContent(content);
        p.setContentHash(hash(content));
        p.setDescription(desc);
        p.setIsEnabled(true);
        p.setIsArchived(false);
        return p;
    }

    private String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    // ---- Hard-coded seed prompts (do not read from filesystem) ----

    private static final String INTENT_PROMPT = """
            你是数据分析意图识别器。将用户问题解析为结构化意图JSON。
            intentType: query|aggregation|comparison|ranking|detail|correlation
            时间范围不明确时needsClarification=true。仅输出JSON。
            """;

    private static final String SQL_GEN_PROMPT = """
            你是SQL生成器。根据意图与元数据生成MySQL查询。
            硬约束：只生成SELECT/WITH...SELECT；禁止DDL/DML/注释/危险函数；
            只能使用提供的字段和表名；所有查询以LIMIT结尾；使用${param}命名参数。
            """;

    private static final String INTERPRET_PROMPT = """
            你是数据分析结果解读器。基于查询结果生成解释。
            硬约束：只基于提供的数据；每句结论附evidence；区分fact/inference/suggestion；
            数据不足时dataSufficient=false。仅输出JSON。
            """;
}
