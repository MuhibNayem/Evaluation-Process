package com.evaluationservice.application.port.out;

/**
 * Outbound port for sending notifications (email, push, etc.).
 */
public interface NotificationPort {

    void sendReminder(String recipientId, String campaignName, String message);

    void sendCompletionNotification(String recipientId, String campaignName);

    void sendDeadlineExtensionNotification(String recipientId, String campaignName, String newDeadline);
}
