package org.example.sharedprompts.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.admin.service.RedisManagementService;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.admin.response.RedisStatusResponseDto;
import org.example.sharedprompts.global.annotation.AdminOnly;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@AdminOnly
@RestController
@RequestMapping("/admin/redis")
@RequiredArgsConstructor
public class AdminRedisController {

    private final RedisManagementService redisManagementService;

    @GetMapping("/status")
    public ResponseEntity<CustomResponse<RedisStatusResponseDto>> getRedisStatus() {
        boolean isHealthy = redisManagementService.checkRedisStatus();
        
        RedisStatusResponseDto response = RedisStatusResponseDto.builder()
                .healthy(isHealthy)
                .connected(isHealthy)
                .build();
        
        return CustomResponseHelper.ok(response);
    }

    @PostMapping("/restart")
    public ResponseEntity<CustomResponse<RedisStatusResponseDto>> restartRedis(
            @CurrentUser AuthUser authUser
    ) {
        log.info("관리자 Redis 재시작 요청: userId={}", authUser.getId());
        
        boolean success = redisManagementService.restartRedis();
        
        RedisStatusResponseDto response = RedisStatusResponseDto.builder()
                .healthy(success)
                .connected(success)
                .message(success ? "Redis 재시작 시도가 완료되었습니다." : "Redis 재시작 시도에 실패했습니다.")
                .build();
        
        return CustomResponseHelper.ok(response);
    }

    @PostMapping("/test-connection")
    public ResponseEntity<CustomResponse<RedisStatusResponseDto>> testConnection() {
        boolean connected = redisManagementService.testRedisConnection();
        
        RedisStatusResponseDto response = RedisStatusResponseDto.builder()
                .healthy(connected)
                .connected(connected)
                .message(connected ? "Redis 연결이 정상입니다." : "Redis 연결에 실패했습니다.")
                .build();
        
        return CustomResponseHelper.ok(response);
    }

    @PostMapping("/health-check")
    public ResponseEntity<CustomResponse<RedisStatusResponseDto>> performHealthCheck() {
        boolean isHealthy = redisManagementService.performHealthCheck();
        
        RedisStatusResponseDto response = RedisStatusResponseDto.builder()
                .healthy(isHealthy)
                .connected(isHealthy)
                .message(isHealthy ? "Redis Health Check 통과" : "Redis Health Check 실패")
                .build();
        
        return CustomResponseHelper.ok(response);
    }
}

