package com.evaluationservice.domain.exception;

import com.evaluationservice.domain.enums.CampaignStatus;

public class InvalidStateTransitionException extends DomainException {

    public InvalidStateTransitionException(String entityType, String currentState, String targetState) {
        super("Cannot transition %s from '%s' to '%s'".formatted(entityType, currentState, targetState),
                "INVALID_STATE_TRANSITION");
    }

    public static InvalidStateTransitionException forCampaign(CampaignStatus current, CampaignStatus target) {
        return new InvalidStateTransitionException("Campaign", current.name(), target.name());
    }
}
