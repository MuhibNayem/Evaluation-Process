package com.evaluationservice.domain.exception;

import com.evaluationservice.domain.value.TemplateId;

public class TemplateAlreadyPublishedException extends DomainException {

    public TemplateAlreadyPublishedException(TemplateId templateId) {
        super("Template '%s' is already published and cannot be modified".formatted(templateId.value()),
                "TEMPLATE_ALREADY_PUBLISHED");
    }
}
