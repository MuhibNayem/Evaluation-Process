package com.evaluationservice.api.controller;

import com.evaluationservice.api.dto.request.CreateAssignmentRequest;
import com.evaluationservice.api.dto.request.UpdateAssignmentRequest;
import com.evaluationservice.api.dto.response.AssignmentListResponse;
import com.evaluationservice.api.dto.response.AssignmentResponse;
import com.evaluationservice.infrastructure.service.AssignmentManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assignments")
@PreAuthorize("hasRole('ADMIN')")
public class AssignmentController {

    private final AssignmentManagementService assignmentManagementService;

    public AssignmentController(AssignmentManagementService assignmentManagementService) {
        this.assignmentManagementService = assignmentManagementService;
    }

    @GetMapping
    public ResponseEntity<AssignmentListResponse> list(
            @RequestParam(required = false) String campaignId,
            @RequestParam(required = false) String stepType,
            @RequestParam(required = false) String sectionId,
            @RequestParam(required = false) String facultyId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String evaluatorId,
            @RequestParam(required = false) String evaluateeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(assignmentManagementService.list(
                campaignId, stepType, sectionId, facultyId, status, evaluatorId, evaluateeId, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(assignmentManagementService.get(id));
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@Valid @RequestBody CreateAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentManagementService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssignmentResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateAssignmentRequest request) {
        return ResponseEntity.ok(assignmentManagementService.update(id, request));
    }
}
