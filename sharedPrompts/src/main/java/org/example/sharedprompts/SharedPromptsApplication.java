package org.example.sharedprompts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SharedPromptsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SharedPromptsApplication.class, args);
	}

}
