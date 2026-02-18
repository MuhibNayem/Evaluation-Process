package com.evaluationservice.application.port.out;

import com.evaluationservice.domain.entity.CampaignAssignment;
import com.evaluationservice.domain.value.CampaignId;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for first-class campaign assignment persistence.
 * Used during migration from JSON-embedded assignments to relational storage.
 */
public interface AssignmentPersistencePort {

    void upsertAssignments(CampaignId campaignId, List<CampaignAssignment> assignments);

    void replaceAssignments(CampaignId campaignId, List<CampaignAssignment> assignments);

    void markCompleted(String assignmentId, String evaluationId);

    Optional<CampaignAssignment> findById(String assignmentId);
}
