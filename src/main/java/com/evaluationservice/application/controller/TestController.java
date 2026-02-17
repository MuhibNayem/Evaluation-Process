package com.evaluationservice.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/secured")
    @Secured("CAN_VIEW_REPORTS")
    public ResponseEntity<String> getSecuredResource() {
        return ResponseEntity.ok("Access Granted: You have CAN_VIEW_REPORTS permission.");
    }

    @GetMapping("/public")
    public ResponseEntity<String> getPublicResource() {
        return ResponseEntity.ok("Public Access Granted.");
    }
}
