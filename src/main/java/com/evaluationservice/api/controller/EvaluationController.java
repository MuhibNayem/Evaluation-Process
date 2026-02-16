package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.SubmitEvaluationRequest;
import com.evaluationservice.api.dto.response.EvaluationResponse;
import com.evaluationservice.api.mapper.ResponseMapper;
import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase;
import com.evaluationservice.application.port.in.EvaluationSubmissionUseCase.SubmitEvaluationCommand;
import com.evaluationservice.domain.entity.Answer;
import com.evaluationservice.domain.entity.Evaluation;
import com.evaluationservice.domain.value.CampaignId;
import com.evaluationservice.domain.value.EvaluationId;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for evaluation submission and management.
 */
@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationSubmissionUseCase evaluationUseCase;
    private final ResponseMapper responseMapper;
    private final EvaluationServiceProperties properties;

    public EvaluationController(
            EvaluationSubmissionUseCase evaluationUseCase,
            ResponseMapper responseMapper,
            EvaluationServiceProperties properties) {
        this.evaluationUseCase = evaluationUseCase;
        this.responseMapper = responseMapper;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<EvaluationResponse> submitEvaluation(@Valid @RequestBody SubmitEvaluationRequest request) {
        List<Answer> answers = request.answers().stream()
                .map(a -> new Answer(
                        UUID.randomUUID().toString(),
                        a.questionId(),
                        a.value(),
                        a.selectedOptions(),
                        a.textResponse(),
                        a.metadata()))
                .toList();

        var command = new SubmitEvaluationCommand(
                CampaignId.of(request.campaignId()),
                request.assignmentId(),
                request.evaluatorId(),
                request.evaluateeId(),
                request.templateId(),
                answers);
        Evaluation evaluation = evaluationUseCase.submitEvaluation(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(evaluation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationResponse> getEvaluation(@PathVariable String id) {
        Evaluation evaluation = evaluationUseCase.getEvaluation(EvaluationId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(evaluation));
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<EvaluationResponse>> listByCampaign(
            @PathVariable String campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int pageSize = resolvePageSize(size);
        List<EvaluationResponse> evaluations = evaluationUseCase
                .listEvaluationsForCampaign(CampaignId.of(campaignId), page, pageSize)
                .stream()
                .map(responseMapper::toResponse)
                .toList();
        return ResponseEntity.ok(evaluations);
    }

    @GetMapping("/evaluatee/{evaluateeId}")
    public ResponseEntity<List<EvaluationResponse>> listByEvaluatee(
            @PathVariable String evaluateeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int pageSize = resolvePageSize(size);
        List<EvaluationResponse> evaluations = evaluationUseCase
                .listEvaluationsForEvaluatee(evaluateeId, page, pageSize)
                .stream()
                .map(responseMapper::toResponse)
                .toList();
        return ResponseEntity.ok(evaluations);
    }

    @PostMapping("/{id}/flag")
    public ResponseEntity<Void> flagEvaluation(@PathVariable String id) {
        evaluationUseCase.flagEvaluation(EvaluationId.of(id));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/invalidate")
    public ResponseEntity<Void> invalidateEvaluation(@PathVariable String id) {
        evaluationUseCase.invalidateEvaluation(EvaluationId.of(id));
        return ResponseEntity.ok().build();
    }

    private int resolvePageSize(Integer requestedSize) {
        if (requestedSize == null) {
            return properties.getPagination().getDefaultPageSize();
        }
        return Math.min(requestedSize, properties.getPagination().getMaxPageSize());
    }
}