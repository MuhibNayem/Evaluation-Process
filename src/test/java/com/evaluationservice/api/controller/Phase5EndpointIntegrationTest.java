package com.evaluationservice.api.controller;

import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.NotificationDeliveryRepository;
import com.evaluationservice.infrastructure.repository.NotificationRuleRepository;
import com.evaluationservice.infrastructure.repository.NotificationTemplateRepository;
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
        "evaluation.service.features.enable-notification-rule-engine=true",
        "spring.datasource.url=jdbc:h2:mem:phase5-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "eureka.client.enabled=false"
})
@DisplayName("Phase 5 Endpoint Integration")
class Phase5EndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private NotificationRuleRepository notificationRuleRepository;
    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;
    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    private HttpClient httpClient;
    private String adminToken;
    private String evaluatorToken;

    @BeforeEach
    void setUp() {
        notificationDeliveryRepository.deleteAll();
        notificationRuleRepository.deleteAll();
        notificationTemplateRepository.deleteAll();
        campaignRepository.deleteAll();
        seedCampaign("c-notif-1");

        httpClient = HttpClient.newHttpClient();
        adminToken = jwtUtil.generateToken("admin-user", List.of("ROLE_ADMIN"));
        evaluatorToken = jwtUtil.generateToken("eval-user", List.of("ROLE_EVALUATOR"));
    }

    @Test
    void adminCanManageNotificationRulesAndTemplates() throws Exception {
        HttpResponse<String> createRule = post("/api/v1/admin/notifications/rules", """
                {
                  "campaignId":"c-notif-1",
                  "ruleCode":"RULE_SUBMIT",
                  "triggerType":"EVALUATION_SUBMITTED",
                  "audience":"EVALUATOR",
                  "channel":"EMAIL",
                  "enabled":true,
                  "config":{}
                }
                """, adminToken);
        assertThat(createRule.statusCode()).isEqualTo(201);
        JsonNode createdRuleJson = objectMapper.readTree(createRule.body());
        assertThat(createdRuleJson.get("id").asLong()).isPositive();

        HttpResponse<String> createTemplate = post("/api/v1/admin/notifications/templates", """
                {
                  "campaignId":"c-notif-1",
                  "templateCode":"TMP_SUBMIT",
                  "name":"Submit template",
                  "channel":"EMAIL",
                  "subject":"Submitted {{campaignName}}",
                  "body":"Hello {{evaluatorId}}",
                  "requiredVariables":["campaignName","evaluatorId"],
                  "status":"PUBLISHED"
                }
                """, adminToken);
        assertThat(createTemplate.statusCode()).isEqualTo(201);
        long templateId = objectMapper.readTree(createTemplate.body()).get("id").asLong();

        HttpResponse<String> render = post("/api/v1/admin/notifications/templates/" + templateId + "/test-render", """
                {
                  "recipient":"user-1",
                  "variables":{"campaignName":"Spring","evaluatorId":"user-1"}
                }
                """, adminToken);
        assertThat(render.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(render.body()).get("renderedBody").asText()).contains("user-1");

        HttpResponse<String> listRules = get("/api/v1/admin/notifications/rules?campaignId=c-notif-1", adminToken);
        assertThat(listRules.statusCode()).isEqualTo(200);
        assertThat(objectMapper.readTree(listRules.body()).isArray()).isTrue();
        assertThat(objectMapper.readTree(listRules.body()).size()).isEqualTo(1);
    }

    @Test
    void nonAdminCannotAccessNotificationAdminEndpoints() throws Exception {
        HttpResponse<String> response = get("/api/v1/admin/notifications/rules", evaluatorToken);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.statusCode()).isNotEqualTo(200);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void seedCampaign(String id) {
        CampaignEntity campaign = new CampaignEntity();
        campaign.setId(id);
        campaign.setName("Notification Campaign");
        campaign.setDescription("desc");
        campaign.setTemplateId("tmpl-1");
        campaign.setTemplateVersion(1);
        campaign.setStatus("ACTIVE");
        campaign.setStartDate(Instant.now().minusSeconds(3600));
        campaign.setEndDate(Instant.now().plusSeconds(86400));
        campaign.setScoringMethod("WEIGHTED_AVERAGE");
        campaign.setAnonymousMode(false);
        campaign.setMinimumRespondents(1);
        campaign.setAudienceSourceType("INLINE");
        campaign.setAudienceSourceConfigJson("{}");
        campaign.setAssignmentRuleType("ALL_TO_ALL");
        campaign.setAssignmentRuleConfigJson("{}");
        campaign.setAssignmentsJson("[]");
        campaign.setCreatedBy("it");
        campaign.setCreatedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        campaign.setLocked(false);
        campaignRepository.save(campaign);
    }
}
