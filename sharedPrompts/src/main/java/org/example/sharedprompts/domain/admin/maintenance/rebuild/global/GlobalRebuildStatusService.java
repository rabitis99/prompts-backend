package org.example.sharedprompts.domain.admin.maintenance.rebuild.global;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.enums.MaintenanceJobStatus;
import org.example.sharedprompts.domain.admin.maintenance.service.RebuildStatusService;
import org.example.sharedprompts.dto.admin.response.RebuildLikeCountsStatusResponseDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Redis 기반 글로벌 상태 관리 서비스
 * 멀티 인스턴스 환경에서 상태를 공유합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalRebuildStatusService implements RebuildStatusService {
    
    private static final String REDIS_KEY = "admin:maintenance:rebuild:status";
    private static final Duration TTL = Duration.ofHours(2); // 상태 정보 TTL
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void saveStatus(MaintenanceJobStatus status, LocalDateTime startedAt, 
                          LocalDateTime finishedAt, String errorMessage) {
        try {
            StatusData data = new StatusData(status, startedAt, finishedAt, errorMessage);
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(REDIS_KEY, json, TTL);
            log.debug("글로벌 상태 저장 완료: {}", status);
        } catch (JsonProcessingException e) {
            log.error("상태 저장 실패", e);
            // Redis 실패 시 로컬 상태로 fallback하지 않고 예외를 던짐
            throw new RuntimeException("상태 저장 실패", e);
        }
    }
    
    @Override
    public RebuildLikeCountsStatusResponseDto getStatus() {
        String json = redisTemplate.opsForValue().get(REDIS_KEY);
        if (json == null) {
            return RebuildLikeCountsStatusResponseDto.builder()
                    .status(MaintenanceJobStatus.IDLE)
                    .build();
        }
        
        try {
            StatusData data = objectMapper.readValue(json, StatusData.class);
            return RebuildLikeCountsStatusResponseDto.builder()
                    .status(data.getStatus())
                    .startedAt(data.getStartedAt())
                    .finishedAt(data.getFinishedAt())
                    .errorMessage(data.getErrorMessage())
                    .build();
        } catch (JsonProcessingException e) {
            log.error("상태 조회 실패", e);
            return RebuildLikeCountsStatusResponseDto.builder()
                    .status(MaintenanceJobStatus.IDLE)
                    .build();
        }
    }
    
    @Override
    public boolean isRunning() {
        RebuildLikeCountsStatusResponseDto status = getStatus();
        return status.getStatus() == MaintenanceJobStatus.RUNNING;
    }
    
    @Override
    public void reset() {
        redisTemplate.delete(REDIS_KEY);
        log.debug("글로벌 상태 초기화 완료");
    }

}

