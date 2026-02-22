package com.evaluationservice.api.controller;

import com.evaluationservice.domain.enums.CampaignStatus;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.entity.EvaluationEntity;
import com.evaluationservice.infrastructure.entity.TemplateEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.EvaluationJpaRepository;
import com.evaluationservice.infrastructure.repository.TemplateRepository;
import com.evaluationservice.infrastructure.security.JwtUtil;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "evaluation.service.security.dev-mode=false",
        "spring.datasource.url=jdbc:h2:mem:phase3-security-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "eureka.client.enabled=false",
        "evaluation.service.assignment.storage-mode=DUAL"
})
@DisplayName("Phase 3 Endpoint Security Integration")
class Phase3EndpointSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAssignmentRepository assignmentRepository;

    @Autowired
    private TemplateRepository templateRepository;

    @Autowired
    private EvaluationJpaRepository evaluationRepository;

    private HttpClient httpClient;
    private String adminToken;
    private String evaluatorToken;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
        assignmentRepository.deleteAll();
        campaignRepository.deleteAll();
        templateRepository.deleteAll();

        seedTemplate("tmpl-it");
        seedCampaign("camp-it", "tmpl-it");
        seedAssignment("a-existing", "camp-it", "e-100", "u-200", "PEER", true, "ev-1");
        seedEvaluation("ev-1", "camp-it", "a-existing", "e-100", "u-200", "tmpl-it", "COMPLETED");

        this.httpClient = HttpClient.newHttpClient();
        this.adminToken = jwtUtil.generateToken("admin-user", List.of("ROLE_ADMIN"));
        this.evaluatorToken = jwtUtil.generateToken("eval-user", List.of("ROLE_EVALUATOR"));
    }

    @Test
    void adminCreateAssignment_duplicateReturnsStructured409() throws Exception {
        String body = """
                {
                  "campaignId":"camp-it",
                  "evaluatorId":"e-100",
                  "evaluateeId":"u-200",
                  "evaluatorRole":"PEER",
                  "stepType":"PEER",
                  "status":"ACTIVE"
                }
                """;

        HttpResponse<String> response = postJson("/api/v1/assignments", body, adminToken);

        assertThat(response.statusCode()).isEqualTo(409);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.get("type").asText()).isEqualTo("https://api.evaluationservice.com/errors/duplicate-assignment");
        assertThat(json.get("existingAssignmentId").asText()).isEqualTo("a-existing");
        assertThat(json.get("campaignId").asText()).isEqualTo("camp-it");
    }

    @Test
    void nonAdminCannotAccessAssignmentAdminEndpoints() throws Exception {
        HttpResponse<String> response = get("/api/v1/assignments", evaluatorToken);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.statusCode()).isNotEqualTo(200);
    }

    @Test
    void evaluatorCanUseValidateSubmitEndpoint() throws Exception {
        String body = """
                {
                  "campaignId":"camp-it",
                  "assignmentId":"a-existing",
                  "evaluatorId":"e-100",
                  "evaluateeId":"u-200",
                  "templateId":"tmpl-it",
                  "answers":[{"questionId":"q1","value":5}]
                }
                """;

        HttpResponse<String> response = postJson("/api/v1/evaluations/validate-submit", body, evaluatorToken);
        assertThat(response.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(response.body());
        assertThat(json.has("valid")).isTrue();
    }

    @Test
    void nonAdminCannotAccessAdminSubmissionEndpoints() throws Exception {
        HttpResponse<String> detail = get("/api/v1/evaluations/ev-1/admin-detail", evaluatorToken);
        HttpResponse<String> reopen = postJson("/api/v1/evaluations/ev-1/reopen", "{}", evaluatorToken);
        assertThat(detail.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(reopen.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(detail.statusCode()).isNotEqualTo(200);
        assertThat(reopen.statusCode()).isNotEqualTo(200);
    }

    @Test
    void adminCanReopenSubmission() throws Exception {
        HttpResponse<String> reopen = postJson("/api/v1/evaluations/ev-1/reopen", "{}", adminToken);
        assertThat(reopen.statusCode()).isEqualTo(200);

        HttpResponse<String> detail = get("/api/v1/evaluations/ev-1/admin-detail", adminToken);
        assertThat(detail.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(detail.body());
        assertThat(json.get("evaluationStatus").asText()).isEqualTo("DRAFT");
        assertThat(json.get("assignmentStatus").asText()).isEqualTo("ACTIVE");
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String body, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void seedTemplate(String templateId) {
        TemplateEntity template = new TemplateEntity();
        template.setId(templateId);
        template.setName("Template");
        template.setDescription("desc");
        template.setCategory("CAT");
        template.setStatus(TemplateStatus.PUBLISHED.name());
        template.setCurrentVersion(1);
        template.setScoringMethod(ScoringMethod.WEIGHTED_AVERAGE.name());
        template.setSectionsJson("""
                [
                  {
                    "id":"s1",
                    "title":"Section",
                    "orderIndex":1,
                    "weight":1.0,
                    "questions":[
                      {"id":"q1","text":"Rate","type":"NUMERIC_RATING","orderIndex":1,"required":true,"options":[],"weight":1.0,"metadata":{}}
                    ]
                  }
                ]
                """);
        template.setCreatedBy("it");
        template.setCreatedAt(Instant.now());
        template.setUpdatedAt(Instant.now());
        templateRepository.save(template);
    }

    private void seedCampaign(String campaignId, String templateId) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId(campaignId);
        campaign.setName("Campaign");
        campaign.setTemplateId(templateId);
        campaign.setTemplateVersion(1);
        campaign.setStatus(CampaignStatus.ACTIVE.name());
        campaign.setStartDate(Instant.now().minusSeconds(3600));
        campaign.setEndDate(Instant.now().plusSeconds(86400));
        campaign.setScoringMethod(ScoringMethod.WEIGHTED_AVERAGE.name());
        campaign.setAnonymousMode(false);
        campaign.setAnonymousRolesJson("[]");
        campaign.setMinimumRespondents(1);
        campaign.setAudienceSourceType("INLINE");
        campaign.setAudienceSourceConfigJson("{}");
        campaign.setAssignmentRuleType("ALL_TO_ALL");
        campaign.setAssignmentRuleConfigJson("{}");
        campaign.setAssignmentsJson("[]");
        campaign.setCreatedBy("it");
        campaign.setCreatedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        campaignRepository.save(campaign);
    }

    private void seedAssignment(
            String id,
            String campaignId,
            String evaluatorId,
            String evaluateeId,
            String evaluatorRole,
            boolean completed,
            String evaluationId) {
        CampaignAssignmentEntity assignment = new CampaignAssignmentEntity();
        assignment.setId(id);
        assignment.setCampaignId(campaignId);
        assignment.setEvaluatorId(evaluatorId);
        assignment.setEvaluateeId(evaluateeId);
        assignment.setEvaluatorRole(evaluatorRole);
        assignment.setCompleted(completed);
        assignment.setEvaluationId(evaluationId);
        assignment.setStepType("PEER");
        assignment.setAnonymityMode("VISIBLE");
        assignment.setStatus(completed ? "COMPLETED" : "ACTIVE");
        assignment.setCreatedAt(Instant.now());
        assignment.setUpdatedAt(Instant.now());
        assignmentRepository.save(assignment);
    }

    private void seedEvaluation(
            String id,
            String campaignId,
            String assignmentId,
            String evaluatorId,
            String evaluateeId,
            String templateId,
            String status) {
        EvaluationEntity entity = new EvaluationEntity();
        entity.setId(id);
        entity.setCampaignId(campaignId);
        entity.setAssignmentId(assignmentId);
        entity.setEvaluatorId(evaluatorId);
        entity.setEvaluateeId(evaluateeId);
        entity.setTemplateId(templateId);
        entity.setStatus(status);
        entity.setAnswersJson("[]");
        entity.setSectionScoresJson("[]");
        entity.setTotalScore(0.0);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setSubmittedAt(Instant.now());
        evaluationRepository.save(entity);
    }
}
