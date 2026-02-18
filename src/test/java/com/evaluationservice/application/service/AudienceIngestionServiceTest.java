package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.AudienceIngestionUseCase.IngestionRequest;
import com.evaluationservice.application.port.in.AudienceIngestionUseCase.IngestionResult;
import com.evaluationservice.application.service.audience.CsvAudienceSourceConnector;
import com.evaluationservice.application.service.audience.JsonAudienceSourceConnector;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.entity.AudienceIngestionRejectionEntity;
import com.evaluationservice.infrastructure.entity.AudienceGroupEntity;
import com.evaluationservice.infrastructure.entity.AudiencePersonEntity;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRejectionRepository;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRunRepository;
import com.evaluationservice.infrastructure.repository.AudienceGroupRepository;
import com.evaluationservice.infrastructure.repository.AudienceMembershipRepository;
import com.evaluationservice.infrastructure.repository.AudiencePersonRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.evaluationservice.infrastructure.service.AudienceIngestionSnapshotService;
import com.evaluationservice.infrastructure.service.AudienceMappingProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AudienceIngestionService")
class AudienceIngestionServiceTest {

    @Test
    @DisplayName("parses quoted CSV fields and saves person")
    void parsesQuotedFieldsAndSavesPerson() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("p-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(personRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        String csv = "person_id,display_name,email,active\n"
                + "p-1,\"Doe, \"\"John\"\"\",john@example.com,yes\n";

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "CSV",
                Map.of("csvData", csv),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(1);
        assertThat(result.rejectedRecords()).isEqualTo(0);

        ArgumentCaptor<AudiencePersonEntity> personCaptor = ArgumentCaptor.forClass(AudiencePersonEntity.class);
        verify(personRepository).save(personCaptor.capture());
        assertThat(personCaptor.getValue().getDisplayName()).isEqualTo("Doe, \"John\"");
        assertThat(personCaptor.getValue().isActive()).isTrue();
        verifyNoInteractions(rejectionRepository);
    }

    @Test
    @DisplayName("rejects rows with invalid active value")
    void rejectsRowsWithInvalidActiveValue() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("p-2")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        String csv = "person_id,display_name,active\n"
                + "p-2,Jane,maybe\n";

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "CSV",
                Map.of("csvData", csv),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(0);
        assertThat(result.rejectedRecords()).isEqualTo(1);
        verify(personRepository, never()).save(any());

        ArgumentCaptor<AudienceIngestionRejectionEntity> rejectionCaptor =
                ArgumentCaptor.forClass(AudienceIngestionRejectionEntity.class);
        verify(rejectionRepository).save(rejectionCaptor.capture());
        assertThat(rejectionCaptor.getValue().getRowNumber()).isEqualTo(2);
        assertThat(rejectionCaptor.getValue().getReason()).contains("Invalid active value");
    }

    @Test
    @DisplayName("rejects tenant collision when person id exists in another tenant")
    void rejectsTenantCollision() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        AudiencePersonEntity existing = new AudiencePersonEntity();
        existing.setId("p-3");
        existing.setTenantId("tenant-b");
        when(personRepository.findById("p-3")).thenReturn(Optional.of(existing));
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        String csv = "person_id,display_name,active\n"
                + "p-3,Shared,true\n";

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "CSV",
                Map.of("csvData", csv),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(0);
        assertThat(result.rejectedRecords()).isEqualTo(1);
        verify(personRepository, never()).save(any());
        verify(rejectionRepository).save(any(AudienceIngestionRejectionEntity.class));
    }

    @Test
    @DisplayName("ingests from JSON records connector")
    void ingestsFromJsonConnector() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("p-9")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(personRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "JSON",
                Map.of("records", java.util.List.of(Map.of(
                        "person_id", "p-9",
                        "display_name", "JSON User",
                        "email", "json@example.com",
                        "active", "true"))),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(1);
        assertThat(result.rejectedRecords()).isEqualTo(0);
        verify(personRepository).save(any(AudiencePersonEntity.class));
    }

    @Test
    @DisplayName("applies mapping profile before ingest validation")
    void appliesMappingProfileBeforeIngestValidation() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("mapped-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(personRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings("tenant-a", 7L, "JSON"))
                .thenReturn(Map.of(
                        "person_id", "employee_id",
                        "display_name", "full_name",
                        "email", "mail",
                        "active", "enabled"));

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "JSON",
                Map.of("records", java.util.List.of(Map.of(
                        "employee_id", "mapped-1",
                        "full_name", "Mapped User",
                        "mail", "mapped@example.com",
                        "enabled", "true"))),
                7L,
                false));

        assertThat(result.processedRecords()).isEqualTo(1);
        verify(personRepository).save(any(AudiencePersonEntity.class));
    }

    @Test
    @DisplayName("replays ingestion from snapshot records")
    void replaysIngestionFromSnapshotRecords() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("rp-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(personRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(runRepository.findById("run-1")).thenReturn(Optional.empty());
        when(snapshotService.load("run-1")).thenReturn(new AudienceIngestionSnapshotService.Snapshot(
                "run-1",
                "tenant-a",
                "JSON",
                null,
                Map.of(),
                java.util.List.of(new com.evaluationservice.application.service.audience.AudienceSourceConnector.SourceRecord(
                        1,
                        Map.of(
                                "person_id", "rp-1",
                                "display_name", "Replay User",
                                "email", "replay@example.com",
                                "active", "true"),
                        "{\"person_id\":\"rp-1\"}"))));

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        IngestionResult result = service.replay("run-1", false);

        assertThat(result.processedRecords()).isEqualTo(1);
        assertThat(result.rejectedRecords()).isEqualTo(0);
        verify(personRepository).save(any(AudiencePersonEntity.class));
    }

    @Test
    @DisplayName("rejects rows with invalid email format")
    void rejectsRowsWithInvalidEmail() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("p-email-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        String csv = "person_id,display_name,email,active\n"
                + "p-email-1,User,bad-email,true\n";

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "CSV",
                Map.of("csvData", csv),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(0);
        assertThat(result.rejectedRecords()).isEqualTo(1);
        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects duplicate and invalid person_id values in payload")
    void rejectsDuplicateAndInvalidPersonIds() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("ok-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        String csv = "person_id,display_name,active\n"
                + "ok-1,User One,true\n"
                + "ok-1,User Duplicate,true\n"
                + "bad id!,User Invalid,true\n";

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "CSV",
                Map.of("csvData", csv),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(1);
        assertThat(result.rejectedRecords()).isEqualTo(2);
        verify(personRepository).save(any(AudiencePersonEntity.class));
    }

    @Test
    @DisplayName("rejects membership when referenced group is missing")
    void rejectsMembershipWhenGroupMissing() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        AudiencePersonEntity person = new AudiencePersonEntity();
        person.setId("p-1");
        person.setTenantId("tenant-a");
        when(personRepository.findById("p-1")).thenReturn(Optional.of(person));
        when(groupRepository.findById("g-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()));

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "JSON",
                Map.of(
                        "entityType", "MEMBERSHIP",
                        "records", java.util.List.of(Map.of(
                                "person_id", "p-1",
                                "group_id", "g-1",
                                "active", "true"))),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(0);
        assertThat(result.rejectedRecords()).isEqualTo(1);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    @DisplayName("applies strict validation profile for person email domain and display name")
    void appliesStrictValidationProfileForPerson() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        when(personRepository.findById("p-1")).thenReturn(Optional.empty());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        EvaluationServiceProperties properties = strictProfileProperties();
        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()),
                properties);

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "JSON",
                Map.of(
                        "validationProfile", "strict",
                        "records", java.util.List.of(
                                Map.of(
                                        "person_id", "p-1",
                                        "display_name", "AB",
                                        "email", "user@outside.org",
                                        "active", "true"))),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(0);
        assertThat(result.rejectedRecords()).isEqualTo(1);
        verify(personRepository, never()).save(any());
    }

    @Test
    @DisplayName("applies strict referential profile for membership active entities and validity window")
    void appliesStrictValidationProfileForMembershipReferentialRules() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        AudiencePersonRepository personRepository = mock(AudiencePersonRepository.class);
        AudienceGroupRepository groupRepository = mock(AudienceGroupRepository.class);
        AudienceMembershipRepository membershipRepository = mock(AudienceMembershipRepository.class);
        AudienceIngestionRunRepository runRepository = mock(AudienceIngestionRunRepository.class);
        AudienceIngestionRejectionRepository rejectionRepository = mock(AudienceIngestionRejectionRepository.class);
        AudienceMappingProfileService mappingProfileService = mock(AudienceMappingProfileService.class);
        AudienceIngestionSnapshotService snapshotService = mock(AudienceIngestionSnapshotService.class);

        when(tenantRepository.existsById("tenant-a")).thenReturn(true);
        AudiencePersonEntity person = new AudiencePersonEntity();
        person.setId("p-1");
        person.setTenantId("tenant-a");
        person.setActive(false);
        when(personRepository.findById("p-1")).thenReturn(Optional.of(person));

        AudienceGroupEntity group = new AudienceGroupEntity();
        group.setId("g-1");
        group.setTenantId("tenant-a");
        group.setGroupType("SECTION");
        group.setActive(true);
        when(groupRepository.findById("g-1")).thenReturn(Optional.of(group));

        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mappingProfileService.resolveActiveMappings(anyString(), any(), anyString())).thenReturn(Map.of());

        EvaluationServiceProperties properties = strictProfileProperties();
        AudienceIngestionService service = new AudienceIngestionService(
                tenantRepository,
                personRepository,
                groupRepository,
                membershipRepository,
                runRepository,
                rejectionRepository,
                mappingProfileService,
                snapshotService,
                java.util.List.of(new CsvAudienceSourceConnector(), new JsonAudienceSourceConnector()),
                properties);

        IngestionResult result = service.ingest(new IngestionRequest(
                "tenant-a",
                "JSON",
                Map.of(
                        "entityType", "MEMBERSHIP",
                        "validationProfile", "strict",
                        "records", java.util.List.of(Map.of(
                                "person_id", "p-1",
                                "group_id", "g-1",
                                "membership_role", "STUDENT",
                                "valid_from", "2026-02-20T00:00:00Z",
                                "valid_to", "2026-02-19T00:00:00Z",
                                "active", "true"))),
                null,
                false));

        assertThat(result.processedRecords()).isEqualTo(0);
        assertThat(result.rejectedRecords()).isEqualTo(1);
        verify(membershipRepository, never()).save(any());
    }

    private EvaluationServiceProperties strictProfileProperties() {
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        EvaluationServiceProperties.ValidationProfile strict = new EvaluationServiceProperties.ValidationProfile();
        strict.setRequirePersonDisplayName(true);
        strict.setMinPersonDisplayNameLength(3);
        strict.setRequirePersonEmail(true);
        strict.setAllowedEmailDomains(java.util.List.of("example.com"));
        strict.setRequireMembershipRole(true);
        strict.setRequireMembershipValidityWindow(true);
        strict.setRequireActivePersonForMembership(true);
        strict.setRequireActiveGroupForMembership(true);
        strict.setMembershipRoleAllowedGroupTypes(Map.of("STUDENT", java.util.List.of("SECTION")));
        properties.getAudience().setValidationProfiles(Map.of("strict", strict));
        return properties;
    }
}
