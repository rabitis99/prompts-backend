package org.example.sharedprompts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing
@EnableAsync
@ConfigurationPropertiesScan("org.example.sharedprompts.global.redis")
@SpringBootApplication
public class SharedPromptsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SharedPromptsApplication.class, args);
	}

}
