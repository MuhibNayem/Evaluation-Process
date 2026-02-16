package com.evaluationservice.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;
import java.util.Map;

/**
 * Feign client for sending webhooks.
 * We use a generic client without a 'url' attribute because the URL is dynamic
 * per request.
 * The 'name' is just an identifier for the client bean.
 */
@FeignClient(name = "webhookClient", url = "https://placeholder-host-replaced-at-runtime")
public interface WebhookClient {

    @PostMapping
    void postWebhook(URI baseUrl, @RequestBody Map<String, String> payload);
}
