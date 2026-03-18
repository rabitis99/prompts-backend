package org.example.sharedprompts.domain.prompt.infrastructure.config;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyBinder;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.VersionedPolicyRepository;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.registry.PolicySourceRegistry;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.PolicyDocumentPipeline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires policy runtime from pipeline: bootstrap loads/validates/binds default document
 * (or fallback empty bundle), then populates repository and registry with same version.
 */
@Configuration
public class PolicyBootstrapConfig {

    @Bean
    public PolicyRuntimeBootstrap policyRuntimeBootstrap(
            PolicyVersion defaultPolicyVersion,
            PolicyBinder policyBinder,
            PolicyDocumentPipeline policyDocumentPipeline,
            PolicyDocumentLoader policyDocumentLoader
    ) {
        return new PolicyRuntimeBootstrap(
                defaultPolicyVersion,
                policyBinder,
                policyDocumentPipeline,
                policyDocumentLoader
        );
    }

    @Bean
    public VersionedPolicyRepository versionedPolicyRepository(PolicyRuntimeBootstrap bootstrap) {
        return bootstrap.getRepository();
    }

    @Bean
    public PolicySourceRegistry policySourceRegistry(PolicyRuntimeBootstrap bootstrap) {
        return bootstrap.getRegistry();
    }
}
