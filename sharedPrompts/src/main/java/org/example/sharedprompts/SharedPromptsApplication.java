package org.example.sharedprompts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan  // 전체 패키지 스캔 (환경별 설정 적용을 위해)
@SpringBootApplication
public class SharedPromptsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SharedPromptsApplication.class, args);
	}

}
