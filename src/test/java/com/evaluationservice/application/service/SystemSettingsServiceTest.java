package com.evaluationservice.application.service;

import com.evaluationservice.application.port.out.CampaignSettingsPersistencePort;
import com.evaluationservice.application.port.out.SystemSettingsPersistencePort;
import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.CampaignId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingsService")
class SystemSettingsServiceTest {

    @Mock
    private SystemSettingsPersistencePort systemSettingsPort;

    @Mock
    private CampaignSettingsPersistencePort campaignSettingsPort;

    private SystemSettingsService service;

    @BeforeEach
    void setUp() {
        service = new SystemSettingsService(systemSettingsPort, campaignSettingsPort);
    }

    @Nested
    @DisplayName("System-wide settings")
    class SystemSettings {

        @Test
        @DisplayName("getAllSettings returns all settings from port")
        void shouldReturnAllSettings() {
            var s1 = setting("scoring.default-method", "WEIGHTED_AVERAGE", SystemSettingCategory.SCORING);
            var s2 = setting("pagination.default-page-size", "20", SystemSettingCategory.PAGINATION);
            when(systemSettingsPort.findAll()).thenReturn(List.of(s1, s2));

            List<SystemSetting> result = service.getAllSettings();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("getSettingsByCategory filters by category")
        void shouldReturnByCategory() {
            var s1 = setting("scoring.default-method", "WEIGHTED_AVERAGE", SystemSettingCategory.SCORING);
            when(systemSettingsPort.findByCategory(SystemSettingCategory.SCORING))
                    .thenReturn(List.of(s1));

            List<SystemSetting> result = service.getSettingsByCategory(SystemSettingCategory.SCORING);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getSettingKey()).isEqualTo("scoring.default-method");
        }

        @Test
        @DisplayName("getSettingByKey throws EntityNotFoundException when not found")
        void shouldThrowWhenKeyNotFound() {
            when(systemSettingsPort.findByKey("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSettingByKey("nonexistent"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("updateSetting updates value and records who changed it")
        void shouldUpdateSetting() {
            var existing = setting("scoring.passing-score-threshold", "70.0", SystemSettingCategory.SCORING);
            when(systemSettingsPort.findByKey("scoring.passing-score-threshold"))
                    .thenReturn(Optional.of(existing));
            when(systemSettingsPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SystemSetting result = service.updateSetting(
                    "scoring.passing-score-threshold", "85.0", "admin-user");

            assertThat(result.getSettingValue()).isEqualTo("85.0");
            assertThat(result.getUpdatedBy()).isEqualTo("admin-user");
        }
    }

    @Nested
    @DisplayName("Campaign overrides")
    class CampaignOverrides {

        @Test
        @DisplayName("getCampaignOverrides returns all overrides for a campaign")
        void shouldReturnCampaignOverrides() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            var override = new CampaignSettingOverride(
                    campaignId, "scoring.passing-score-threshold", "90.0", "admin", Instant.now());
            when(campaignSettingsPort.findByCampaignId(campaignId)).thenReturn(List.of(override));

            List<CampaignSettingOverride> result = service.getCampaignOverrides(campaignId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("setCampaignOverride creates new override when none exists")
        void shouldCreateNewOverride() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            String key = "scoring.passing-score-threshold";

            when(systemSettingsPort.findByKey(key))
                    .thenReturn(Optional.of(setting(key, "70.0", SystemSettingCategory.SCORING)));
            when(campaignSettingsPort.findByCampaignIdAndKey(campaignId, key))
                    .thenReturn(Optional.empty());
            when(campaignSettingsPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CampaignSettingOverride result = service.setCampaignOverride(
                    campaignId, key, "85.0", "admin");

            assertThat(result.getSettingValue()).isEqualTo("85.0");
            assertThat(result.getCampaignId()).isEqualTo(campaignId);
        }

        @Test
        @DisplayName("setCampaignOverride updates existing override")
        void shouldUpdateExistingOverride() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            String key = "scoring.passing-score-threshold";
            var existing = new CampaignSettingOverride(
                    campaignId, key, "80.0", "admin", Instant.now());

            when(systemSettingsPort.findByKey(key))
                    .thenReturn(Optional.of(setting(key, "70.0", SystemSettingCategory.SCORING)));
            when(campaignSettingsPort.findByCampaignIdAndKey(campaignId, key))
                    .thenReturn(Optional.of(existing));
            when(campaignSettingsPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CampaignSettingOverride result = service.setCampaignOverride(
                    campaignId, key, "90.0", "admin");

            assertThat(result.getSettingValue()).isEqualTo("90.0");
        }

        @Test
        @DisplayName("setCampaignOverride throws when setting key does not exist")
        void shouldThrowWhenSettingKeyInvalid() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            when(systemSettingsPort.findByKey("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setCampaignOverride(campaignId, "nonexistent", "val", "admin"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("removeCampaignOverride delegates to persistence port")
        void shouldRemoveOverride() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            String key = "scoring.passing-score-threshold";

            service.removeCampaignOverride(campaignId, key);

            verify(campaignSettingsPort).deleteByCampaignIdAndKey(campaignId, key);
        }
    }

    // --- Helper ---

    private SystemSetting setting(String key, String value, SystemSettingCategory category) {
        return new SystemSetting(key, value, category, "desc", "system", Instant.now());
    }
}
