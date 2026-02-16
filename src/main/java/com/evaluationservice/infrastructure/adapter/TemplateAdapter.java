package com.evaluationservice.infrastructure.adapter;

import com.evaluationservice.application.port.out.TemplatePersistencePort;
import com.evaluationservice.domain.entity.Template;
import com.evaluationservice.domain.value.TemplateId;
import com.evaluationservice.infrastructure.mapper.DomainEntityMapper;
import com.evaluationservice.infrastructure.repository.TemplateRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TemplateAdapter implements TemplatePersistencePort {

    private final TemplateRepository repository;
    private final DomainEntityMapper mapper;

    public TemplateAdapter(TemplateRepository repository, DomainEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Template save(Template template) {
        var entity = mapper.toJpaEntity(template);
        var saved = repository.save(entity);
        return mapper.toDomainTemplate(saved);
    }

    @Override
    public Optional<Template> findById(TemplateId templateId) {
        return repository.findById(templateId.value())
                .map(mapper::toDomainTemplate);
    }

    @Override
    public List<Template> findByCategory(String category, int page, int size) {
        return repository.findByCategory(category, PageRequest.of(page, size))
                .map(mapper::toDomainTemplate)
                .getContent();
    }

    @Override
    public List<Template> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size))
                .map(mapper::toDomainTemplate)
                .getContent();
    }

    @Override
    public boolean existsById(TemplateId templateId) {
        return repository.existsById(templateId.value());
    }

    @Override
    public void deleteById(TemplateId templateId) {
        repository.deleteById(templateId.value());
    }
}
