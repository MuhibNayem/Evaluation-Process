package com.evaluationservice.application.service;

import com.evaluationservice.application.port.out.CampaignSettingsPersistencePort;
import com.evaluationservice.application.port.out.SystemSettingsPersistencePort;
import com.evaluationservice.domain.entity.CampaignSettingOverride;
import com.evaluationservice.domain.entity.SystemSetting;
import com.evaluationservice.domain.enums.SystemSettingCategory;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsResolverService")
class SettingsResolverServiceTest {

    @Mock
    private SystemSettingsPersistencePort systemSettingsPort;

    @Mock
    private CampaignSettingsPersistencePort campaignSettingsPort;

    private EvaluationServiceProperties defaultProperties;
    private SettingsResolverService resolver;

    @BeforeEach
    void setUp() {
        defaultProperties = new EvaluationServiceProperties();
        // Set known defaults
        defaultProperties.getScoring().setPassingScoreThreshold(70.0);
        defaultProperties.getPagination().setDefaultPageSize(20);
        defaultProperties.getPagination().setMaxPageSize(100);
        defaultProperties.getCampaign().setDefaultMinimumRespondents(1);
        defaultProperties.getFeatures().setEnableReports(true);
        resolver = new SettingsResolverService(systemSettingsPort, campaignSettingsPort, defaultProperties);
    }

    @Nested
    @DisplayName("3-level fallback resolution")
    class ThreeLevelFallback {

        @Test
        @DisplayName("Level 1: returns campaign override when present")
        void shouldReturnCampaignOverride() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            CampaignSettingOverride override = new CampaignSettingOverride(
                    campaignId, "scoring.passing-score-threshold", "85.0", "admin", Instant.now());

            when(campaignSettingsPort.findByCampaignIdAndKey(campaignId, "scoring.passing-score-threshold"))
                    .thenReturn(Optional.of(override));

            String result = resolver.resolve("scoring.passing-score-threshold", campaignId);

            assertThat(result).isEqualTo("85.0");
            verify(systemSettingsPort, never()).findByKey(any());
        }

        @Test
        @DisplayName("Level 2: falls back to system setting when no campaign override")
        void shouldFallbackToSystemSetting() {
            CampaignId campaignId = CampaignId.of("campaign-1");
            SystemSetting systemSetting = new SystemSetting(
                    "scoring.passing-score-threshold", "80.0", SystemSettingCategory.SCORING,
                    "Pass threshold", "admin", Instant.now());

            when(campaignSettingsPort.findByCampaignIdAndKey(campaignId, "scoring.passing-score-threshold"))
                    .thenReturn(Optional.empty());
            when(systemSettingsPort.findByKey("scoring.passing-score-threshold"))
                    .thenReturn(Optional.of(systemSetting));

            String result = resolver.resolve("scoring.passing-score-threshold", campaignId);

            assertThat(result).isEqualTo("80.0");
        }

        @Test
        @DisplayName("Level 3: falls back to application.yml when no DB settings exist")
        void shouldFallbackToApplicationYml() {
            CampaignId campaignId = CampaignId.of("campaign-1");

            when(campaignSettingsPort.findByCampaignIdAndKey(campaignId, "scoring.passing-score-threshold"))
                    .thenReturn(Optional.empty());
            when(systemSettingsPort.findByKey("scoring.passing-score-threshold"))
                    .thenReturn(Optional.empty());

            String result = resolver.resolve("scoring.passing-score-threshold", campaignId);

            assertThat(result).isEqualTo("70.0");
        }

        @Test
        @DisplayName("System-level resolve (no campaign) skips campaign lookup")
        void shouldResolveWithoutCampaign() {
            SystemSetting systemSetting = new SystemSetting(
                    "pagination.default-page-size", "50", SystemSettingCategory.PAGINATION,
                    "Page size", "admin", Instant.now());

            when(systemSettingsPort.findByKey("pagination.default-page-size"))
                    .thenReturn(Optional.of(systemSetting));

            String result = resolver.resolve("pagination.default-page-size");

            assertThat(result).isEqualTo("50");
            verifyNoInteractions(campaignSettingsPort);
        }
    }

    @Nested
    @DisplayName("Typed resolvers")
    class TypedResolvers {

        @Test
        @DisplayName("resolveInt returns parsed integer")
        void shouldResolveInt() {
            when(systemSettingsPort.findByKey("pagination.default-page-size"))
                    .thenReturn(Optional.empty());

            int result = resolver.resolveInt("pagination.default-page-size");

            assertThat(result).isEqualTo(20);
        }

        @Test
        @DisplayName("resolveDouble returns parsed double")
        void shouldResolveDouble() {
            when(systemSettingsPort.findByKey("scoring.passing-score-threshold"))
                    .thenReturn(Optional.empty());

            double result = resolver.resolveDouble("scoring.passing-score-threshold");

            assertThat(result).isEqualTo(70.0);
        }

        @Test
        @DisplayName("resolveBoolean returns parsed boolean")
        void shouldResolveBoolean() {
            when(systemSettingsPort.findByKey("features.enable-reports"))
                    .thenReturn(Optional.empty());

            boolean result = resolver.resolveBoolean("features.enable-reports");

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("throws on unknown setting key")
        void shouldThrowOnUnknownKey() {
            when(systemSettingsPort.findByKey("unknown.key"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolver.resolve("unknown.key"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown setting key");
        }
    }
}
