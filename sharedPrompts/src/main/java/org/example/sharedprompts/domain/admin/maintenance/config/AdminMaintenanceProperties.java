package org.example.sharedprompts.domain.admin.maintenance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "admin.maintenance")
public class AdminMaintenanceProperties {
    
    /**
     * Like count 재빌드 작업 설정
     */
    private Rebuild rebuild = new Rebuild();
    
    @Getter
    @Setter
    public static class Rebuild {
        /**
         * 페이지 크기 (한 번에 처리할 데이터 건수)
         */
        private int pageSize = 1_000;
        
        /**
         * ShedLock 설정
         */
        private Lock lock = new Lock();
        
        /**
         * 재시도 설정
         */
        private Retry retry = new Retry();
        
        /**
         * 배치 처리 설정
         */
        private Batch batch = new Batch();
        
        /**
         * 글로벌 상태 관리 사용 여부
         */
        private boolean useGlobalStatus = false;
    }
    
    @Getter
    @Setter
    public static class Retry {
        /**
         * 최대 재시도 횟수
         */
        private int maxRetries = 3;
        
        /**
         * 재시도 간 지연 시간 (밀리초)
         */
        private long retryDelayMs = 1000;
        
        /**
         * 재시도 활성화 여부
         */
        private boolean enabled = false;
    }
    
    @Getter
    @Setter
    public static class Batch {
        /**
         * 배치 크기 (한 번에 Redis에 저장할 건수)
         */
        private int size = 100;
        
        /**
         * 배치 처리 활성화 여부
         */
        private boolean enabled = true;
    }
    
    @Getter
    @Setter
    public static class Lock {
        /**
         * Lock 이름
         */
        private String name = "AdminMaintenanceService_rebuildLikeCounts";
        
        /**
         * 최대 Lock 유지 시간 (작업이 이 시간보다 오래 걸리면 자동 해제)
         */
        private String lockAtMostFor = "1h";
        
        /**
         * 최소 Lock 유지 시간 (작업 완료 후 최소 이 시간 동안 Lock 유지)
         */
        private String lockAtLeastFor = "30m";
    }
}

