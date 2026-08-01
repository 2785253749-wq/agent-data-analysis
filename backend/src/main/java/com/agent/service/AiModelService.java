package com.agent.service;

import com.agent.dto.AiModelDTO;
import com.agent.dto.AiModelRequest;
import com.agent.entity.AiModelEntity;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.AiModelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * AI model configuration with hard security constraints.
 *
 * 2. Global default: only ONE enabled+default model system-wide.
 * 3. Base URL: HTTPS + allowlisted domains only (no SSRF / internal IP / localhost / arbitrary port).
 * 4. api_key_ref: whitelist env var names only (DEEPSEEK_API_KEY). Never expose key.
 */
@Service
public class AiModelService {

    /** Allowlisted env-var references for api_key_ref (constraint 4). */
    public static final Set<String> ALLOWED_KEY_REFS = Set.of("DEEPSEEK_API_KEY");

    /** Allowlisted base-URL host suffixes (constraint 3). */
    public static final Set<String> ALLOWED_HOST_SUFFIXES = Set.of(
            "api.deepseek.com", "deepseek.com");

    private final AiModelRepository repo;

    public AiModelService(AiModelRepository repo) {
        this.repo = repo;
    }

    public Page<AiModelDTO> list(int page, int size) {
        return repo.findAll(PageRequest.of(page, size)).map(this::toDTO);
    }

    /** Enabled models for regular users — DTO hides apiKeyRef. */
    public List<AiModelDTO> active() {
        return repo.findByIsEnabledTrueOrderByIdAsc().stream().map(this::toDTO).toList();
    }

    public AiModelDTO get(Long id) {
        return toDTO(find(id));
    }

    @Transactional
    public AiModelDTO create(AiModelRequest req) {
        validateBaseUrl(req.baseUrl());
        validateKeyRef(req.apiKeyRef());

        AiModelEntity m = new AiModelEntity();
        apply(m, req);
        // Global default: if this is the only/default request, clear others.
        if (Boolean.TRUE.equals(req.isDefault())) {
            clearDefault();
        }
        return toDTO(repo.save(m));
    }

    @Transactional
    public AiModelDTO update(Long id, AiModelRequest req) {
        validateBaseUrl(req.baseUrl());
        validateKeyRef(req.apiKeyRef());

        AiModelEntity m = find(id);
        apply(m, req);
        if (Boolean.TRUE.equals(req.isDefault())) {
            clearDefault();
            m.setIsDefault(true);
        }
        return toDTO(repo.save(m));
    }

    @Transactional
    public void delete(Long id) {
        AiModelEntity m = find(id);
        if (Boolean.TRUE.equals(m.getIsDefault())) {
            throw new IllegalArgumentException("默认模型不可删除");
        }
        repo.delete(m);
    }

    @Transactional
    public AiModelDTO setDefault(Long id) {
        AiModelEntity m = find(id);
        clearDefault();
        m.setIsDefault(true);
        m.setIsEnabled(true);
        return toDTO(repo.save(m));
    }

    /** The single active default model (used by orchestrator). */
    public AiModelEntity activeDefault() {
        return repo.findByIsEnabledTrueAndIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException("没有启用的默认模型配置"));
    }

    // ---- Constraint validators ----

    private void validateBaseUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("Base URL 必须使用 HTTPS");
            }
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Base URL 缺少域名");
            }
            // Reject IP / localhost / internal
            if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || "localhost".equalsIgnoreCase(host)
                    || host.endsWith(".local") || host.endsWith(".internal")) {
                throw new IllegalArgumentException("Base URL 禁止使用 IP、localhost 或内网地址");
            }
            // Allowlist host suffix
            boolean allowed = ALLOWED_HOST_SUFFIXES.stream().anyMatch(s -> host.equals(s) || host.endsWith("." + s));
            if (!allowed) {
                throw new IllegalArgumentException("Base URL 域名不在允许列表内");
            }
            // Port: only default 443 (no arbitrary ports)
            if (uri.getPort() != -1 && uri.getPort() != 443) {
                throw new IllegalArgumentException("Base URL 禁止自定义端口");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Base URL 格式不合法");
        }
    }

    private void validateKeyRef(String ref) {
        if (ref == null || !ALLOWED_KEY_REFS.contains(ref)) {
            throw new IllegalArgumentException("api_key_ref 仅允许白名单环境变量: " + ALLOWED_KEY_REFS);
        }
    }

    private void clearDefault() {
        repo.findAll().forEach(m -> {
            if (Boolean.TRUE.equals(m.getIsDefault())) {
                m.setIsDefault(false);
                repo.save(m);
            }
        });
    }

    private void apply(AiModelEntity m, AiModelRequest req) {
        m.setName(req.name());
        m.setProvider(req.provider());
        m.setBaseUrl(req.baseUrl());
        m.setModelName(req.modelName());
        m.setTimeoutMs(req.timeoutMs());
        m.setTemperature(req.temperature());
        m.setMaxTokens(req.maxTokens());
        m.setApiKeyRef(req.apiKeyRef());
        m.setIsEnabled(req.isEnabled() != null ? req.isEnabled() : true);
        if (Boolean.TRUE.equals(req.isDefault())) m.setIsDefault(true);
    }

    private AiModelEntity find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("AiModel", id));
    }

    private AiModelDTO toDTO(AiModelEntity m) {
        boolean keyConfigured = System.getenv(m.getApiKeyRef()) != null
                || System.getProperty(m.getApiKeyRef()) != null;
        return new AiModelDTO(
                m.getId(), m.getName(), m.getProvider(), m.getBaseUrl(), m.getModelName(),
                m.getTimeoutMs(), m.getTemperature(), m.getMaxTokens(),
                m.getIsEnabled(), m.getIsDefault(), keyConfigured,
                m.getCreatedAt(), m.getUpdatedAt());
    }
}
