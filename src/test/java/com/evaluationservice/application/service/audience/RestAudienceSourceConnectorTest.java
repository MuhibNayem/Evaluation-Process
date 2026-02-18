package com.evaluationservice.application.service.audience;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RestAudienceSourceConnector")
class RestAudienceSourceConnectorTest {

    @Test
    @DisplayName("loads records from GET response records array")
    void loadsRecordsFromGet() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/audience", exchange ->
                respond(exchange, 200, "{\"records\":[{\"person_id\":\"p-1\",\"display_name\":\"User One\",\"active\":\"true\"}]}"));
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/audience";
            RestAudienceSourceConnector connector = new RestAudienceSourceConnector(new ObjectMapper());

            List<AudienceSourceConnector.SourceRecord> records = connector.loadRecords(Map.of("url", url));

            assertThat(records).hasSize(1);
            assertThat(records.getFirst().fields().get("person_id")).isEqualTo("p-1");
            assertThat(records.getFirst().fields().get("display_name")).isEqualTo("User One");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("loads records from POST response nested path")
    void loadsRecordsFromPostNestedPath() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/query", exchange -> {
            String method = exchange.getRequestMethod();
            if (!"POST".equals(method)) {
                respond(exchange, 405, "{\"error\":\"method\"}");
                return;
            }
            respond(exchange, 200, "{\"payload\":{\"items\":[{\"id\":\"p-9\",\"name\":\"Json Rest User\",\"active\":\"yes\"}]}}");
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/query";
            RestAudienceSourceConnector connector = new RestAudienceSourceConnector(new ObjectMapper());

            List<AudienceSourceConnector.SourceRecord> records = connector.loadRecords(Map.of(
                    "url", url,
                    "method", "POST",
                    "recordsPath", "payload.items",
                    "body", Map.of("scope", "all")));

            assertThat(records).hasSize(1);
            assertThat(records.getFirst().fields().get("id")).isEqualTo("p-9");
            assertThat(records.getFirst().fields().get("name")).isEqualTo("Json Rest User");
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int statusCode, String payload) throws IOException {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }
}
