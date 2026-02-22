package com.evaluationservice.api.dto.response;

import java.util.List;
import java.util.Map;

public record QuestionVersionCompareResponse(
        Long questionItemId,
        int fromVersion,
        int toVersion,
        List<FieldDiff> diffs) {

    public record FieldDiff(
            String field,
            Object fromValue,
            Object toValue) {
    }

    public static QuestionVersionCompareResponse fromDiffMap(
            Long questionItemId,
            int fromVersion,
            int toVersion,
            Map<String, Object[]> diffMap) {
        List<FieldDiff> diffs = diffMap.entrySet().stream()
                .map(e -> new FieldDiff(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
        return new QuestionVersionCompareResponse(questionItemId, fromVersion, toVersion, diffs);
    }
}
