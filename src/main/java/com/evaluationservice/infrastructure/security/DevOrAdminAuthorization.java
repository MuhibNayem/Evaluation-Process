package com.evaluationservice.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class DevOrAdminAuthorization {

    private final boolean devMode;

    public DevOrAdminAuthorization(@Value("${evaluation.service.security.dev-mode:false}") boolean devMode) {
        this.devMode = devMode;
    }

    public boolean allow(Authentication authentication) {
        if (devMode) {
            return true;
        }
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
