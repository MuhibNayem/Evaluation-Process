package com.evaluationservice.application.service.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class RestAudienceSourceConnector implements AudienceSourceConnector {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration MAX_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration MAX_READ_TIMEOUT = Duration.ofSeconds(120);

    private final ObjectMapper objectMapper;

    public RestAudienceSourceConnector(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    public String sourceType() {
        return "REST";
    }

    @Override
    public List<SourceRecord> loadRecords(Map<String, Object> sourceConfig) {
        Map<String, Object> config = sourceConfig == null ? Map.of() : sourceConfig;
        String url = text(config.get("url"));
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("REST source requires sourceConfig.url");
        }

        String method = upperOrDefault(text(config.get("method")), "GET");
        if (!method.equals("GET") && !method.equals("POST")) {
            throw new IllegalArgumentException("REST method must be GET or POST");
        }

        Duration connectTimeout = boundedDuration(config.get("connectTimeoutMs"), DEFAULT_CONNECT_TIMEOUT, MAX_CONNECT_TIMEOUT);
        Duration readTimeout = boundedDuration(config.get("readTimeoutMs"), DEFAULT_READ_TIMEOUT, MAX_READ_TIMEOUT);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(readTimeout);

        addHeaders(requestBuilder, config.get("headers"));
        if (method.equals("POST")) {
            String body = requestBody(config.get("body"));
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
            requestBuilder.header("Content-Type", "application/json");
        } else {
            requestBuilder.GET();
        }

        HttpResponse<String> response;
        try {
            response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalArgumentException("REST source request failed: " + ex.getMessage(), ex);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalArgumentException("REST source returned non-success status: " + response.statusCode());
        }

        List<Map<String, Object>> records = extractRecords(response.body(), text(config.get("recordsPath")));
        if (records.isEmpty()) {
            throw new IllegalArgumentException("REST source returned no records");
        }

        List<SourceRecord> result = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            Map<String, String> fields = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : records.get(i).entrySet()) {
                String key = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (key.isEmpty()) {
                    continue;
                }
                String value = entry.getValue() == null ? null : String.valueOf(entry.getValue()).trim();
                fields.put(key, value == null || value.isEmpty() ? null : value);
            }
            result.add(new SourceRecord(i + 1, fields, toJson(records.get(i))));
        }
        return result;
    }

    private String requestBody(Object body) {
        if (body == null) {
            return "{}";
        }
        if (body instanceof String textBody) {
            return textBody;
        }
        return toJson(body);
    }

    private void addHeaders(HttpRequest.Builder requestBuilder, Object headers) {
        if (!(headers instanceof Map<?, ?> headerMap)) {
            return;
        }
        for (Map.Entry<?, ?> entry : headerMap.entrySet()) {
            String key = text(entry.getKey());
            String value = text(entry.getValue());
            if (key != null && !key.isBlank() && value != null) {
                requestBuilder.header(key, value);
            }
        }
    }

    private List<Map<String, Object>> extractRecords(String body, String recordsPath) {
        Object payload;
        try {
            payload = objectMapper.readValue(body, Object.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("REST source did not return valid JSON", ex);
        }

        Object recordsNode;
        if (recordsPath != null && !recordsPath.isBlank()) {
            recordsNode = resolvePath(payload, recordsPath);
        } else if (payload instanceof List<?>) {
            recordsNode = payload;
        } else if (payload instanceof Map<?, ?> map && map.containsKey("records")) {
            recordsNode = map.get("records");
        } else {
            recordsNode = payload;
        }

        if (!(recordsNode instanceof List<?> rows)) {
            throw new IllegalArgumentException("REST records payload must be an array");
        }

        List<Map<String, Object>> records = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("REST record must be an object");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> item = objectMapper.convertValue(row, new TypeReference<Map<String, Object>>() {});
            records.add(item);
        }
        return records;
    }

    private Object resolvePath(Object payload, String path) {
        Object current = payload;
        for (String segment : path.split("\\.")) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(segment);
            } else {
                return null;
            }
        }
        return current;
    }

    private Duration boundedDuration(Object value, Duration fallback, Duration max) {
        if (value == null) {
            return fallback;
        }
        try {
            long ms = Long.parseLong(String.valueOf(value));
            if (ms <= 0) {
                return fallback;
            }
            return Duration.ofMillis(Math.min(ms, max.toMillis()));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to serialize JSON payload", ex);
        }
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String upperOrDefault(String value, String fallback) {
        return value == null ? fallback : value.toUpperCase(Locale.ROOT);
    }
}
