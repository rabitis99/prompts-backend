package org.example.sharedprompts.module.domain.production.model.storage;

public record StorageLifecyclePolicy(
        int transitionToGlacierDays,
        int transitionToDeepArchiveDays
) {}
