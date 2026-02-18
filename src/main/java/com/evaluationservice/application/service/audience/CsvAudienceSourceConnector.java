package com.evaluationservice.application.service.audience;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CsvAudienceSourceConnector implements AudienceSourceConnector {

    @Override
    public String sourceType() {
        return "CSV";
    }

    @Override
    public List<SourceRecord> loadRecords(Map<String, Object> sourceConfig) {
        String csvData = asText(sourceConfig == null ? null : sourceConfig.get("csvData"));
        if (csvData == null || csvData.isBlank()) {
            throw new IllegalArgumentException("CSV source requires sourceConfig.csvData");
        }

        List<List<String>> rows = parseCsvRecords(csvData);
        if (rows.size() < 2) {
            throw new IllegalArgumentException("CSV must include header and at least one data row");
        }

        List<String> header = rows.getFirst();
        List<String> normalizedHeader = normalizeHeader(header);
        List<SourceRecord> records = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            if (row.stream().allMatch(String::isBlank)) {
                continue;
            }

            Map<String, String> fields = new LinkedHashMap<>();
            for (int col = 0; col < normalizedHeader.size(); col++) {
                String key = normalizedHeader.get(col);
                if (key.isEmpty()) {
                    continue;
                }
                fields.put(key, col < row.size() ? normalizeCell(row.get(col)) : null);
            }
            records.add(new SourceRecord(i + 1, fields, String.join(",", row)));
        }

        return records;
    }

    private List<String> normalizeHeader(List<String> header) {
        List<String> normalized = new ArrayList<>();
        for (String value : header) {
            String key = removeBom(value).trim().toLowerCase(Locale.ROOT);
            if (!key.isEmpty() && normalized.contains(key)) {
                throw new IllegalArgumentException("CSV header contains duplicate column: " + key);
            }
            normalized.add(key);
        }
        return normalized;
    }

    private String normalizeCell(String value) {
        String trimmed = value == null ? null : value.trim();
        return (trimmed == null || trimmed.isEmpty()) ? null : trimmed;
    }

    private String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String removeBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private List<List<String>> parseCsvRecords(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < csv.length(); i++) {
            char ch = csv.charAt(i);
            char next = i + 1 < csv.length() ? csv.charAt(i + 1) : '\0';

            if (inQuotes) {
                if (ch == '"' && next == '"') {
                    currentField.append('"');
                    i++;
                } else if (ch == '"') {
                    inQuotes = false;
                } else {
                    currentField.append(ch);
                }
                continue;
            }

            if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
            } else if (ch == '\n') {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
                records.add(currentRecord);
                currentRecord = new ArrayList<>();
            } else if (ch == '\r') {
                // Skip CR; LF handles row termination.
            } else {
                currentField.append(ch);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Malformed CSV: unterminated quoted field");
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString());
            records.add(currentRecord);
        }
        return records;
    }
}
