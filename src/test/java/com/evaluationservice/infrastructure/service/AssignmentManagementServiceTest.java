package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.CreateAssignmentRequest;
import com.evaluationservice.api.exception.DuplicateAssignmentException;
import com.evaluationservice.infrastructure.entity.CampaignAssignmentEntity;
import com.evaluationservice.infrastructure.repository.CampaignAssignmentRepository;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentManagementServiceTest {

    @Mock
    private CampaignAssignmentRepository assignmentRepository;
    @Mock
    private CampaignRepository campaignRepository;

    private AssignmentManagementService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentManagementService(assignmentRepository, campaignRepository);
    }

    @Test
    void rejectsDuplicateAssignmentTuple() {
        when(campaignRepository.existsById("c1")).thenReturn(true);
        CampaignAssignmentEntity existing = new CampaignAssignmentEntity();
        existing.setId("a-existing");
        when(assignmentRepository.findByCampaignIdAndEvaluatorIdAndEvaluateeIdAndEvaluatorRole(
                "c1", "e1", "u1", "PEER")).thenReturn(Optional.of(existing));

        CreateAssignmentRequest request = new CreateAssignmentRequest(
                "c1",
                "e1",
                "u1",
                "PEER",
                "PEER",
                null,
                null,
                "VISIBLE",
                "ACTIVE");

        DuplicateAssignmentException ex = assertThrows(DuplicateAssignmentException.class, () -> service.create(request));
        assertEquals("a-existing", ex.getExistingAssignmentId());
    }

    @Test
    void rejectsInvalidStatusUpdate() {
        CampaignAssignmentEntity entity = new CampaignAssignmentEntity();
        entity.setId("a1");
        when(assignmentRepository.findById("a1")).thenReturn(Optional.of(entity));
        assertThrows(IllegalArgumentException.class, () -> service.update(
                "a1",
                new com.evaluationservice.api.dto.request.UpdateAssignmentRequest(
                        null, null, null, null, "DONE")));
    }
}
