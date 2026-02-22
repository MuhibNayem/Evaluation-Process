package com.evaluationservice.infrastructure.service;

import com.evaluationservice.api.dto.request.CreateNotificationRuleRequest;
import com.evaluationservice.api.dto.request.CreateNotificationTemplateRequest;
import com.evaluationservice.api.dto.response.NotificationDeliveryResponse;
import com.evaluationservice.api.dto.response.NotificationRuleResponse;
import com.evaluationservice.application.port.out.NotificationPort;
import com.evaluationservice.infrastructure.entity.NotificationDeliveryEntity;
import com.evaluationservice.infrastructure.entity.NotificationRuleEntity;
import com.evaluationservice.infrastructure.repository.CampaignRepository;
import com.evaluationservice.infrastructure.repository.NotificationDeliveryRepository;
import com.evaluationservice.infrastructure.repository.NotificationRuleRepository;
import com.evaluationservice.infrastructure.repository.NotificationTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationModuleServiceTest {

    @Mock
    private NotificationRuleRepository ruleRepository;
    @Mock
    private NotificationTemplateRepository templateRepository;
    @Mock
    private NotificationDeliveryRepository deliveryRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private NotificationPort notificationPort;

    private NotificationModuleService service;

    @BeforeEach
    void setUp() {
        service = new NotificationModuleService(
                ruleRepository,
                templateRepository,
                deliveryRepository,
                campaignRepository,
                notificationPort,
                new ObjectMapper());
    }

    @Test
    void createRuleRejectsUnknownCampaign() {
        when(campaignRepository.existsById("c-1")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.createRule(
                new CreateNotificationRuleRequest(
                        "c-1",
                        "RULE_A",
                        "EVALUATION_SUBMITTED",
                        "EVALUATOR",
                        "EMAIL",
                        null,
                        true,
                        Map.of())));

        verify(ruleRepository, never()).save(any());
    }

    @Test
    void createRulePersistsValidRule() {
        when(campaignRepository.existsById("c-1")).thenReturn(true);
        when(ruleRepository.findByCampaignIdAndRuleCode("c-1", "RULE_A")).thenReturn(Optional.empty());
        when(ruleRepository.save(any(NotificationRuleEntity.class))).thenAnswer(invocation -> {
            NotificationRuleEntity entity = invocation.getArgument(0);
            entity.setId(10L);
            return entity;
        });

        NotificationRuleResponse response = service.createRule(new CreateNotificationRuleRequest(
                "c-1",
                "RULE_A",
                "EVALUATION_SUBMITTED",
                "EVALUATOR",
                "EMAIL",
                null,
                true,
                Map.of("templateCode", "TMP_1")));

        assertEquals(10L, response.id());
        assertEquals("EVALUATION_SUBMITTED", response.triggerType());
        assertEquals("EVALUATOR", response.audience());
        assertEquals("EMAIL", response.channel());
    }

    @Test
    void createTemplateRejectsPublishedTemplateWithMissingRequiredVariable() {
        when(campaignRepository.existsById("c-1")).thenReturn(true);
        when(templateRepository.findByCampaignIdAndTemplateCode("c-1", "T_1")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.createTemplate(new CreateNotificationTemplateRequest(
                "c-1",
                "T_1",
                "Template",
                "EMAIL",
                "Hi {{name}}",
                "Body {{name}} {{score}}",
                List.of("name"),
                "PUBLISHED")));
    }

    @Test
    void processEventDispatchesAndPersistsDelivery() {
        NotificationRuleEntity rule = new NotificationRuleEntity();
        rule.setId(22L);
        rule.setCampaignId("c-1");
        rule.setRuleCode("RULE_A");
        rule.setTriggerType("EVALUATION_SUBMITTED");
        rule.setAudience("EVALUATOR");
        rule.setChannel("EMAIL");
        rule.setEnabled(true);
        rule.setConfigJson("{}");
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());

        when(ruleRepository.findByEnabledTrueAndTriggerTypeOrderByUpdatedAtDesc("EVALUATION_SUBMITTED"))
                .thenReturn(List.of(rule));
        when(deliveryRepository.save(any(NotificationDeliveryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processEvent("EVALUATION_SUBMITTED", "c-1", Map.of(
                "evaluatorId", "user-11",
                "campaignName", "Spring Eval",
                "message", "Submitted"));

        verify(notificationPort).sendReminder("user-11", "Spring Eval", "Submitted");
        ArgumentCaptor<NotificationDeliveryEntity> captor = ArgumentCaptor.forClass(NotificationDeliveryEntity.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals("SENT", captor.getValue().getStatus());
        assertEquals("user-11", captor.getValue().getRecipient());
    }

    @Test
    void retryDeliveryRequiresFailedStatus() {
        NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
        delivery.setId(5L);
        delivery.setStatus("SENT");
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> service.retryDelivery(5L));
        verify(notificationPort, never()).sendReminder(anyString(), anyString(), anyString());
    }

    @Test
    void retryDeliveryResendsFailedDelivery() {
        NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
        delivery.setId(5L);
        delivery.setStatus("FAILED");
        delivery.setRecipient("user-1");
        delivery.setMessagePayloadJson("{\"recipient\":\"user-1\",\"campaignName\":\"Spring\",\"message\":\"Retry message\"}");
        when(deliveryRepository.findById(5L)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(NotificationDeliveryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDeliveryResponse response = service.retryDelivery(5L);

        verify(notificationPort).sendReminder("user-1", "Spring", "Retry message");
        assertEquals("SENT", response.status());
    }
}
