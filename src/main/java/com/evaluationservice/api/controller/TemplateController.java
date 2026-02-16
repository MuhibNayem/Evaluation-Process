package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.CreateTemplateRequest;
import com.evaluationservice.api.dto.response.TemplateResponse;
import com.evaluationservice.api.mapper.ResponseMapper;
import com.evaluationservice.application.port.in.TemplateManagementUseCase;
import com.evaluationservice.application.port.in.TemplateManagementUseCase.CreateTemplateCommand;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.infrastructure.config.EvaluationServiceProperties;
import com.evaluationservice.infrastructure.security.SecurityContextUserProvider;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for evaluation template management.
 */
@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

    private final TemplateManagementUseCase templateUseCase;
    private final ResponseMapper responseMapper;
    private final SecurityContextUserProvider userProvider;
    private final EvaluationServiceProperties properties;

    public TemplateController(
            TemplateManagementUseCase templateUseCase,
            ResponseMapper responseMapper,
            SecurityContextUserProvider userProvider,
            EvaluationServiceProperties properties) {
        this.templateUseCase = templateUseCase;
        this.responseMapper = responseMapper;
        this.userProvider = userProvider;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        var command = new CreateTemplateCommand(
                request.name(),
                request.description(),
                request.category(),
                request.scoringMethod() != null ? request.scoringMethod() : properties.getScoring().getDefaultMethod(),
                List.of(),
                userProvider.getCurrentUserId());
        Template template = templateUseCase.createTemplate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(template));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable String id) {
        Template template = templateUseCase.getTemplate(TemplateId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(template));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> listTemplates(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {
        int pageSize = resolvePageSize(size);
        List<TemplateResponse> templates = templateUseCase.listTemplates(category, page, pageSize)
                .stream()
                .map(responseMapper::toResponse)
                .toList();
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<TemplateResponse> publishTemplate(@PathVariable String id) {
        Template template = templateUseCase.publishTemplate(TemplateId.of(id), userProvider.getCurrentUserId());
        return ResponseEntity.ok(responseMapper.toResponse(template));
    }

    @PostMapping("/{id}/deprecate")
    public ResponseEntity<TemplateResponse> deprecateTemplate(@PathVariable String id) {
        Template template = templateUseCase.deprecateTemplate(TemplateId.of(id));
        return ResponseEntity.ok(responseMapper.toResponse(template));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        templateUseCase.deleteTemplate(TemplateId.of(id));
        return ResponseEntity.noContent().build();
    }

    private int resolvePageSize(Integer requestedSize) {
        if (requestedSize == null) {
            return properties.getPagination().getDefaultPageSize();
        }
        return Math.min(requestedSize, properties.getPagination().getMaxPageSize());
    }
}
