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
                mapSections(request.sections()),
                userProvider.getCurrentUserId());
        Template template = templateUseCase.createTemplate(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody com.evaluationservice.api.dto.request.UpdateTemplateRequest request) {
        var command = new com.evaluationservice.application.port.in.TemplateManagementUseCase.UpdateTemplateCommand(
                TemplateId.of(id),
                request.name(),
                request.description(),
                request.category(),
                request.scoringMethod(),
                request.customFormula(),
                mapSections(request.sections()));
        Template template = templateUseCase.updateTemplate(command);
        return ResponseEntity.ok(responseMapper.toResponse(template));
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

    // --- Mappers ---

    private List<com.evaluationservice.domain.entity.Section> mapSections(
            List<com.evaluationservice.api.dto.request.SectionRequest> dtos) {
        if (dtos == null)
            return java.util.Collections.emptyList();
        return dtos.stream().map(this::mapSection).toList();
    }

    private com.evaluationservice.domain.entity.Section mapSection(
            com.evaluationservice.api.dto.request.SectionRequest dto) {
        return new com.evaluationservice.domain.entity.Section(
                dto.id() != null ? dto.id() : java.util.UUID.randomUUID().toString(),
                dto.title(),
                dto.description(),
                dto.orderIndex(),
                dto.weight() != null ? com.evaluationservice.domain.value.Weight.of(dto.weight())
                        : com.evaluationservice.domain.value.Weight.EQUAL,
                mapQuestions(dto.questions()));
    }

    private List<com.evaluationservice.domain.entity.Question> mapQuestions(
            List<com.evaluationservice.api.dto.request.QuestionRequest> dtos) {
        if (dtos == null)
            return java.util.Collections.emptyList();
        return dtos.stream().map(this::mapQuestion).toList();
    }

    private com.evaluationservice.domain.entity.Question mapQuestion(
            com.evaluationservice.api.dto.request.QuestionRequest dto) {
        return new com.evaluationservice.domain.entity.Question(
                dto.id() != null ? dto.id() : java.util.UUID.randomUUID().toString(),
                dto.text(),
                dto.type(),
                dto.orderIndex(),
                dto.required(),
                dto.options(),
                dto.weight() != null ? com.evaluationservice.domain.value.Weight.of(dto.weight())
                        : com.evaluationservice.domain.value.Weight.EQUAL,
                dto.metadata(),
                dto.conditionalLogic());
    }
}
