package com.evaluationservice.infrastructure.service;

import com.evaluationservice.infrastructure.entity.AudienceMappingProfileEntity;
import com.evaluationservice.infrastructure.entity.AudienceMappingProfileEventEntity;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileEventRepository;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@DisplayName("AudienceMappingProfileService")
class AudienceMappingProfileServiceTest {

    @Test
    @DisplayName("normalizes valid mapping profile")
    void normalizesValidMappingProfile() {
        AudienceMappingProfileRepository repo = mock(AudienceMappingProfileRepository.class);
        AudienceMappingProfileEventRepository eventRepo = mock(AudienceMappingProfileEventRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AudienceMappingProfileService service = new AudienceMappingProfileService(
                repo, eventRepo, tenantRepository, eventPublisher, new ObjectMapper());

        Map<String, String> result = service.normalizeAndValidate("csv", Map.of(
                "person_id", "EmployeeId",
                "display_name", "FullName",
                "email", "WorkEmail",
                "active", "Enabled"));

        assertThat(result.get("person_id")).isEqualTo("employeeid");
        assertThat(result.get("display_name")).isEqualTo("fullname");
    }

    @Test
    @DisplayName("rejects mapping without person_id")
    void rejectsMissingPersonId() {
        AudienceMappingProfileRepository repo = mock(AudienceMappingProfileRepository.class);
        AudienceMappingProfileEventRepository eventRepo = mock(AudienceMappingProfileEventRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AudienceMappingProfileService service = new AudienceMappingProfileService(
                repo, eventRepo, tenantRepository, eventPublisher, new ObjectMapper());

        assertThatThrownBy(() -> service.normalizeAndValidate("json", Map.of("email", "mail")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("person_id");
    }

    @Test
    @DisplayName("resolves active mapping profile")
    void resolvesActiveMappingProfile() {
        AudienceMappingProfileRepository repo = mock(AudienceMappingProfileRepository.class);
        AudienceMappingProfileEventRepository eventRepo = mock(AudienceMappingProfileEventRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AudienceMappingProfileService service = new AudienceMappingProfileService(
                repo, eventRepo, tenantRepository, eventPublisher, new ObjectMapper());

        AudienceMappingProfileEntity entity = new AudienceMappingProfileEntity();
        entity.setId(5L);
        entity.setTenantId("tenant-a");
        entity.setSourceType("JSON");
        entity.setMappingsJson("{\"person_id\":\"employee_id\"}");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setActive(true);

        when(repo.findByIdAndTenantIdAndActiveTrue(5L, "tenant-a")).thenReturn(Optional.of(entity));
        when(tenantRepository.existsById("tenant-a")).thenReturn(true);

        Map<String, String> result = service.resolveActiveMappings("tenant-a", 5L, "json");
        assertThat(result.get("person_id")).isEqualTo("employee_id");
    }

    @Test
    @DisplayName("creates mapping profile and emits audit event")
    void createsMappingProfileAndEmitsAuditEvent() {
        AudienceMappingProfileRepository repo = mock(AudienceMappingProfileRepository.class);
        AudienceMappingProfileEventRepository eventRepo = mock(AudienceMappingProfileEventRepository.class);
        TenantRepository tenantRepository = mock(TenantRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        AudienceMappingProfileService service = new AudienceMappingProfileService(
                repo, eventRepo, tenantRepository, eventPublisher, new ObjectMapper());

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(repo.save(any(AudienceMappingProfileEntity.class))).thenAnswer(invocation -> {
            AudienceMappingProfileEntity saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });
        when(eventRepo.save(any(AudienceMappingProfileEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(
                "tenant-a",
                "profile-1",
                "json",
                Map.of("person_id", "employee_id"),
                true,
                "tester");

        assertThat(response.id()).isEqualTo(77L);
        verify(eventRepo).save(any(AudienceMappingProfileEventEntity.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }
}
