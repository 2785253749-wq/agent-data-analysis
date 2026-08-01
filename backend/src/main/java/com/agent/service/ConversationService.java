package com.agent.service;

import com.agent.dto.*;
import com.agent.entity.AnalysisTaskEntity;
import com.agent.entity.ConversationEntity;
import com.agent.exception.ResourceNotFoundException;
import com.agent.repository.AnalysisTaskRepository;
import com.agent.repository.ConversationRepository;
import com.agent.repository.DatasetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Multi-turn conversation CRUD + follow-up message flow.
 *
 * Rules:
 * - tasks-as-turns: no separate message table; user msg = task.question, assistant = task.result_json.
 * - dataset switch rejected once taskCount > 0 (would pollute context).
 * - context updated only on COMPLETED; taskCount counts ALL created tasks.
 * - context redacted via ConversationContextService.
 * - isolation via UserAccessContext (owner-only, 404 otherwise).
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository convRepo;
    private final AnalysisTaskRepository taskRepo;
    private final DatasetRepository datasetRepo;
    private final UserAccessContext access;
    private final ConversationContextService contextService;
    private final AnalysisOrchestrator orchestrator;

    public ConversationService(ConversationRepository convRepo,
                               AnalysisTaskRepository taskRepo,
                               DatasetRepository datasetRepo,
                               UserAccessContext access,
                               ConversationContextService contextService,
                               AnalysisOrchestrator orchestrator) {
        this.convRepo = convRepo;
        this.taskRepo = taskRepo;
        this.datasetRepo = datasetRepo;
        this.access = access;
        this.contextService = contextService;
        this.orchestrator = orchestrator;
    }

    // ---- CRUD ----

    public PagedResponse<ConversationSummaryDTO> list(String status, int page, int size) {
        String st = (status == null || status.isBlank()) ? "ACTIVE" : status;
        Page<ConversationEntity> result = convRepo.findByUserIdAndStatus(
                access.currentUserId(), st, PageRequest.of(page, size));
        return PagedResponse.from(result.map(this::toSummary));
    }

    public ConversationDetailDTO detail(Long id) {
        ConversationEntity c = findOwned(id);
        List<AnalysisTaskEntity> tasks = taskRepo.findByConversationIdOrderByCreatedAtAsc(id);
        List<ConversationDetailDTO.TurnDTO> turns = new ArrayList<>();
        for (AnalysisTaskEntity t : tasks) {
            turns.add(new ConversationDetailDTO.TurnDTO(
                    t.getId(), t.getQuestion(), t.getStatus(), durationMs(t), t.getCreatedAt()));
        }
        return new ConversationDetailDTO(
                c.getId(), c.getTitle(), c.getStatus(), c.getDatasetId(), c.getTaskCount(),
                contextService.parse(c.getContextSummary()), turns,
                c.getCreatedAt(), c.getUpdatedAt());
    }

    @Transactional
    public ConversationSummaryDTO create(ConversationRequest req) {
        ConversationEntity c = new ConversationEntity();
        c.setUserId(access.currentUserId());
        c.setTitle(req.title());
        c.setDatasetId(req.datasetId());
        c.setStatus("ACTIVE");
        c.setTaskCount(0);
        return toSummary(convRepo.save(c));
    }

    @Transactional
    public ConversationSummaryDTO update(Long id, ConversationRequest req) {
        ConversationEntity c = findOwned(id);
        if (!c.getTitle().equals(req.title())) {
            c.setTitle(req.title());
        }
        // 补充点2: reject dataset switch once tasks exist
        if (req.datasetId() != null && c.getTaskCount() > 0
                && !req.datasetId().equals(c.getDatasetId())) {
            throw new IllegalArgumentException("该会话已有分析任务，不能切换数据集，请新建会话");
        }
        if (req.datasetId() != null) {
            c.setDatasetId(req.datasetId());
        }
        return toSummary(convRepo.save(c));
    }

    @Transactional
    public void archive(Long id) {
        ConversationEntity c = findOwned(id);
        c.setStatus("ARCHIVED");
        convRepo.save(c);
    }

    /**
     * Follow-up question within a conversation (补充点1: task-as-turn).
     * Injects context summary into the analysis, still passes existing SQL safety validation.
     */
    @Transactional
    public AnalysisResponse message(Long id, ConversationMessageRequest req) {
        ConversationEntity c = findOwned(id);
        if ("ARCHIVED".equals(c.getStatus())) {
            throw new IllegalArgumentException("会话已归档，不能继续提问");
        }

        Long datasetId = req.datasetId() != null ? req.datasetId() : c.getDatasetId();

        // Build enriched question with context summary injected (bounded, redacted).
        String enriched = buildEnrichedQuestion(c, req.question());

        AnalysisRequest analysisReq = new AnalysisRequest(enriched, datasetId, id);
        AnalysisResponse result = orchestrator.analyze(analysisReq);

        // taskCount counts ALL created tasks (补充点5)
        c.setTaskCount(c.getTaskCount() + 1);
        convRepo.save(c);

        // Context updates only when COMPLETED (补充点5)
        if ("COMPLETED".equals(result.status())) {
            String lastConclusion = result.interpretation() != null
                    ? result.interpretation().conclusion() : result.intent() != null
                    ? String.join(";", result.intent().metrics()) : null;
            Map<String, Object> ctx = contextService.mergeCompletedIntent(
                    contextService.parse(c.getContextSummary()),
                    result.intent(), datasetId,
                    datasetRepo.findById(datasetId).map(d -> d.getName()).orElse(null),
                    lastConclusion);
            c.setContextSummary(contextService.toJson(ctx));
            convRepo.save(c);
        }

        return result;
    }

    // ---- Helpers ----

    private ConversationEntity findOwned(Long id) {
        ConversationEntity c = convRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", id));
        if (!c.getUserId().equals(access.currentUserId())) {
            throw new ResourceNotFoundException("Conversation", id);
        }
        return c;
    }

    private ConversationSummaryDTO toSummary(ConversationEntity c) {
        return new ConversationSummaryDTO(
                c.getId(), c.getTitle(), c.getStatus(), c.getDatasetId(),
                c.getTaskCount(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private Long durationMs(AnalysisTaskEntity t) {
        if (t.getStartedAt() == null || t.getCompletedAt() == null) return null;
        return java.time.Duration.between(t.getStartedAt(), t.getCompletedAt()).toMillis();
    }

    /**
     * Inject bounded context (last conclusion + metrics/dimensions/timeRange) into the follow-up.
     * Does NOT prepend full history.
     */
    private String buildEnrichedQuestion(ConversationEntity c, String question) {
        Map<String, Object> ctx = contextService.parse(c.getContextSummary());
        StringBuilder sb = new StringBuilder();
        sb.append("追问问题：").append(question).append("\n\n");
        if (!ctx.isEmpty()) {
            sb.append("会话上下文（仅摘要）：\n");
            ctx.forEach((k, v) -> {
                if (v != null && !String.valueOf(v).isBlank()) {
                    sb.append("  ").append(k).append(": ").append(v).append("\n");
                }
            });
        }
        return sb.toString();
    }
}
