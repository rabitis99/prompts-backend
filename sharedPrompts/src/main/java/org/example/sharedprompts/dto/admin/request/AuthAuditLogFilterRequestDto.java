package org.example.sharedprompts.dto.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.auth.enums.AuthEventType;
import org.example.sharedprompts.domain.audit.auth.enums.AuthFailReason;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthAuditLogFilterRequestDto {

    @JsonProperty("event_type")
    private AuthEventType eventType;

    private Provider provider;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("fail_reason")
    private AuthFailReason failReason;

    @JsonProperty("start_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDate;

    @JsonProperty("end_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDate;

    /**
     * @ModelAttribute 바인딩을 위한 snake_case 파라미터 지원
     *
     * 예)
     * - ?event_type=LOGIN_SUCCESS
     * - ?provider=GOOGLE
     * - ?user_id=1
     * - ?fail_reason=INVALID_PASSWORD
     * - ?start_date=2024-01-01T00:00:00
     * - ?end_date=2024-01-31T23:59:59
     */
    /**
     * 서비스/테스트 코드에서 사용하기 좋은 정적 팩토리 메서드
     */
    public static AuthAuditLogFilterRequestDto of(
            AuthEventType eventType,
            Provider provider,
            Long userId,
            AuthFailReason failReason,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return AuthAuditLogFilterRequestDto.builder()
                .eventType(eventType)
                .provider(provider)
                .userId(userId)
                .failReason(failReason)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}


