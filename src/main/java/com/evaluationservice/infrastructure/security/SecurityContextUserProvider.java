package com.evaluationservice.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Extracts the authenticated user's identity from the Spring Security context.
 * Falls back to "anonymous" if no authentication is present (e.g., during
 * dev/test).
 */
@Component
public class SecurityContextUserProvider {

    private static final String ANONYMOUS_USER = "anonymous";

    /**
     * Gets the current authenticated user ID from the SecurityContext.
     *
     * @return the user ID, or "anonymous" if not authenticated
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ANONYMOUS_USER;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String username) {
            return username;
        }
        return authentication.getName();
    }
}
