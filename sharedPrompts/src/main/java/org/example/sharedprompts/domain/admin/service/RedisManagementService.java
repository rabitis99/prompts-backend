package org.example.sharedprompts.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.redis.RedisHealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisManagementService {

    private final RedisHealthService redisHealthService;
    private final StringRedisTemplate redisTemplate;
    
    @Value("${redis.restart.command:}")
    private String restartCommand;
    
    @Value("${redis.restart.enabled:false}")
    private boolean restartEnabled;
    
    @Value("${redis.restart.timeout-seconds:30}")
    private int restartTimeoutSeconds;

    public boolean checkRedisStatus() {
        return redisHealthService.getCachedHealthStatus();
    }

    public boolean restartRedis() {
        log.info("Redis 재시작 시도 시작");
        
        if (!restartEnabled) {
            log.warn("Redis 재시작 기능이 비활성화되어 있습니다. redis.restart.enabled=true로 설정하세요.");
            return false;
        }
        
        if (restartCommand == null || restartCommand.trim().isEmpty()) {
            log.warn("Redis 재시작 명령어가 설정되지 않았습니다. redis.restart.command를 설정하세요.");
            return false;
        }
        
        try {
            boolean restartSuccess = executeRestartCommand();
            
            if (!restartSuccess) {
                log.error("Redis 재시작 명령어 실행 실패");
                return false;
            }
            
            Thread.sleep(3000);
            
            for (int i = 0; i < 5; i++) {
                boolean isHealthy = redisHealthService.forceHealthCheck();
                if (isHealthy) {
                    log.info("Redis 재시작 성공: 연결 확인 완료 (시도 횟수: {})", i + 1);
                    return true;
                }
                if (i < 4) {
                    Thread.sleep(2000);
                }
            }
            
            log.warn("Redis 재시작 후 연결 확인 실패");
            return false;
            
        } catch (Exception e) {
            log.error("Redis 재시작 시도 중 예외 발생", e);
            return false;
        }
    }
    
    private boolean executeRestartCommand() {
        try {
            log.info("Redis 재시작 명령어 실행: {}", restartCommand);
            
            ProcessBuilder processBuilder;
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("cmd", "/c", restartCommand);
            } else {
                processBuilder = new ProcessBuilder("sh", "-c", restartCommand);
            }
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    log.debug("Redis 재시작 명령어 출력: {}", line);
                }
            }
            
            boolean finished = process.waitFor(restartTimeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                log.error("Redis 재시작 명령어 실행 타임아웃 ({}초)", restartTimeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Redis 재시작 명령어 실행 성공");
                return true;
            } else {
                log.error("Redis 재시작 명령어 실행 실패: exitCode={}, output={}", exitCode, output);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Redis 재시작 명령어 실행 중 예외 발생", e);
            return false;
        }
    }

    public boolean testRedisConnection() {
        try {
            String result = redisTemplate.execute((RedisCallback<String>) connection -> {
                return connection.ping();
            });
            
            boolean isConnected = "PONG".equals(result);
            
            if (isConnected) {
                log.info("Redis 연결 테스트 성공");
                redisHealthService.setHealthy(true);
            } else {
                log.warn("Redis 연결 테스트 실패: PING 응답이 PONG이 아님");
                redisHealthService.setHealthy(false);
            }
            
            return isConnected;
        } catch (Exception e) {
            log.error("Redis 연결 테스트 중 예외 발생", e);
            redisHealthService.setHealthy(false);
            return false;
        }
    }

    public boolean performHealthCheck() {
        return redisHealthService.forceHealthCheck();
    }
}

