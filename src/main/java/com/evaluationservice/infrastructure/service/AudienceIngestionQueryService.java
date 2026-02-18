package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.response.AudienceIngestionRejectionResponse;
import com.evaluationservice.api.dto.response.AudienceIngestionRunResponse;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRejectionRepository;
import com.evaluationservice.infrastructure.repository.AudienceIngestionRunRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AudienceIngestionQueryService {

    private final AudienceIngestionRunRepository runRepository;
    private final AudienceIngestionRejectionRepository rejectionRepository;

    public AudienceIngestionQueryService(
            AudienceIngestionRunRepository runRepository,
            AudienceIngestionRejectionRepository rejectionRepository) {
        this.runRepository = runRepository;
        this.rejectionRepository = rejectionRepository;
    }

    public List<AudienceIngestionRunResponse> listRuns(String tenantId, int limit) {
        int size = Math.max(1, Math.min(limit, 200));
        if (tenantId != null && !tenantId.isBlank()) {
            return runRepository.findByTenantIdOrderByStartedAtDesc(tenantId, PageRequest.of(0, size))
                    .map(run -> new AudienceIngestionRunResponse(
                            run.getId(),
                            run.getTenantId(),
                            run.getSourceType(),
                            run.getStatus(),
                            run.isDryRun(),
                            run.getProcessedRecords(),
                            run.getRejectedRecords(),
                            run.getErrorMessage(),
                            run.getStartedAt(),
                            run.getEndedAt()))
                    .getContent();
        }

        return runRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, size))
                .map(run -> new AudienceIngestionRunResponse(
                        run.getId(),
                        run.getTenantId(),
                        run.getSourceType(),
                        run.getStatus(),
                        run.isDryRun(),
                        run.getProcessedRecords(),
                        run.getRejectedRecords(),
                        run.getErrorMessage(),
                        run.getStartedAt(),
                        run.getEndedAt()))
                .getContent();
    }

    public AudienceIngestionRunResponse getRun(String runId) {
        var run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Ingestion run not found: " + runId));
        return new AudienceIngestionRunResponse(
                run.getId(),
                run.getTenantId(),
                run.getSourceType(),
                run.getStatus(),
                run.isDryRun(),
                run.getProcessedRecords(),
                run.getRejectedRecords(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getEndedAt());
    }

    public List<AudienceIngestionRejectionResponse> listRejections(String runId, int limit) {
        int size = Math.max(1, Math.min(limit, 500));
        return rejectionRepository.findByRunIdOrderByRowNumberAsc(runId, PageRequest.of(0, size))
                .map(rejection -> new AudienceIngestionRejectionResponse(
                        rejection.getId(),
                        rejection.getRunId(),
                        rejection.getTenantId(),
                        rejection.getRowNumber(),
                        rejection.getReason(),
                        rejection.getRowData(),
                        rejection.getCreatedAt()))
                .getContent();
    }
}
