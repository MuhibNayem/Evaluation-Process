package com.evaluationservice.api.dto.request;

import java.util.List;

public record UpdateEvaluationRequest(
        List<AnswerRequest> answers) {
}
