package org.example.sharedprompts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Legacy routing (UnifiedRoutingFacade, UnifiedRoutingPolicy, IntentDefaultsResolver,
 * RoutingRuleEngine, DomainFinalizer, OutputContractPlanner) lives in package
 * {@code ...orchestration.legacy} and is excluded from component scanning so it is not
 * part of the active runtime. The active pipeline uses SemanticResolutionService only.
 */
@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
@ComponentScan(
        basePackages = "org.example.sharedprompts",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "org\\.example\\.sharedprompts\\.domain\\.prompt\\.application\\.service\\.orchestration\\.legacy\\..*")
        }
)
public class SharedPromptsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SharedPromptsApplication.class, args);
	}

}
