package com.evaluationservice.application.port.out;

import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.value.TemplateId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for template persistence operations.
 */
public interface TemplatePersistencePort {

    Template save(Template template);

    Optional<Template> findById(TemplateId templateId);

    List<Template> findByCategory(String category, int page, int size);

    List<Template> findAll(int page, int size);

    boolean existsById(TemplateId templateId);

    void deleteById(TemplateId templateId);
}
