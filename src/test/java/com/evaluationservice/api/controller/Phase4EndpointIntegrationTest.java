package com.evaluationservice.api.controller;

import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.infrastructure.entity.TemplateEntity;
import com.evaluationservice.infrastructure.repository.QuestionBankItemRepository;
import com.evaluationservice.infrastructure.repository.QuestionBankItemVersionRepository;
import com.evaluationservice.infrastructure.repository.QuestionBankSetRepository;
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
        "evaluation.service.features.enable-question-bank=true",
        "spring.datasource.url=jdbc:h2:mem:phase4-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "eureka.client.enabled=false"
})
@DisplayName("Phase 4 Endpoint Integration")
class Phase4EndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private QuestionBankSetRepository setRepository;
    @Autowired
    private QuestionBankItemRepository itemRepository;
    @Autowired
    private QuestionBankItemVersionRepository versionRepository;
    @Autowired
    private TemplateRepository templateRepository;

    private HttpClient httpClient;
    private String adminToken;
    private String evaluatorToken;

    @BeforeEach
    void setUp() {
        versionRepository.deleteAll();
        itemRepository.deleteAll();
        setRepository.deleteAll();
        templateRepository.deleteAll();
        seedTemplate("tmpl-score");
        httpClient = HttpClient.newHttpClient();
        adminToken = jwtUtil.generateToken("admin-user", List.of("ROLE_ADMIN"));
        evaluatorToken = jwtUtil.generateToken("eval-user", List.of("ROLE_EVALUATOR"));
    }

    @Test
    void questionBankLifecycleAndVersionCompareWorksForAdmin() throws Exception {
        HttpResponse<String> createSet = post("/api/v1/questions-bank/sets", """
                {"tenantId":"tenant-1","name":"Core Set","versionTag":"v1","owner":"admin"}
                """, adminToken);
        assertThat(createSet.statusCode()).isEqualTo(201);
        long setId = objectMapper.readTree(createSet.body()).get("id").asLong();

        HttpResponse<String> createItem = post("/api/v1/questions-bank/sets/" + setId + "/items", """
                {"stableKey":"Q1","contextType":"FACULTY","categoryName":"CAT-A","defaultType":"NUMERIC_RATING","defaultMarks":10}
                """, adminToken);
        assertThat(createItem.statusCode()).isEqualTo(201);
        long itemId = objectMapper.readTree(createItem.body()).get("id").asLong();

        HttpResponse<String> v1 = post("/api/v1/questions-bank/items/" + itemId + "/versions", """
                {"status":"DRAFT","changeSummary":"initial","questionText":"Old text","questionType":"NUMERIC_RATING","marks":10,"remarksMandatory":false,"metadata":{"remarksMandatory":false}}
                """, adminToken);
        assertThat(v1.statusCode()).isEqualTo(201);

        HttpResponse<String> v2 = post("/api/v1/questions-bank/items/" + itemId + "/versions", """
                {"status":"ACTIVE","changeSummary":"activate","questionText":"New text","questionType":"NUMERIC_RATING","marks":8,"remarksMandatory":true,"metadata":{"remarksMandatory":true}}
                """, adminToken);
        assertThat(v2.statusCode()).isEqualTo(201);

        HttpResponse<String> compare = get("/api/v1/questions-bank/items/" + itemId + "/versions/compare?fromVersion=1&toVersion=2", adminToken);
        assertThat(compare.statusCode()).isEqualTo(200);
        JsonNode compareJson = objectMapper.readTree(compare.body());
        assertThat(compareJson.get("diffs").isArray()).isTrue();
        assertThat(compareJson.get("diffs").size()).isGreaterThan(0);
    }

    @Test
    void nonAdminCannotUseQuestionBankEndpoints() throws Exception {
        HttpResponse<String> response = get("/api/v1/questions-bank/sets", evaluatorToken);
        assertThat(response.statusCode()).isGreaterThanOrEqualTo(400);
        assertThat(response.statusCode()).isNotEqualTo(200);
    }

    @Test
    void scoringPreviewWorksForAdminAndDeniedForNonAdmin() throws Exception {
        String body = """
                {
                  "templateId":"tmpl-score",
                  "answers":[{"questionId":"q1","value":8}]
                }
                """;
        HttpResponse<String> denied = post("/api/v1/scoring/preview", body, evaluatorToken);
        assertThat(denied.statusCode()).isGreaterThanOrEqualTo(400);
        HttpResponse<String> allowed = post("/api/v1/scoring/preview", body, adminToken);
        assertThat(allowed.statusCode()).isEqualTo(200);
        JsonNode json = objectMapper.readTree(allowed.body());
        assertThat(json.get("totalScore")).isNotNull();
        assertThat(json.get("sections").isArray()).isTrue();
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
}
