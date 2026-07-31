package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Contract for DeepSeek intent recognition output.
 *
 * Given a natural language question, the model returns structured intent JSON
 * that drives subsequent SQL generation and analysis steps.
 *
 * Per spec section 6.1: if time range is ambiguous, set needsClarification=true
 * rather than guessing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record IntentDTO(

        /** 意图类型: 查询(query) | 聚合(aggregation) | 对比(comparison) | 排行(ranking) | 明细(detail) | 关联(correlation) */
        String intentType,

        /** 用户想要查询的指标列表 */
        List<String> metrics,

        /** 用户想要分析的维度列表（GROUP BY 字段） */
        List<String> dimensions,

        /** 用户指定的过滤条件 */
        List<FilterDef> filters,

        /** 时间范围：null 表示无时间限制，模糊时设为 needsClarification=true */
        TimeRangeDef timeRange,

        /** 对比类型，如"同比增长"、"环比"，null 表示无对比需求 */
        String comparison,

        /** 当时间范围、指标等不明确时，设为 true，不应猜测 */
        boolean needsClarification,

        /** 当 needsClarification=true 时，需要向用户确认的问题 */
        List<String> clarificationQuestions

) {
    /**
     * A single filter condition extracted from user question.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FilterDef(
            String field,
            String operator,   // =, !=, >, <, >=, <=, like, in, between, is_null, is_not_null
            String value,
            String value2      // for BETWEEN operator
    ) {}

    /**
     * Time range extracted from user question.
     * Both start and end are ISO-8601 format (or null if open-ended).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TimeRangeDef(
            String type,       // absolute | relative | latest_n_days | latest_n_months | year_to_date | month_to_date
            String start,      // ISO-8601 or null for open-ended
            String end         // ISO-8601 or null for open-ended
    ) {}
}
