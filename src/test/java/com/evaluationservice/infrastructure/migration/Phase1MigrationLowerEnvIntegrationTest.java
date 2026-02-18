package com.evaluationservice.infrastructure.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Phase 1 Migration Lower Env Integration")
class Phase1MigrationLowerEnvIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("evaluation_phase1_it")
            .withUsername("eval")
            .withPassword("eval");

    @Test
    @DisplayName("applies schema migrations and backfills campaign assignments from legacy JSON")
    void appliesMigrationsAndBackfillsAssignments() throws Exception {
        Flyway upToV4 = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .target("4")
                .baselineOnMigrate(true)
                .load();
        upToV4.migrate();

        try (var conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
                var stmt = conn.createStatement()) {
            stmt.execute("""
                    INSERT INTO templates (id, name, status, current_version, scoring_method, created_by, created_at, updated_at)
                    VALUES ('tmpl-it-1', 'Template IT', 'PUBLISHED', 1, 'WEIGHTED_AVERAGE', 'it', NOW(), NOW())
                    """);
            stmt.execute("""
                    INSERT INTO campaigns (
                      id, name, template_id, template_version, status, start_date, end_date, scoring_method,
                      anonymous_mode, minimum_respondents, assignments_json, created_by, created_at, updated_at
                    ) VALUES (
                      'camp-it-1', 'Campaign IT', 'tmpl-it-1', 1, 'ACTIVE', NOW(), NOW() + INTERVAL '1 day',
                      'WEIGHTED_AVERAGE', false, 1,
                      '[{"id":"assign-it-1","evaluatorId":"eva-1","evaluateeId":"target-1","evaluatorRole":"PEER","completed":true}]',
                      'it', NOW(), NOW()
                    )
                    """);
        }

        Flyway all = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        all.migrate();

        try (var conn = DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
                var stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("""
                    SELECT evaluator_id, evaluatee_id, evaluator_role, completed
                    FROM campaign_assignments
                    WHERE id = 'assign-it-1'
                    """);
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("evaluator_id")).isEqualTo("eva-1");
            assertThat(rs.getString("evaluatee_id")).isEqualTo("target-1");
            assertThat(rs.getString("evaluator_role")).isEqualTo("PEER");
            assertThat(rs.getBoolean("completed")).isTrue();
        }
    }
}
