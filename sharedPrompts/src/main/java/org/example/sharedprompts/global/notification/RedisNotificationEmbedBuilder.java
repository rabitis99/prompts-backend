package org.example.sharedprompts.global.notification;

import org.example.sharedprompts.global.notification.dto.DiscordEmbed;
import org.example.sharedprompts.global.notification.dto.DiscordEmbedField;
import org.example.sharedprompts.global.notification.dto.DiscordEmbedFooter;
import org.example.sharedprompts.global.notification.dto.DiscordWebhookPayload;
import org.example.sharedprompts.global.util.DurationFormatter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RedisNotificationEmbedBuilder {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Seoul");
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;
    private static final String FOOTER_TEXT = "SharedPrompts Backend";

    public static DiscordWebhookPayload buildDownNotification(String errorMessage, long consecutiveFailures) {
        String timestamp = LocalDateTime.now(TIMEZONE).format(TIMESTAMP_FORMATTER);
        
        DiscordEmbed embed = DiscordEmbed.builder()
                .title("🚨 Redis 장애 감지")
                .color(15158332)
                .description("Redis 서버에 연결할 수 없거나 응답하지 않습니다.")
                .fields(List.of(
                        DiscordEmbedField.builder()
                                .name("⏰ 감지 시간")
                                .value(timestamp)
                                .inline(true)
                                .build(),
                        DiscordEmbedField.builder()
                                .name("❌ 연속 실패 횟수")
                                .value(String.valueOf(consecutiveFailures))
                                .inline(true)
                                .build(),
                        DiscordEmbedField.builder()
                                .name("📝 오류 메시지")
                                .value(formatErrorMessage(errorMessage))
                                .inline(false)
                                .build(),
                        DiscordEmbedField.builder()
                                .name("🔧 조치 방법")
                                .value("Redis 서버 상태를 확인하고 재시작하세요. (Docker: `docker restart redis`, systemd: `systemctl restart redis`, managed Redis: 클라우드 콘솔에서 확인)")
                                .inline(false)
                                .build()
                ))
                .footer(DiscordEmbedFooter.builder()
                        .text(FOOTER_TEXT)
                        .build())
                .timestamp(Instant.now().toString())
                .build();

        return DiscordWebhookPayload.builder()
                .content("⚠️ **Redis 서버 장애 알림**")
                .embeds(List.of(embed))
                .build();
    }

    public static DiscordWebhookPayload buildRecoveryNotification(long downtimeDurationMs) {
        String timestamp = LocalDateTime.now(TIMEZONE).format(TIMESTAMP_FORMATTER);
        String downtimeText = DurationFormatter.formatDowntime(downtimeDurationMs);
        
        DiscordEmbed embed = DiscordEmbed.builder()
                .title("✅ Redis 서버 복구 완료")
                .color(3066993)
                .description("Redis 서버가 정상적으로 복구되었습니다.")
                .fields(List.of(
                        DiscordEmbedField.builder()
                                .name("⏰ 복구 시간")
                                .value(timestamp)
                                .inline(true)
                                .build(),
                        DiscordEmbedField.builder()
                                .name("⏱️ 장애 지속 시간")
                                .value(downtimeText)
                                .inline(true)
                                .build()
                ))
                .footer(DiscordEmbedFooter.builder()
                        .text(FOOTER_TEXT)
                        .build())
                .timestamp(Instant.now().toString())
                .build();

        return DiscordWebhookPayload.builder()
                .content("✅ **Redis 서버 복구 알림**")
                .embeds(List.of(embed))
                .build();
    }

    private static String formatErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "연결 실패";
        }
        if (errorMessage.length() > ERROR_MESSAGE_MAX_LENGTH) {
            return errorMessage.substring(0, ERROR_MESSAGE_MAX_LENGTH) + "...";
        }
        return errorMessage;
    }
}

