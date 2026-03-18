package org.example.sharedprompts.domain.prompt.infrastructure.policy.repository;

import org.example.sharedprompts.domain.prompt.application.semantic.policy.VersionedPolicyRepository;
import org.example.sharedprompts.domain.prompt.domain.semantic.policy.validation.ValidatedPolicyBundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory versioned policy repository.
 * Version order is insertion order; getPrevious uses that order.
 */
public final class InMemoryVersionedPolicyRepository implements VersionedPolicyRepository {

    private final Map<String, ValidatedPolicyBundle> byVersion;
    private final List<String> versionOrder;

    public InMemoryVersionedPolicyRepository(Map<String, ValidatedPolicyBundle> byVersion, List<String> versionOrder) {
        this.byVersion = new LinkedHashMap<>(byVersion != null ? byVersion : Map.of());
        this.versionOrder = versionOrder != null ? new ArrayList<>(versionOrder) : new ArrayList<>(this.byVersion.keySet());
    }

    @Override
    public Optional<ValidatedPolicyBundle> getPolicy(String versionId) {
        return Optional.ofNullable(byVersion.get(versionId));
    }

    @Override
    public List<String> listVersions() {
        return List.copyOf(versionOrder.isEmpty() ? byVersion.keySet() : versionOrder);
    }

    @Override
    public Optional<ValidatedPolicyBundle> getLatest() {
        if (versionOrder.isEmpty()) return Optional.empty();
        String latest = versionOrder.get(versionOrder.size() - 1);
        return Optional.ofNullable(byVersion.get(latest));
    }

    @Override
    public Optional<ValidatedPolicyBundle> getPrevious(String versionId) {
        int idx = versionOrder.indexOf(versionId);
        if (idx <= 0) return Optional.empty();
        String prev = versionOrder.get(idx - 1);
        return Optional.ofNullable(byVersion.get(prev));
    }
}
