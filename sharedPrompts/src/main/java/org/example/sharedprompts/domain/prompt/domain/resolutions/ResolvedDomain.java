package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.TaskDomain;
public record ResolvedDomain(
        TaskDomain domain,
        boolean fallback,
        ResolutionSource source
) {
    /** Constructor for backward compatibility: source becomes FALLBACK or DEFAULT. */
    public static ResolvedDomain of(TaskDomain domain, boolean fallback) {
        return new ResolvedDomain(domain, fallback, fallback ? ResolutionSource.FALLBACK : ResolutionSource.DEFAULT);
    }

    public ResolvedDomain(TaskDomain domain, boolean fallback) {
        this(domain, fallback, fallback ? ResolutionSource.FALLBACK : ResolutionSource.DEFAULT);
    }
}
