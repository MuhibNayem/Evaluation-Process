package com.evaluationservice.domain.exception;

public class DuplicateSubmissionException extends DomainException {

    public DuplicateSubmissionException(String evaluatorId, String evaluateeId, String campaignId) {
        super("Evaluator '%s' has already submitted an evaluation for evaluatee '%s' in campaign '%s'"
                .formatted(evaluatorId, evaluateeId, campaignId),
                "DUPLICATE_SUBMISSION");
    }
}
