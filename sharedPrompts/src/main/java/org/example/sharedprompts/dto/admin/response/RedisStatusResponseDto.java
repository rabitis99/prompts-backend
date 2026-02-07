package org.example.sharedprompts.dto.admin.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Redis 상태 응답 DTO
 */
@Getter
@Builder
public class RedisStatusResponseDto {
    
    /**
     * Redis가 정상 상태인지 여부
     */
    private boolean healthy;
    
    /**
     * Redis에 연결되어 있는지 여부
     */
    private boolean connected;
    
    /**
     * 상태 메시지 (선택적)
     */
    private String message;
}

