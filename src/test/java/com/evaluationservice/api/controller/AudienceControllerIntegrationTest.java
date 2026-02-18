package com.evaluationservice.api.controller;

import com.evaluationservice.infrastructure.entity.TenantEntity;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRejectionRepository;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRunRepository;
import com.evaluationservice.infrastructure.repository.AudienceGroupRepository;
import com.evaluationservice.infrastructure.repository.AudienceMembershipRepository;
import com.evaluationservice.infrastructure.repository.AudienceMappingProfileRepository;
import com.evaluationservice.infrastructure.repository.AudiencePersonRepository;
import com.evaluationservice.infrastructure.repository.IntegrationOutboxEventRepository;
import com.evaluationservice.infrastructure.repository.TenantRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "evaluation.service.security.dev-mode=true",
        "spring.datasource.url=jdbc:h2:mem:audience-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "eureka.client.enabled=false"
})
@DisplayName("AudienceController Integration")
class AudienceControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AudiencePersonRepository audiencePersonRepository;

    @Autowired
    private AudienceGroupRepository audienceGroupRepository;

    @Autowired
    private AudienceMembershipRepository audienceMembershipRepository;

    @Autowired
    private AudienceMappingProfileRepository mappingProfileRepository;

    @Autowired
    private AudienceIngestionRunRepository ingestionRunRepository;

    @Autowired
    private AudienceIngestionRejectionRepository rejectionRepository;

    @Autowired
    private IntegrationOutboxEventRepository outboxEventRepository;

    private HttpClient httpClient;

    @BeforeEach
    void setup() {
        rejectionRepository.deleteAll();
        ingestionRunRepository.deleteAll();
        audienceMembershipRepository.deleteAll();
        audienceGroupRepository.deleteAll();
        audiencePersonRepository.deleteAll();
        mappingProfileRepository.deleteAll();
        outboxEventRepository.deleteAll();
        tenantRepository.deleteAll();

        TenantEntity tenant = new TenantEntity();
        tenant.setId("tenant-it");
        tenant.setName("Integration Tenant");
        tenant.setCode("TENANT_IT");
        tenant.setActive(true);
        tenant.setCreatedAt(Instant.now());
        tenant.setUpdatedAt(Instant.now());
        tenantRepository.save(tenant);
        this.httpClient = HttpClient.newHttpClient();
    }

    @Test
    @DisplayName("creates mapping profile then ingests mapped JSON payload")
    void createsMappingProfileAndIngestsMappedJson() throws Exception {
        String createProfileBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "name", "hr-json-profile",
                "sourceType", "JSON",
                "fieldMappings", Map.of(
                        "person_id", "employee_id",
                        "display_name", "full_name",
                        "email", "work_email",
                        "active", "enabled"
                ),
                "active", true));

        String profileResponse = postJson("/api/v1/audience/mapping-profiles", createProfileBody);

        JsonNode profileJson = objectMapper.readTree(profileResponse);
        assertThat(profileJson.get("tenantId").asText()).isEqualTo("tenant-it");
        long profileId = profileJson.get("id").asLong();

        String ingestBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "mappingProfileId", profileId,
                "dryRun", false,
                "sourceConfig", Map.of(
                        "records", java.util.List.of(Map.of(
                                "employee_id", "emp-1001",
                                "full_name", "Mapped Integration User",
                                "work_email", "mapped-it@example.com",
                                "enabled", "true"
                        ))
                )));

        String ingestResponse = postJson("/api/v1/audience/ingest", ingestBody);

        JsonNode ingestJson = objectMapper.readTree(ingestResponse);
        assertThat(ingestJson.get("processedRecords").asInt()).isEqualTo(1);
        assertThat(ingestJson.get("rejectedRecords").asInt()).isEqualTo(0);

        var person = audiencePersonRepository.findById("emp-1001");
        assertThat(person).isPresent();
        assertThat(person.get().getDisplayName()).isEqualTo("Mapped Integration User");
        assertThat(person.get().getEmail()).isEqualTo("mapped-it@example.com");
        assertThat(person.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("persists ingestion rejection and exposes it through query API")
    void persistsRejectionAndExposesItViaApi() throws Exception {
        String ingestBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "records", java.util.List.of(Map.of(
                                "person_id", "emp-2001",
                                "display_name", "Reject User",
                                "active", "maybe"
                        ))
                )));

        String responseBody = postJson("/api/v1/audience/ingest", ingestBody);

        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("processedRecords").asInt()).isEqualTo(0);
        assertThat(json.get("rejectedRecords").asInt()).isEqualTo(1);
        String runId = json.get("runId").asText();

        String rejectionsResponse = get("/api/v1/audience/ingestion-runs/" + runId + "/rejections");

        JsonNode rejections = objectMapper.readTree(rejectionsResponse);
        assertThat(rejections).isNotNull();
        assertThat(rejections.isArray()).isTrue();
        assertThat(rejections).hasSize(1);
        assertThat(rejections.get(0).get("runId").asText()).isEqualTo(runId);
        assertThat(rejections.get(0).get("reason").asText()).contains("Invalid active value");
    }

    @Test
    @DisplayName("updates and deactivates mapping profile with lifecycle events")
    void updatesAndDeactivatesMappingProfileWithEvents() throws Exception {
        String createBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "name", "profile-a",
                "sourceType", "JSON",
                "fieldMappings", Map.of(
                        "person_id", "employee_id",
                        "display_name", "full_name"
                ),
                "active", true));
        String createResponse = postJson("/api/v1/audience/mapping-profiles", createBody);
        long profileId = objectMapper.readTree(createResponse).get("id").asLong();

        String updateBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "name", "profile-a-v2",
                "fieldMappings", Map.of(
                        "person_id", "employee_id",
                        "display_name", "employee_name",
                        "email", "work_email"
                ),
                "active", true));
        String updated = putJson("/api/v1/audience/mapping-profiles/" + profileId, updateBody);
        JsonNode updatedJson = objectMapper.readTree(updated);
        assertThat(updatedJson.get("name").asText()).isEqualTo("profile-a-v2");

        String deactivateBody = objectMapper.writeValueAsString(Map.of("tenantId", "tenant-it"));
        String deactivated = postJson("/api/v1/audience/mapping-profiles/" + profileId + "/deactivate", deactivateBody);
        JsonNode deactivatedJson = objectMapper.readTree(deactivated);
        assertThat(deactivatedJson.get("active").asBoolean()).isFalse();

        String eventsBody = get("/api/v1/audience/mapping-profiles/" + profileId + "/events?tenantId=tenant-it&limit=10");
        JsonNode events = objectMapper.readTree(eventsBody);
        assertThat(events.isArray()).isTrue();
        assertThat(events.size()).isGreaterThanOrEqualTo(3);
        assertThat(events.get(0).get("eventType").asText()).isIn("DEACTIVATED", "UPDATED", "CREATED");

        long outboxCount = outboxEventRepository.countByAggregateTypeAndAggregateId(
                "AUDIENCE_MAPPING_PROFILE",
                String.valueOf(profileId));
        assertThat(outboxCount).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("replays ingestion run from persisted snapshot")
    void replaysIngestionRunFromSnapshot() throws Exception {
        String ingestBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "records", java.util.List.of(Map.of(
                                "person_id", "rep-1",
                                "display_name", "Replay Source User",
                                "active", "true"
                        ))
                )));

        String created = postJson("/api/v1/audience/ingest", ingestBody);
        String originalRunId = objectMapper.readTree(created).get("runId").asText();

        String replayBody = objectMapper.writeValueAsString(Map.of("dryRun", true));
        String replay = postJson("/api/v1/audience/ingestion-runs/" + originalRunId + "/replay", replayBody);
        JsonNode replayJson = objectMapper.readTree(replay);
        assertThat(replayJson.get("processedRecords").asInt()).isEqualTo(1);
        assertThat(replayJson.get("dryRun").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("rejects invalid email with row-level quality reason")
    void rejectsInvalidEmailWithQualityReason() throws Exception {
        String ingestBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "records", java.util.List.of(Map.of(
                                "person_id", "qual-1",
                                "display_name", "Quality User",
                                "email", "not-an-email",
                                "active", "true"
                        ))
                )));

        String responseBody = postJson("/api/v1/audience/ingest", ingestBody);
        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.get("processedRecords").asInt()).isEqualTo(0);
        assertThat(json.get("rejectedRecords").asInt()).isEqualTo(1);
        String runId = json.get("runId").asText();

        String rejectionsResponse = get("/api/v1/audience/ingestion-runs/" + runId + "/rejections");
        JsonNode rejections = objectMapper.readTree(rejectionsResponse);
        assertThat(rejections).hasSize(1);
        assertThat(rejections.get(0).get("reason").asText()).contains("Invalid email format");
    }

    @Test
    @DisplayName("ingests groups and memberships with referential validation")
    void ingestsGroupsAndMembershipsWithReferentialValidation() throws Exception {
        String groupIngestBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "entityType", "GROUP",
                        "records", java.util.List.of(Map.of(
                                "group_id", "sec-1",
                                "group_type", "SECTION",
                                "name", "Section 1A",
                                "active", "true"
                        ))
                )));
        String groupResult = postJson("/api/v1/audience/ingest", groupIngestBody);
        JsonNode groupJson = objectMapper.readTree(groupResult);
        assertThat(groupJson.get("processedRecords").asInt()).isEqualTo(1);

        String memberRejectBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "entityType", "MEMBERSHIP",
                        "records", java.util.List.of(Map.of(
                                "person_id", "missing-person",
                                "group_id", "sec-1",
                                "active", "true"
                        ))
                )));
        String memberRejectResult = postJson("/api/v1/audience/ingest", memberRejectBody);
        JsonNode rejectJson = objectMapper.readTree(memberRejectResult);
        assertThat(rejectJson.get("processedRecords").asInt()).isEqualTo(0);
        assertThat(rejectJson.get("rejectedRecords").asInt()).isEqualTo(1);

        String personIngestBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "records", java.util.List.of(Map.of(
                                "person_id", "stu-1",
                                "display_name", "Student One",
                                "active", "true"
                        ))
                )));
        String personResult = postJson("/api/v1/audience/ingest", personIngestBody);
        JsonNode personJson = objectMapper.readTree(personResult);
        assertThat(personJson.get("processedRecords").asInt()).isEqualTo(1);

        String memberSuccessBody = objectMapper.writeValueAsString(Map.of(
                "tenantId", "tenant-it",
                "sourceType", "JSON",
                "dryRun", false,
                "sourceConfig", Map.of(
                        "entityType", "MEMBERSHIP",
                        "records", java.util.List.of(Map.of(
                                "person_id", "stu-1",
                                "group_id", "sec-1",
                                "membership_role", "STUDENT",
                                "active", "true"
                        ))
                )));
        String memberSuccessResult = postJson("/api/v1/audience/ingest", memberSuccessBody);
        JsonNode successJson = objectMapper.readTree(memberSuccessResult);
        assertThat(successJson.get("processedRecords").asInt()).isEqualTo(1);
        assertThat(successJson.get("rejectedRecords").asInt()).isEqualTo(0);
    }

    private String postJson(String path, String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }

    private String putJson(String path, String payload) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        return response.body();
    }
}
