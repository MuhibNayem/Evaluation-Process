package com.evaluationservice.application.service.audience;

import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties.JdbcConnectionRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class JdbcAudienceSourceConnector implements AudienceSourceConnector {

    private final EvaluationServiceProperties properties;
    private final ObjectMapper objectMapper;

    public JdbcAudienceSourceConnector(EvaluationServiceProperties properties, ObjectMapper objectMapper) {
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public String sourceType() {
        return "JDBC";
    }

    @Override
    public List<SourceRecord> loadRecords(Map<String, Object> sourceConfig) {
        Map<String, Object> config = sourceConfig == null ? Map.of() : sourceConfig;
        String connectionRefName = text(config.get("connectionRef"));
        if (connectionRefName == null || connectionRefName.isBlank()) {
            throw new IllegalArgumentException("JDBC source requires sourceConfig.connectionRef");
        }

        JdbcConnectionRef ref = properties.getAudience().getJdbc().getConnections().get(connectionRefName);
        if (ref == null || !ref.isEnabled()) {
            throw new IllegalArgumentException("Unknown or disabled JDBC connectionRef: " + connectionRefName);
        }

        String query = resolveQuery(ref, text(config.get("query")));
        validateSelectOnly(query);
        loadDriver(ref.getDriverClassName());

        try (Connection connection = DriverManager.getConnection(ref.getUrl(), ref.getUsername(), ref.getPassword());
             PreparedStatement stmt = connection.prepareStatement(query)) {
            if (ref.getQueryTimeoutSeconds() > 0) {
                stmt.setQueryTimeout(ref.getQueryTimeoutSeconds());
            }
            if (ref.getFetchSize() > 0) {
                stmt.setFetchSize(ref.getFetchSize());
            }
            if (ref.getMaxRows() > 0) {
                stmt.setMaxRows(ref.getMaxRows());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return toRecords(rs);
            }
        } catch (SQLException ex) {
            throw new IllegalArgumentException("JDBC source query failed: " + ex.getMessage(), ex);
        }
    }

    private List<SourceRecord> toRecords(ResultSet rs) throws SQLException {
        List<SourceRecord> records = new ArrayList<>();
        ResultSetMetaData metadata = rs.getMetaData();
        int columns = metadata.getColumnCount();

        int rowNumber = 1;
        while (rs.next()) {
            Map<String, String> fields = new LinkedHashMap<>();
            Map<String, Object> raw = new LinkedHashMap<>();
            for (int i = 1; i <= columns; i++) {
                String label = metadata.getColumnLabel(i);
                String key = label == null ? "" : label.trim().toLowerCase(Locale.ROOT);
                if (key.isEmpty()) {
                    continue;
                }
                Object value = rs.getObject(i);
                raw.put(key, value);
                String normalized = value == null ? null : String.valueOf(value).trim();
                fields.put(key, (normalized == null || normalized.isEmpty()) ? null : normalized);
            }
            records.add(new SourceRecord(rowNumber++, fields, toJson(raw)));
        }
        return records;
    }

    private String resolveQuery(JdbcConnectionRef ref, String requestQuery) {
        if (requestQuery != null && !requestQuery.isBlank()) {
            if (!ref.isAllowCustomQuery()) {
                throw new IllegalArgumentException("Custom JDBC query is not allowed for this connectionRef");
            }
            return requestQuery;
        }
        if (ref.getDefaultQuery() == null || ref.getDefaultQuery().isBlank()) {
            throw new IllegalArgumentException(
                    "JDBC source requires sourceConfig.query when no defaultQuery is configured");
        }
        return ref.getDefaultQuery();
    }

    private void validateSelectOnly(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (!normalized.startsWith("select")) {
            throw new IllegalArgumentException("JDBC query must be a SELECT statement");
        }
    }

    private void loadDriver(String driverClassName) {
        if (driverClassName == null || driverClassName.isBlank()) {
            return;
        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("JDBC driver not found: " + driverClassName, ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize JDBC row", ex);
        }
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
