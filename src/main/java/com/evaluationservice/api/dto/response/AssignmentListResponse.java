package com.evaluationservice.api.dto.response;

import java.util.List;

public record AssignmentListResponse(
        List<AssignmentResponse> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {
}
