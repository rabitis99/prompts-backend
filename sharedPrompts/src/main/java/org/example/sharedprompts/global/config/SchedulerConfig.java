package org.example.sharedprompts.global.config;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory redisConnectionFactory) {
        return new RedisLockProvider(redisConnectionFactory);
    }

    /**
     * UTC 기준 Clock Bean
     * 
     * 스케줄러에서 타임존을 명시적으로 제어하기 위해 UTC Clock을 제공합니다.
     * 클라우드 환경에서 컨테이너가 UTC이지만 애플리케이션이 다른 타임존을 기대할 경우,
     * 저장된 createdAt과 일치하도록 UTC를 사용합니다.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}