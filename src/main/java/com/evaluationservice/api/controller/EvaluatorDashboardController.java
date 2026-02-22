package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.response.EvaluatorDashboardResponse;
import com.evaluationservice.infrastructure.security.SecurityContextUserProvider;
import com.evaluationservice.infrastructure.service.EvaluatorDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluators/me")
public class EvaluatorDashboardController {

    private final EvaluatorDashboardService evaluatorDashboardService;
    private final SecurityContextUserProvider userProvider;

    public EvaluatorDashboardController(
            EvaluatorDashboardService evaluatorDashboardService,
            SecurityContextUserProvider userProvider) {
        this.evaluatorDashboardService = evaluatorDashboardService;
        this.userProvider = userProvider;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<EvaluatorDashboardResponse> dashboard() {
        return ResponseEntity.ok(evaluatorDashboardService.getSummary(userProvider.getCurrentUserId()));
    }
}
