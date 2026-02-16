package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.value.CampaignId;

import java.time.Instant;
import java.util.Objects;

/**
 * Per-campaign override for a system setting.
 * Allows a campaign to use a different value than the system-wide default.
 */
public class CampaignSettingOverride {

    private final CampaignId campaignId;
    private final String settingKey;
    private String settingValue;
    private String updatedBy;
    private Instant updatedAt;

    public CampaignSettingOverride(
            CampaignId campaignId,
            String settingKey,
            String settingValue,
            String updatedBy,
            Instant updatedAt) {
        this.campaignId = Objects.requireNonNull(campaignId, "Campaign ID cannot be null");
        this.settingKey = Objects.requireNonNull(settingKey, "Setting key cannot be null");
        this.settingValue = Objects.requireNonNull(settingValue, "Setting value cannot be null");
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    // --- Domain Behavior ---

    public void updateValue(String newValue, String updatedBy) {
        Objects.requireNonNull(newValue, "Setting value cannot be null");
        this.settingValue = newValue;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public int asInt() {
        return Integer.parseInt(settingValue);
    }

    public double asDouble() {
        return Double.parseDouble(settingValue);
    }

    public boolean asBoolean() {
        return Boolean.parseBoolean(settingValue);
    }

    public String asString() {
        return settingValue;
    }

    // --- Getters ---

    public CampaignId getCampaignId() {
        return campaignId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        CampaignSettingOverride that = (CampaignSettingOverride) o;
        return campaignId.equals(that.campaignId) && settingKey.equals(that.settingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(campaignId, settingKey);
    }
}
