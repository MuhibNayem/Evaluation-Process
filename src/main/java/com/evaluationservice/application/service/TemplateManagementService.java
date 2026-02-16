package com.evaluationservice.application.service;

import com.evaluationservice.application.port.in.TemplateManagementUseCase;
import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.TemplateStatus;
import com.evaluationservice.domain.exception.EntityNotFoundException;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.domain.value.Timestamp;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Application service implementing template management use cases.
 */
@Service
@Transactional
public class TemplateManagementService implements TemplateManagementUseCase {

    private final TemplatePersistencePort templatePersistencePort;
    private final ApplicationEventPublisher eventPublisher;

    public TemplateManagementService(
            TemplatePersistencePort templatePersistencePort,
            ApplicationEventPublisher eventPublisher) {
        this.templatePersistencePort = Objects.requireNonNull(templatePersistencePort);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
    }

    @Override
    public Template createTemplate(CreateTemplateCommand command) {
        var template = new Template(
                TemplateId.generate(),
                command.name(),
                command.description(),
                command.category(),
                TemplateStatus.DRAFT,
                0,
                command.scoringMethod(),
                command.sections(),
                command.createdBy(),
                Timestamp.now(),
                Timestamp.now(),
                null);
        return templatePersistencePort.save(template);
    }

    @Override
    public Template updateTemplate(UpdateTemplateCommand command) {
        var template = findTemplateOrThrow(command.templateId());
        template.updateDetails(command.name(), command.description(), command.category());
        if (command.scoringMethod() != null) {
            template.setScoringMethod(command.scoringMethod(), command.customFormula());
        }
        if (command.sections() != null) {
            template.replaceSections(command.sections());
        }
        return templatePersistencePort.save(template);
    }

    @Override
    public Template publishTemplate(TemplateId templateId, String publishedBy) {
        var template = findTemplateOrThrow(templateId);
        template.publish();
        Template saved = templatePersistencePort.save(template);
        eventPublisher.publishEvent(
                new com.evaluationservice.domain.event.TemplatePublishedEvent(
                        templateId, saved.getCurrentVersion(), publishedBy, java.time.Instant.now()));
        return saved;
    }

    @Override
    public Template deprecateTemplate(TemplateId templateId) {
        var template = findTemplateOrThrow(templateId);
        template.deprecate();
        return templatePersistencePort.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public Template getTemplate(TemplateId templateId) {
        return findTemplateOrThrow(templateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Template> listTemplates(String category, int page, int size) {
        if (category != null && !category.isBlank()) {
            return templatePersistencePort.findByCategory(category, page, size);
        }
        return templatePersistencePort.findAll(page, size);
    }

    @Override
    public void deleteTemplate(TemplateId templateId) {
        var template = findTemplateOrThrow(templateId);
        if (template.getStatus() == TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot delete a published template. Deprecate it instead.");
        }
        templatePersistencePort.deleteById(templateId);
    }

    private Template findTemplateOrThrow(TemplateId templateId) {
        return templatePersistencePort.findById(templateId)
                .orElseThrow(() -> new EntityNotFoundException("Template", templateId.value()));
    }
}
