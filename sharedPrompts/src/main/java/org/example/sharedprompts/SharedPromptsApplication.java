package org.example.sharedprompts;

import org.example.sharedprompts.global.config.ExcludeFromComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Legacy 라우팅 컴포넌트(RoutingRuleEngine, DomainFinalizer, OutputContractPlanner 등)는
 * {@link ExcludeFromComponentScan}을 통해 런타임 컴포넌트 스캔에서 제외된다.
 * 현재 파이프라인은 semantic resolution 기반으로 동작하며 legacy routing은 스캔되지 않는다.
 */
@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
@ComponentScan(
		basePackages = "org.example.sharedprompts",
		excludeFilters = {
				@ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ExcludeFromComponentScan.class)
		}
)
public class SharedPromptsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SharedPromptsApplication.class, args);
	}

}