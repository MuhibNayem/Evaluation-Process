package com.evaluationservice.application.service.audience;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties.JdbcConnectionRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JdbcAudienceSourceConnector")
class JdbcAudienceSourceConnectorTest {

    @Test
    @DisplayName("loads records using configured connectionRef and query")
    void loadsRecordsUsingConnectionRef() throws Exception {
        String url = "jdbc:h2:mem:jdbc_audience_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        setupH2Data(url);

        EvaluationServiceProperties properties = propertiesWithConnection(url, true, null);
        JdbcAudienceSourceConnector connector = new JdbcAudienceSourceConnector(properties, new ObjectMapper());

        List<AudienceSourceConnector.SourceRecord> records = connector.loadRecords(Map.of(
                "connectionRef", "hr_ref",
                "query", "SELECT employee_id AS person_id, full_name AS display_name, work_email AS email, enabled AS active FROM hr_people"));

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().fields().get("person_id")).isEqualTo("E-1001");
        assertThat(records.getFirst().fields().get("display_name")).isEqualTo("Jdbc User");
        assertThat(records.getFirst().fields().get("email")).isEqualTo("jdbc.user@example.com");
    }

    @Test
    @DisplayName("rejects unknown connectionRef")
    void rejectsUnknownConnectionRef() {
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        JdbcAudienceSourceConnector connector = new JdbcAudienceSourceConnector(properties, new ObjectMapper());

        assertThatThrownBy(() -> connector.loadRecords(Map.of(
                "connectionRef", "missing",
                "query", "SELECT 1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown or disabled JDBC connectionRef");
    }

    @Test
    @DisplayName("rejects custom query when disallowed by connectionRef")
    void rejectsCustomQueryWhenDisallowed() {
        EvaluationServiceProperties properties = propertiesWithConnection(
                "jdbc:h2:mem:any;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                false,
                "SELECT 1 AS person_id");
        JdbcAudienceSourceConnector connector = new JdbcAudienceSourceConnector(properties, new ObjectMapper());

        assertThatThrownBy(() -> connector.loadRecords(Map.of(
                "connectionRef", "hr_ref",
                "query", "SELECT 2 AS person_id")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Custom JDBC query is not allowed");
    }

    private EvaluationServiceProperties propertiesWithConnection(
            String url,
            boolean allowCustomQuery,
            String defaultQuery) {
        EvaluationServiceProperties properties = new EvaluationServiceProperties();
        JdbcConnectionRef ref = new JdbcConnectionRef();
        ref.setEnabled(true);
        ref.setUrl(url);
        ref.setUsername("sa");
        ref.setPassword("");
        ref.setDriverClassName("org.h2.Driver");
        ref.setAllowCustomQuery(allowCustomQuery);
        ref.setDefaultQuery(defaultQuery);
        properties.getAudience().getJdbc().getConnections().put("hr_ref", ref);
        return properties;
    }

    private void setupH2Data(String url) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS hr_people (" +
                    "employee_id VARCHAR(40), full_name VARCHAR(120), work_email VARCHAR(160), enabled VARCHAR(10))");
            statement.execute("DELETE FROM hr_people");
            statement.execute("INSERT INTO hr_people(employee_id, full_name, work_email, enabled) VALUES " +
                    "('E-1001', 'Jdbc User', 'jdbc.user@example.com', 'true')");
        }
    }
}
