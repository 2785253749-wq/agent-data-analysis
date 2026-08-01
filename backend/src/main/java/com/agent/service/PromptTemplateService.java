package com.agent.service;

import com.agent.dto.PromptTemplateDTO;
import com.agent.entity.PromptTemplateEntity;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.PromptTemplateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

/**
 * Prompt template management.
 *
 * 1. Regular users never see prompt content/variables — active endpoint returns metadata only.
 * 5. Versions immutable: content/variables edit creates a NEW version; old versions only
 *    enable/disable/archive. Analysis steps record templateId + version + contentHash.
 * 6. Cannot disable the LAST enabled prompt of a REQUIRED type.
 */
@Service
public class PromptTemplateService {

    private final PromptTemplateRepository repo;

    public PromptTemplateService(PromptTemplateRepository repo) {
        this.repo = repo;
    }

    public Page<PromptTemplateDTO> list(String type, int page, int size) {
        Page<PromptTemplateEntity> result = (type == null || type.isBlank())
                ? repo.findAll(PageRequest.of(page, size))
                : repo.findByType(type, PageRequest.of(page, size));
        return result.map(this::toDTO);
    }

    /** Active prompt metadata for a type — regular users see name/version only, not content. */
    public PromptTemplateDTO activeMeta(String type) {
        return repo.findFirstByTypeAndIsEnabledTrue(type)
                .map(p -> new PromptTemplateDTO(
                        p.getId(), p.getName(), p.getType(), p.getVersion(), null, null,
                        p.getContentHash(), p.getDescription(), true, false, p.getCreatedAt(), p.getUpdatedAt()))
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate(active:" + type + ")", 0L));
    }

    /** Active prompt content for the orchestrator (server-side only, never exposed via API). */
    public PromptTemplateEntity activeEntity(String type) {
        return repo.findFirstByTypeAndIsEnabledTrue(type)
                .orElseThrow(() -> new ResourceNotFoundException("PromptTemplate(active:" + type + ")", 0L));
    }

    /**
     * Create a new version. Content/variables are immutable after creation.
     * version = current max for the type + 1.
     */
    @Transactional
    public PromptTemplateDTO create(PromptTemplateDTO.CreateRequest req) {
        int nextVersion = repo.findFirstByTypeOrderByVersionDesc(req.type())
                .map(p -> p.getVersion() + 1).orElse(1);

        PromptTemplateEntity p = new PromptTemplateEntity();
        p.setName(req.name());
        p.setType(req.type());
        p.setVersion(nextVersion);
        p.setContent(req.content());
        p.setVariables(req.variables());
        p.setContentHash(hash(req.content()));
        p.setDescription(req.description());
        p.setIsEnabled(false);   // new versions never auto-enabled
        p.setIsArchived(false);
        return toDTO(repo.save(p));
    }

    /** Immutable content → only description can be edited on an existing version. */
    @Transactional
    public PromptTemplateDTO updateMeta(Long id, String description) {
        PromptTemplateEntity p = find(id);
        if (description != null) p.setDescription(description);
        return toDTO(repo.save(p));
    }

    @Transactional
    public PromptTemplateDTO enable(Long id) {
        PromptTemplateEntity p = find(id);
        if (Boolean.TRUE.equals(p.getIsArchived())) {
            throw new IllegalArgumentException("已归档模板不能启用");
        }
        // Disable other versions of the same type (constraint: one enabled per type).
        repo.findByTypeAndIsEnabledTrue(p.getType()).forEach(other -> {
            if (!other.getId().equals(p.getId())) {
                other.setIsEnabled(false);
                repo.save(other);
            }
        });
        p.setIsEnabled(true);
        return toDTO(repo.save(p));
    }

    @Transactional
    public PromptTemplateDTO disable(Long id) {
        PromptTemplateEntity p = find(id);
        if (!Boolean.TRUE.equals(p.getIsEnabled())) return toDTO(p);
        // Constraint 6: cannot disable the last enabled prompt of a REQUIRED type.
        if (PromptTemplateEntity.REQUIRED_TYPES.contains(p.getType())) {
            long enabledCount = repo.findByTypeAndIsEnabledTrue(p.getType()).size();
            if (enabledCount <= 1) {
                throw new IllegalArgumentException("不能停用该类型的最后一个启用模板");
            }
        }
        p.setIsEnabled(false);
        return toDTO(repo.save(p));
    }

    @Transactional
    public void archive(Long id) {
        PromptTemplateEntity p = find(id);
        if (Boolean.TRUE.equals(p.getIsEnabled())) {
            // Archiving an enabled version: try to fall back to another version, else reject.
            List<PromptTemplateEntity> sameType = repo.findByTypeOrderByVersionDesc(p.getType());
            boolean anotherEnabled = sameType.stream()
                    .anyMatch(o -> Boolean.TRUE.equals(o.getIsEnabled()) && !o.getId().equals(p.getId()));
            if (PromptTemplateEntity.REQUIRED_TYPES.contains(p.getType()) && !anotherEnabled) {
                throw new IllegalArgumentException("不能归档该类型的唯一启用模板");
            }
            p.setIsEnabled(false);
        }
        p.setIsArchived(true);
        repo.save(p);
    }

    private PromptTemplateEntity find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("PromptTemplate", id));
    }

    private String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    private PromptTemplateDTO toDTO(PromptTemplateEntity p) {
        return new PromptTemplateDTO(
                p.getId(), p.getName(), p.getType(), p.getVersion(),
                p.getContent(), p.getVariables(), p.getContentHash(),
                p.getDescription(), p.getIsEnabled(), p.getIsArchived(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
