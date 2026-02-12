package org.example.sharedprompts.module.domain.production.model.storage;

public record StorageLifecyclePolicy(
        int transitionToGlacierDays,
        int transitionToDeepArchiveDays
) {
    public StorageLifecyclePolicy {
        if (transitionToGlacierDays <= 0) {
            throw new IllegalArgumentException("transitionToGlacierDays must be positive");
        }
        if (transitionToDeepArchiveDays <= 0) {
            throw new IllegalArgumentException("transitionToDeepArchiveDays must be positive");
        }
        if (transitionToGlacierDays >= transitionToDeepArchiveDays) {
            throw new IllegalArgumentException(
                    "transitionToGlacierDays (" + transitionToGlacierDays +
                    ") must be less than transitionToDeepArchiveDays (" + transitionToDeepArchiveDays + ")");
        }
    }
}
