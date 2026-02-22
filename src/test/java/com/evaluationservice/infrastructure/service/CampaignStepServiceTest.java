package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.UpdateCampaignStepsRequest;
import com.evaluationservice.infrastructure.entity.CampaignEntity;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.CampaignStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignStepServiceTest {

    @Mock
    private CampaignStepRepository campaignStepRepository;

    @Mock
    private CampaignRepository campaignRepository;

    private CampaignStepService service;

    @BeforeEach
    void setUp() {
        service = new CampaignStepService(campaignStepRepository, campaignRepository);
    }

    @Test
    void rejectsDuplicateStepTypes() {
        when(campaignRepository.findById("c1")).thenReturn(java.util.Optional.of(campaign("DRAFT", false)));
        List<UpdateCampaignStepsRequest.StepItem> steps = List.of(
                new UpdateCampaignStepsRequest.StepItem("PEER", true, 1, null, null, false, 0, null, null),
                new UpdateCampaignStepsRequest.StepItem("PEER", true, 2, null, null, false, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.replace("c1", steps));
    }

    @Test
    void rejectsInvalidWindow() {
        when(campaignRepository.findById("c1")).thenReturn(java.util.Optional.of(campaign("DRAFT", false)));
        Instant now = Instant.now();
        List<UpdateCampaignStepsRequest.StepItem> steps = List.of(
                new UpdateCampaignStepsRequest.StepItem("SELF", true, 1, now, now, false, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.replace("c1", steps));
    }

    @Test
    void rejectsUnsupportedStepType() {
        when(campaignRepository.findById("c1")).thenReturn(java.util.Optional.of(campaign("DRAFT", false)));
        List<UpdateCampaignStepsRequest.StepItem> steps = List.of(
                new UpdateCampaignStepsRequest.StepItem("MANAGER", true, 1, null, null, false, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.replace("c1", steps));
    }

    @Test
    void rejectsNonContinuousDisplayOrder() {
        when(campaignRepository.findById("c1")).thenReturn(java.util.Optional.of(campaign("DRAFT", false)));
        List<UpdateCampaignStepsRequest.StepItem> steps = List.of(
                new UpdateCampaignStepsRequest.StepItem("SELF", true, 1, null, null, false, 0, null, null),
                new UpdateCampaignStepsRequest.StepItem("PEER", true, 3, null, null, false, 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.replace("c1", steps));
    }

    @Test
    void rejectsWhenCampaignLocked() {
        when(campaignRepository.findById("c1")).thenReturn(java.util.Optional.of(campaign("PUBLISHED_OPEN", true)));
        List<UpdateCampaignStepsRequest.StepItem> steps = List.of(
                new UpdateCampaignStepsRequest.StepItem("SELF", true, 1, null, null, false, 0, null, null));
        assertThrows(IllegalStateException.class, () -> service.replace("c1", steps));
    }

    private CampaignEntity campaign(String status, boolean locked) {
        CampaignEntity entity = new CampaignEntity();
        entity.setId("c1");
        entity.setStatus(status);
        entity.setLocked(locked);
        return entity;
    }
}
