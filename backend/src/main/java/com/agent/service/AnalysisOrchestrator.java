package com.agent.service;

import com.agent.dto.*;
import com.agent.entity.AnalysisTaskEntity;
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
    private final IntentRecognitionService intentService;
    private final SqlGenerationService sqlService;
    private final SqlSafetyService safetyService;
    private final QueryExecutionService executionService;
    private final ResultInterpretationService interpretationService;
    private final ChartRecommendationService chartService;
    private final ObjectMapper objectMapper;

    public AnalysisOrchestrator(
            AnalysisTaskRepository taskRepo,
            IntentRecognitionService intentService,
            SqlGenerationService sqlService,
            SqlSafetyService safetyService,
            QueryExecutionService executionService,
            ResultInterpretationService interpretationService,
            ChartRecommendationService chartService,
            ObjectMapper objectMapper) {
        this.taskRepo = taskRepo;
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
            long t1 = stepStart(task, "INTENT");
            intent = intentService.recognize(new IntentRequest(request.question(), request.datasetId()));
            task.setIntentJson(toJson(intent));
            steps.add(new AnalysisResponse.StepInfo("INTENT", "COMPLETED", stepEnd(t1)));
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
            long t2 = stepStart(task, "SQL_GEN");
            SqlGenerationRequest sqlReq = new SqlGenerationRequest(
                    request.question(), intent, request.datasetId());
            sqlResult = sqlService.generate(sqlReq);
            task.setSqlText(sqlResult.sql());
            steps.add(new AnalysisResponse.StepInfo("SQL_GEN", "COMPLETED", stepEnd(t2)));
            taskRepo.save(task);

            // M3: SQL Validation
            long t3 = stepStart(task, "SQL_VALIDATE");
            validation = safetyService.validate(sqlResult.sql(), request.datasetId());
            steps.add(new AnalysisResponse.StepInfo("SQL_VALIDATE",
                    validation.passed() ? "COMPLETED" : "FAILED", stepEnd(t3)));
            if (!validation.passed()) {
                throw new RuntimeException("SQL validation failed: " + validation.reason());
            }

            // M4: Query Execution
            long t4 = stepStart(task, "QUERY");
            queryResult = executionService.execute(validation.sanitizedSql(), sqlResult.parameters());
            task.setResultSummary(queryResult.summary());
            steps.add(new AnalysisResponse.StepInfo("QUERY", "COMPLETED", stepEnd(t4)));
            taskRepo.save(task);

            // M5: Interpretation
            long t5 = stepStart(task, "INTERPRET");
            interpretation = interpretationService.interpret(request.question(), queryResult);
            steps.add(new AnalysisResponse.StepInfo("INTERPRET", "COMPLETED", stepEnd(t5)));

            // M6: Chart Recommendation
            long t6 = stepStart(task, "CHART");
            chartSpec = chartService.recommend(queryResult, intent);
            steps.add(new AnalysisResponse.StepInfo("CHART", "COMPLETED", stepEnd(t6)));

            task.setStatus("COMPLETED");
        } catch (Exception e) {
            log.error("Analysis pipeline failed at step", e);
            error = e.getMessage();
            task.setStatus("FAILED");
            task.setErrorMessage(error);
        }

        task.setCompletedAt(LocalDateTime.now());
        taskRepo.save(task);

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
        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        return taskRepo.save(task);
    }

    private long stepStart(AnalysisTaskEntity task, String stepType) {
        return System.currentTimeMillis();
    }

    private long stepEnd(long startMs) {
        return System.currentTimeMillis() - startMs;
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
