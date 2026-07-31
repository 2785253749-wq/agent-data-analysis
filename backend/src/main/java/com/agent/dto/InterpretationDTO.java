package com.agent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Contract for DeepSeek data interpretation output.
 *
 * Per spec section 6.3: conclusions must be grounded in provided data,
 * each statement must have evidence, and fact/inference/suggestion must be distinguished.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InterpretationDTO(

        /** Main conclusion — a single-sentence summary of findings */
        String conclusion,

        /** Detailed interpretations, each with evidence */
        List<InterpretationPoint> points,

        /** Whether the data is sufficient for meaningful conclusions */
        boolean dataSufficient,

        /** Overall confidence level: high | medium | low */
        String confidence,

        /** Caveats / limitations of the analysis */
        List<String> caveats

) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InterpretationPoint(
            /** The statement being made */
            String statement,

            /** fact | inference | suggestion */
            String type,

            /** Supporting data evidence for this statement */
            String evidence,

            /** Numeric confidence for this point (0.0-1.0) */
            Double confidence
    ) {}
}
