package com.evaluationservice.application.service.audience;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class JsonAudienceSourceConnector implements AudienceSourceConnector {

    @Override
    public String sourceType() {
        return "JSON";
    }

    @Override
    public List<SourceRecord> loadRecords(Map<String, Object> sourceConfig) {
        Object rawRecords = sourceConfig == null ? null : sourceConfig.get("records");
        if (!(rawRecords instanceof List<?> records)) {
            throw new IllegalArgumentException("JSON source requires sourceConfig.records array");
        }

        List<SourceRecord> mapped = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            Object row = records.get(i);
            if (!(row instanceof Map<?, ?> data)) {
                throw new IllegalArgumentException("Each JSON record must be an object");
            }

            Map<String, String> fields = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : data.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim().toLowerCase(Locale.ROOT);
                if (key.isEmpty()) {
                    continue;
                }
                String value = entry.getValue() == null ? null : String.valueOf(entry.getValue()).trim();
                fields.put(key, value == null || value.isEmpty() ? null : value);
            }
            mapped.add(new SourceRecord(i + 1, fields, String.valueOf(row)));
        }
        return mapped;
    }
}
