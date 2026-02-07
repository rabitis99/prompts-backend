package org.example.sharedprompts.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.redis.RedisHealthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisManagementService {

    private final RedisHealthService redisHealthService;
    
    @Value("${redis.restart.command:}")
    private String restartCommand;
    
    @Value("${redis.restart.enabled:false}")
    private boolean restartEnabled;
    
    @Value("${redis.restart.timeout-seconds:30}")
    private int restartTimeoutSeconds;

    public boolean checkRedisStatus() {
        return redisHealthService.getCachedHealthStatus();
    }

    @Async
    public CompletableFuture<Boolean> restartRedis() {
        log.info("Redis 재시작 시도 시작");
        
        if (!restartEnabled) {
            log.warn("Redis 재시작 기능이 비활성화되어 있습니다. redis.restart.enabled=true로 설정하세요.");
            return CompletableFuture.completedFuture(false);
        }
        
        if (restartCommand == null || restartCommand.trim().isEmpty()) {
            log.warn("Redis 재시작 명령어가 설정되지 않았습니다. redis.restart.command를 설정하세요.");
            return CompletableFuture.completedFuture(false);
        }
        
        try {
            boolean restartSuccess = executeRestartCommand();
            
            if (!restartSuccess) {
                log.error("Redis 재시작 명령어 실행 실패");
                return CompletableFuture.completedFuture(false);
            }
            
            Thread.sleep(3000);
            
            for (int i = 0; i < 5; i++) {
                boolean isHealthy = redisHealthService.forceHealthCheck();
                if (isHealthy) {
                    log.info("Redis 재시작 성공: 연결 확인 완료 (시도 횟수: {})", i + 1);
                    return CompletableFuture.completedFuture(true);
                }
                if (i < 4) {
                    Thread.sleep(2000);
                }
            }
            
            log.warn("Redis 재시작 후 연결 확인 실패");
            return CompletableFuture.completedFuture(false);
            
        } catch (Exception e) {
            log.error("Redis 재시작 시도 중 예외 발생", e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    private boolean executeRestartCommand() {
        try {
            log.debug("Redis 재시작 명령어 실행: {}", restartCommand);
            
            List<String> commandParts = parseCommand(restartCommand);
            if (commandParts.isEmpty()) {
                log.error("Redis 재시작 명령어 파싱 실패: 빈 명령어");
                return false;
            }
            
            ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
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
        return redisHealthService.forceHealthCheck();
    }

    public boolean performHealthCheck() {
        return testRedisConnection();
    }

    private List<String> parseCommand(String command) {
        List<String> parts = new ArrayList<>();
        if (command == null || command.trim().isEmpty()) {
            return parts;
        }
        
        // 공백으로 분리하되, 따옴표로 감싸진 부분은 보존
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts;
    }
}

