package com.codemich.quickpaybank.notification;

import com.codemich.quickpaybank.notification.dto.NotificationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RestTemplate restTemplate;

    @Value("${notification.url}")
    private String notificationUrl;

    @Async("notificationExecutor")
    public void notify(String email, String message) {
        try {
            var payload = new NotificationPayload(email, message);
            restTemplate.postForEntity(notificationUrl, payload, Void.class);
            log.info("Notificação enviada para {}", email);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação para {}: {}", email, e.getMessage());
        }
    }
}
