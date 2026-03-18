package org.example.sharedprompts.domain.prompt.infrastructure.policy.repository;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyDocumentLoader;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.PolicyLoadValidateResult;
import org.example.sharedprompts.domain.prompt.application.semantic.policy.VersionedPolicyRepository;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.version.PolicyVersion;
import org.example.sharedprompts.domain.prompt.infrastructure.policy.PolicyDocumentPipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Versioned policy repository that loads policy bundles from classpath.
 * Each version is defined by a version id and a map of policyType → classpath resource.
 */
public final class ClasspathPolicyRepository implements VersionedPolicyRepository {

    private final PolicyDocumentLoader loader;
    private final PolicyDocumentPipeline pipeline;
    private final List<VersionEntry> versionEntries;
    private final Map<String, ValidatedPolicyBundle> cache = new LinkedHashMap<>();
    private final List<String> versionOrder = new ArrayList<>();

    public ClasspathPolicyRepository(
            PolicyDocumentLoader loader,
            PolicyDocumentPipeline pipeline,
            List<VersionEntry> versionEntries
    ) {
        this.loader = loader;
        this.pipeline = pipeline;
        this.versionEntries = versionEntries != null ? List.copyOf(versionEntries) : List.of();
        loadAll();
    }

    private void loadAll() {
        for (VersionEntry e : versionEntries) {
            Map<String, Map<String, Object>> rawByType = new LinkedHashMap<>();
            for (Map.Entry<String, String> re : e.resourcesByPolicyType().entrySet()) {
                Map<String, Object> raw = loader.loadFromClasspath(re.getValue());
                if (!raw.isEmpty()) {
                    rawByType.put(re.getKey(), raw);
                }
            }
            PolicyVersion pv = PolicyVersion.of(e.versionId(), "classpath", "classpath");
            PolicyLoadValidateResult result = pipeline.loadValidateBundle(pv, rawByType);
            if (result.isSuccess() && result.validatedBundle().isPresent()) {
                cache.put(e.versionId(), result.validatedBundle().get());
                versionOrder.add(e.versionId());
            }
        }
    }

    @Override
    public Optional<ValidatedPolicyBundle> getPolicy(String versionId) {
        return Optional.ofNullable(cache.get(versionId));
    }

    @Override
    public List<String> listVersions() {
        return List.copyOf(versionOrder);
    }

    @Override
    public Optional<ValidatedPolicyBundle> getLatest() {
        if (versionOrder.isEmpty()) return Optional.empty();
        return Optional.ofNullable(cache.get(versionOrder.get(versionOrder.size() - 1)));
    }

    @Override
    public Optional<ValidatedPolicyBundle> getPrevious(String versionId) {
        int idx = versionOrder.indexOf(versionId);
        if (idx <= 0) return Optional.empty();
        return Optional.ofNullable(cache.get(versionOrder.get(idx - 1)));
    }

    /**
     * Defines one version's sources: policyType → classpath resource path.
     */
    public record VersionEntry(String versionId, Map<String, String> resourcesByPolicyType) {
        public VersionEntry {
            if (versionId == null || versionId.isBlank()) {
                throw new IllegalArgumentException("versionId is required");
            }
            resourcesByPolicyType = resourcesByPolicyType != null ? Map.copyOf(resourcesByPolicyType) : Map.of();
        }
    }
}
