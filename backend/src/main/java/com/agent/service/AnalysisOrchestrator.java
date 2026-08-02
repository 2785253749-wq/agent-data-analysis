package com.agent.service;

import com.agent.dto.*;
import com.agent.entity.AiModelEntity;
import com.agent.entity.AnalysisStepEntity;
import com.agent.entity.AnalysisTaskEntity;
import com.agent.entity.PromptTemplateEntity;
import com.agent.repository.AnalysisStepRepository;
import com.agent.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the full analysis pipeline: M1 → M2 → M3 → M4 → M5 → M6.
 *
 * Each step's result feeds into the next. SSE events are emitted for
 * frontend progress tracking. Failures at any step terminate the pipeline.
 */
@Service
public class AnalysisOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestrator.class);

    private final AnalysisTaskRepository taskRepo;
    private final AnalysisStepRepository stepRepo;
    private final ResultSnapshotService snapshotService;
    private final AuditLogService auditLogService;
    private final AiModelService modelService;
    private final PromptTemplateService promptService;
    private final FailureClassifier failureClassifier;
    private final IntentRecognitionService intentService;
    private final SqlGenerationService sqlService;
    private final SqlSafetyService safetyService;
    private final QueryExecutionService executionService;
    private final ResultInterpretationService interpretationService;
    private final ChartRecommendationService chartService;
    private final ObjectMapper objectMapper;

    public AnalysisOrchestrator(
            AnalysisTaskRepository taskRepo,
            AnalysisStepRepository stepRepo,
            ResultSnapshotService snapshotService,
            AuditLogService auditLogService,
            AiModelService modelService,
            PromptTemplateService promptService,
            FailureClassifier failureClassifier,
            IntentRecognitionService intentService,
            SqlGenerationService sqlService,
            SqlSafetyService safetyService,
            QueryExecutionService executionService,
            ResultInterpretationService interpretationService,
            ChartRecommendationService chartService,
            ObjectMapper objectMapper) {
        this.taskRepo = taskRepo;
        this.stepRepo = stepRepo;
        this.snapshotService = snapshotService;
        this.auditLogService = auditLogService;
        this.modelService = modelService;
        this.promptService = promptService;
        this.failureClassifier = failureClassifier;
        this.intentService = intentService;
        this.sqlService = sqlService;
        this.safetyService = safetyService;
        this.executionService = executionService;
        this.interpretationService = interpretationService;
        this.chartService = chartService;
        this.objectMapper = objectMapper;
    }

    /**
     * Run the full analysis pipeline and return the completed result.
     */
    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request) {
        AnalysisTaskEntity task = createTask(request);
        auditLogService.record("system", 0L, "ANALYSIS_SUBMIT", "ANALYSIS", task.getId(),
                "SUCCESS", null, java.util.Map.of("taskId", task.getId()));
        List<AnalysisResponse.StepInfo> steps = new ArrayList<>();
        String error = null;

        IntentDTO intent = null;
        SqlResultDTO sqlResult = null;
        SqlValidationResult validation = null;
        QueryResult queryResult = null;
        InterpretationDTO interpretation = null;
        ChartSpecDTO chartSpec = null;

        try {
            // M1: Intent Recognition
            IntentStep intentStep = stepStart(task, "INTENT", 1);
            intent = intentService.recognize(new IntentRequest(request.question(), request.datasetId()));
            task.setIntentJson(toJson(intent));
            steps.add(new AnalysisResponse.StepInfo("INTENT", "COMPLETED", stepEnd(intentStep)));
            stepComplete(intentStep, "COMPLETED", toJson(intent), null);
            taskRepo.save(task);

            // If needs clarification, stop here
            if (intent.needsClarification()) {
                task.setStatus("COMPLETED");
                task.setResultSummary("需要用户澄清问题：" + String.join("; ", intent.clarificationQuestions()));
                task.setCompletedAt(LocalDateTime.now());
                taskRepo.save(task);
                return buildResponse(task, intent, null, null, null, null, null, null, steps);
            }

            // M2: SQL Generation
            IntentStep sqlGenStep = stepStart(task, "SQL_GEN", 2);
            SqlGenerationRequest sqlReq = new SqlGenerationRequest(
                    request.question(), intent, request.datasetId());
            sqlResult = sqlService.generate(sqlReq);
            task.setSqlText(sqlResult.sql());
            steps.add(new AnalysisResponse.StepInfo("SQL_GEN", "COMPLETED", stepEnd(sqlGenStep)));
            stepComplete(sqlGenStep, "COMPLETED", toJson(sqlResult), null);
            taskRepo.save(task);

            // M3: SQL Validation
            IntentStep validStep = stepStart(task, "SQL_VALIDATE", 3);
            validation = safetyService.validate(sqlResult.sql(), request.datasetId());
            boolean valid = validation.passed();
            steps.add(new AnalysisResponse.StepInfo("SQL_VALIDATE",
                    valid ? "COMPLETED" : "FAILED", stepEnd(validStep)));
            stepComplete(validStep, valid ? "COMPLETED" : "FAILED", toJson(validation), null);
            if (!valid) {
                throw new RuntimeException("SQL validation failed: " + validation.reason());
            }

            // M4: Query Execution
            IntentStep queryStep = stepStart(task, "QUERY", 4);
            queryResult = executionService.execute(validation.sanitizedSql(), sqlResult.parameters());
            task.setResultSummary(queryResult.summary());
            steps.add(new AnalysisResponse.StepInfo("QUERY", "COMPLETED", stepEnd(queryStep)));
            stepComplete(queryStep, "COMPLETED", toJson(queryResult), null);
            taskRepo.save(task);

            // M5: Interpretation
            IntentStep interpretStep = stepStart(task, "INTERPRET", 5);
            interpretation = interpretationService.interpret(request.question(), queryResult);
            steps.add(new AnalysisResponse.StepInfo("INTERPRET", "COMPLETED", stepEnd(interpretStep)));
            stepComplete(interpretStep, "COMPLETED", toJson(interpretation), null);

            // M6: Chart Recommendation
            IntentStep chartStep = stepStart(task, "CHART", 6);
            chartSpec = chartService.recommend(queryResult, intent);
            steps.add(new AnalysisResponse.StepInfo("CHART", "COMPLETED", stepEnd(chartStep)));
            stepComplete(chartStep, "COMPLETED", toJson(chartSpec), null);

            task.setStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Analysis pipeline failed at step", e);
            error = e.getMessage();
            task.setStatus("FAILED");
            task.setErrorMessage(error);
        }

        task.setCompletedAt(LocalDateTime.now());
        // Persist a bounded, redacted snapshot (rows ≤ 200, params redacted, ≤ 1 MB).
        task.setResultJson(snapshotService.build(
                task.getId(),
                task.getIntentJson(),
                task.getSqlText(),
                sqlResult != null ? sqlResult.parameters() : null,
                validation != null && validation.passed(),
                validation != null ? validation.violations() : null,
                queryResult,
                interpretation != null ? toJson(interpretation) : null,
                chartSpec != null ? toJson(chartSpec) : null));
        taskRepo.save(task);

        if ("FAILED".equals(task.getStatus())) {
            auditLogService.record("system", 0L, "ANALYSIS_FAILED", "ANALYSIS", task.getId(),
                    "FAILED", null, java.util.Map.of("taskId", task.getId()));
        } else {
            auditLogService.record("system", 0L, "ANALYSIS_COMPLETED", "ANALYSIS", task.getId(),
                    "SUCCESS", null, java.util.Map.of("taskId", task.getId()));
        }

        return buildResponse(task, intent, sqlResult, validation, queryResult,
                interpretation, chartSpec, error, steps);
    }

    /**
     * Run analysis with SSE progress streaming.
     */
    public SseEmitter analyzeStream(AnalysisRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5 min timeout

        new Thread(() -> {
            try {
                AnalysisResponse result = analyze(request);
                emitter.send(SseEmitter.event()
                        .name("taskCompleted")
                        .data(result));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("taskFailed")
                            .data(Map.of("error", e.getMessage())));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();

        return emitter;
    }

    private AnalysisTaskEntity createTask(AnalysisRequest request) {
        AnalysisTaskEntity task = new AnalysisTaskEntity();
        task.setUserId(0L);
        task.setQuestion(request.question());
        task.setDatasetId(request.datasetId());
        task.setConversationId(request.conversationId());
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        return taskRepo.save(task);
    }

    private IntentStep stepStart(AnalysisTaskEntity task, String stepType, int order) {
        return new IntentStep(task.getId(), stepType, order, System.currentTimeMillis());
    }

    private long stepEnd(IntentStep step) {
        return System.currentTimeMillis() - step.startMs;
    }

    private void stepComplete(IntentStep step, String status, String outputJson, String error) {
        AnalysisStepEntity entity = new AnalysisStepEntity();
        entity.setTaskId(step.taskId);
        entity.setStepType(step.stepType);
        entity.setStepOrder(step.order);
        entity.setStatus(status);
        entity.setOutputJson(outputJson);
        entity.setErrorMessage(error);
        entity.setDurationMs(stepEnd(step));
        // Record real model + prompt (immutable) so history can replay exact versions.
        try {
            AiModelEntity model = modelService.activeDefault();
            entity.setModelName(model.getModelName());
        } catch (Exception e) {
            entity.setModelName("unknown");
        }
        entity.setPromptVersion(currentPromptVersion(step.stepType));
        if ("FAILED".equals(status)) {
            entity.setFailureCategory(failureClassifier.classify(step.stepType, error));
        }
        entity.setCompletedAt(java.time.LocalDateTime.now());
        stepRepo.save(entity);
    }

    /** Map a pipeline step to its prompt template type. */
    private String promptTypeForStep(String stepType) {
        return switch (stepType) {
            case "INTENT" -> PromptTemplateEntity.TYPE_INTENT;
            case "SQL_GEN" -> PromptTemplateEntity.TYPE_SQL_GEN;
            case "INTERPRET" -> PromptTemplateEntity.TYPE_INTERPRET;
            default -> null; // SQL_VALIDATE / QUERY / CHART have no LLM prompt
        };
    }

    private String currentPromptVersion(String stepType) {
        String type = promptTypeForStep(stepType);
        if (type == null) return "none";
        try {
            PromptTemplateEntity p = promptService.activeEntity(type);
            return p.getVersion() + ":" + p.getContentHash().substring(0, 8);
        } catch (Exception e) {
            return "v1";
        }
    }

    /** Per-step timing holder. */
    private static final class IntentStep {
        final Long taskId;
        final String stepType;
        final int order;
        final long startMs;
        IntentStep(Long taskId, String stepType, int order, long startMs) {
            this.taskId = taskId;
            this.stepType = stepType;
            this.order = order;
            this.startMs = startMs;
        }
    }

    private AnalysisResponse buildResponse(
            AnalysisTaskEntity task, IntentDTO intent, SqlResultDTO sql,
            SqlValidationResult validation, QueryResult queryResult,
            InterpretationDTO interpretation, ChartSpecDTO chartSpec,
            String error, List<AnalysisResponse.StepInfo> steps) {
        return new AnalysisResponse(
                task.getId(), task.getQuestion(), task.getStatus(),
                intent, sql, validation, queryResult, interpretation, chartSpec,
                error, steps,
                task.getCreatedAt(), task.getCompletedAt());
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { return null; }
    }
}
