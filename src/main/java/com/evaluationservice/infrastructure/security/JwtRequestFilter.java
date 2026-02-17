package com.evaluationservice.infrastructure.security;

import com.evaluationservice.infrastructure.persistence.RoleRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter that intercepts HTTP requests to validate JWT tokens.
 * Extracts the token from the Authorization header, validates it,
 * and loads dynamic permissions from the database.
 * In dev-mode, this filter is skipped entirely.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final RoleRepository roleRepository;
    private final JwtUtil jwtUtil;
    private final boolean devMode;

    public JwtRequestFilter(RoleRepository roleRepository, JwtUtil jwtUtil,
            @Value("${evaluation.service.security.dev-mode:false}") boolean devMode) {
        this.roleRepository = roleRepository;
        this.jwtUtil = jwtUtil;
        this.devMode = devMode;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // Skip JWT validation entirely in dev mode
        if (devMode) {
            chain.doFilter(request, response);
            return;
        }

        final String requestTokenHeader = request.getHeader("Authorization");

        String username = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtUtil.getUsernameFromToken(jwtToken);
            } catch (IllegalArgumentException e) {
                logger.error("Unable to get JWT Token");
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                logger.error("JWT Token has expired");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            if (jwtUtil.validateToken(jwtToken)) {
                List<String> roles = jwtUtil.getClaimFromToken(jwtToken, claims -> claims.get("roles", List.class));

                Set<GrantedAuthority> authorities = new HashSet<>();
                if (roles != null) {
                    authorities.addAll(roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));

                    Set<String> permissions = roleRepository.findPermissionsByRoleNames(roles);
                    authorities
                            .addAll(permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, null,
                        authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}
