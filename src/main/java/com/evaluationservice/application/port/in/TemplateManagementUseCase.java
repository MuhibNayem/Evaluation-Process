package com.evaluationservice.application.port.in;

import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.enums.ScoringMethod;
import com.evaluationservice.domain.entity.Section;
import com.evaluationservice.domain.value.TemplateId;

import java.util.List;

/**
 * Inbound port for template management operations.
 */
public interface TemplateManagementUseCase {

    record CreateTemplateCommand(
            String name,
            String description,
            String category,
            ScoringMethod scoringMethod,
            List<Section> sections,
            String createdBy) {
    }

    record UpdateTemplateCommand(
            TemplateId templateId,
            String name,
            String description,
            String category,
            ScoringMethod scoringMethod,
            String customFormula,
            List<Section> sections) {
    }

    Template createTemplate(CreateTemplateCommand command);

    Template updateTemplate(UpdateTemplateCommand command);

    Template publishTemplate(TemplateId templateId, String publishedBy);

    Template deprecateTemplate(TemplateId templateId);

    Template getTemplate(TemplateId templateId);

    List<Template> listTemplates(String category, int page, int size);

    void deleteTemplate(TemplateId templateId);
}
