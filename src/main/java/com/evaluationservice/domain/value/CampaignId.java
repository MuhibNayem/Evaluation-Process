package com.evaluationservice.domain.value;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for an evaluation campaign.
 */
public record CampaignId(String value) {

    public CampaignId {
        Objects.requireNonNull(value, "CampaignId cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("CampaignId cannot be blank");
        }
    }

    public static CampaignId generate() {
        return new CampaignId(UUID.randomUUID().toString());
    }

    public static CampaignId of(String value) {
        return new CampaignId(value);
    }
}
