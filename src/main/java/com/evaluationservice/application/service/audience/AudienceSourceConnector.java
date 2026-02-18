package com.evaluationservice.application.service.audience;

import java.util.List;
import java.util.Map;

public interface AudienceSourceConnector {

    String sourceType();

    List<SourceRecord> loadRecords(Map<String, Object> sourceConfig);

    record SourceRecord(int rowNumber, Map<String, String> fields, String rawData) {
    }
}
