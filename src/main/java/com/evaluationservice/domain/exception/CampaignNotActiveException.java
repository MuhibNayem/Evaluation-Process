package com.evaluationservice.domain.exception;

import com.evaluationservice.domain.value.CampaignId;

public class CampaignNotActiveException extends DomainException {

    public CampaignNotActiveException(CampaignId campaignId) {
        super("Campaign '%s' is not in an active state".formatted(campaignId.value()),
                "CAMPAIGN_NOT_ACTIVE");
    }
}
