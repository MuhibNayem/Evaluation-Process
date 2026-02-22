package com.evaluationservice.api.controller;

import com.evaluationservice.infrastructure.security.JwtUtil;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "evaluation.service.security.dev-mode=false",
        "evaluation.service.features.enable-notification-rule-engine=false",
        "spring.datasource.url=jdbc:h2:mem:phase5-ff-it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.cache.type=simple",
        "eureka.client.enabled=false"
})
@DisplayName("Phase 5 Notification Feature Flag")
class Phase5FeatureFlagIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtil jwtUtil;

    private HttpClient httpClient;
    private String adminToken;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
        adminToken = jwtUtil.generateToken("admin-user", List.of("ROLE_ADMIN"));
    }

    @Test
    void returnsConflictWhenNotificationFeatureDisabled() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/admin/notifications/rules"))
                .header("Authorization", "Bearer " + adminToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("Feature is disabled: features.enable-notification-rule-engine");
    }
}
