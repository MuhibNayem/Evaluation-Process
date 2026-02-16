package com.evaluationservice.infrastructure.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * JPA entity for campaign-level setting overrides.
 * Composite PK: (campaignId, settingKey).
 */
@Entity
@Table(name = "campaign_setting_overrides")
@IdClass(CampaignSettingOverrideEntity.CampaignSettingId.class)
public class CampaignSettingOverrideEntity {

    @Id
    @Column(name = "campaign_id", length = 36)
    private String campaignId;

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CampaignSettingOverrideEntity() {
    }

    // --- Composite ID class ---

    public static class CampaignSettingId implements Serializable {
        private String campaignId;
        private String settingKey;

        public CampaignSettingId() {
        }

        public CampaignSettingId(String campaignId, String settingKey) {
            this.campaignId = campaignId;
            this.settingKey = settingKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            CampaignSettingId that = (CampaignSettingId) o;
            return Objects.equals(campaignId, that.campaignId) &&
                    Objects.equals(settingKey, that.settingKey);
        }

        @Override
        public int hashCode() {
            return Objects.hash(campaignId, settingKey);
        }
    }

    // --- Getters and Setters ---

    public String getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(String campaignId) {
        this.campaignId = campaignId;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
