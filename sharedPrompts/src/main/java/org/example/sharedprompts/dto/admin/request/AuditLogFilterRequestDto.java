package org.example.sharedprompts.dto.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.enums.AuditEntityType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class AuditLogFilterRequestDto {

    @JsonProperty("actor_id")
    private Long actorId;

    @JsonProperty("entity_type")
    private AuditEntityType entityType;

    private AuditAction action;

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
     * - ?actor_id=1
     * - ?entity_type=PROMPT
     * - ?start_date=2024-01-01T00:00:00
     */

    /**
     * 서비스/테스트 코드에서 사용하기 좋은 정적 팩토리 메서드
     */
    public static AuditLogFilterRequestDto of(
            Long actorId,
            AuditEntityType entityType,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return AuditLogFilterRequestDto.builder()
                .actorId(actorId)
                .entityType(entityType)
                .action(action)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }
}


