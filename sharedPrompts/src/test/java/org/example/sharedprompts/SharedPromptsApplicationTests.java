package org.example.sharedprompts;

import org.example.sharedprompts.domain.prompt.infrastructure.config.TestActionTypeMetadataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(TestActionTypeMetadataConfig.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class SharedPromptsApplicationTests {

	@Test
	void contextLoads() {
	}

}
