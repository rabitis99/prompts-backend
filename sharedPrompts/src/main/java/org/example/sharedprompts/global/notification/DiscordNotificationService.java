package org.example.sharedprompts.global.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.notification.dto.DiscordWebhookPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordNotificationService {

    private final RestTemplate restTemplate;
    
    @Value("${discord.webhook.url:}")
    private String webhookUrl;
    
    @Value("${discord.webhook.enabled:false}")
    private boolean enabled;

    public void sendRedisDownNotification(String errorMessage, long consecutiveFailures) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Discord 알림이 비활성화되어 있거나 웹훅 URL이 설정되지 않았습니다.");
            return;
        }

        try {
            DiscordWebhookPayload payload = RedisNotificationEmbedBuilder.buildDownNotification(errorMessage, consecutiveFailures);
            sendWebhook(payload);
            log.info("Discord Redis 장애 알림 전송 완료");
        } catch (Exception e) {
            log.error("Discord Redis 장애 알림 전송 실패", e);
        }
    }

    public void sendRedisRecoveryNotification(long downtimeDurationMs) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Discord 알림이 비활성화되어 있거나 웹훅 URL이 설정되지 않았습니다.");
            return;
        }

        try {
            DiscordWebhookPayload payload = RedisNotificationEmbedBuilder.buildRecoveryNotification(downtimeDurationMs);
            sendWebhook(payload);
            log.info("Discord Redis 복구 알림 전송 완료");
        } catch (Exception e) {
            log.error("Discord Redis 복구 알림 전송 실패", e);
        }
    }

    private void sendWebhook(DiscordWebhookPayload payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<DiscordWebhookPayload> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Discord 웹훅 전송 성공");
            } else {
                log.warn("Discord 웹훅 전송 실패: status={}, body={}", 
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Discord 웹훅 전송 중 예외 발생", e);
            throw e;
        }
    }
}

