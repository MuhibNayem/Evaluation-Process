package com.evaluationservice.domain.entity;

import com.evaluationservice.domain.enums.SystemSettingCategory;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a system-wide configuration setting.
 * Admin-configurable at runtime and stored in the database.
 * Each setting has a unique key, a string value, and belongs to a category.
 */
public class SystemSetting {

    private final String settingKey;
    private String settingValue;
    private final SystemSettingCategory category;
    private String description;
    private String updatedBy;
    private Instant updatedAt;

    public SystemSetting(
            String settingKey,
            String settingValue,
            SystemSettingCategory category,
            String description,
            String updatedBy,
            Instant updatedAt) {
        this.settingKey = Objects.requireNonNull(settingKey, "Setting key cannot be null");
        this.settingValue = Objects.requireNonNull(settingValue, "Setting value cannot be null");
        this.category = Objects.requireNonNull(category, "Category cannot be null");
        this.description = description;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        if (settingKey.isBlank()) {
            throw new IllegalArgumentException("Setting key cannot be blank");
        }
    }

    // --- Domain Behavior ---

    /**
     * Updates the setting value. Records who made the change.
     */
    public void updateValue(String newValue, String updatedBy) {
        Objects.requireNonNull(newValue, "Setting value cannot be null");
        this.settingValue = newValue;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    /**
     * Parses the string value as the given type.
     */
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

    public String getSettingKey() {
        return settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public SystemSettingCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
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
        SystemSetting that = (SystemSetting) o;
        return settingKey.equals(that.settingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settingKey);
    }
}
