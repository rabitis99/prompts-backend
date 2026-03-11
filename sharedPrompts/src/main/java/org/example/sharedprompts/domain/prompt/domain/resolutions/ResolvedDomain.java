package org.example.sharedprompts.domain.prompt.domain.resolutions;

import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;
public record ResolvedDomain(
        TaskDomain domain,
        boolean fallback,
        ResolutionSource source
) {
    public ResolvedDomain {
        if (source == null) {
            source = fallback ? ResolutionSource.FALLBACK : ResolutionSource.DEFAULT;
        }
        if (fallback != (source == ResolutionSource.FALLBACK)) {
            throw new IllegalArgumentException("fallback/source 조합이 일치해야 합니다.");
        }
    }

    /** Constructor for backward compatibility: source becomes FALLBACK or DEFAULT. */
    public static ResolvedDomain of(TaskDomain domain, boolean fallback) {
        return new ResolvedDomain(domain, fallback, fallback ? ResolutionSource.FALLBACK : ResolutionSource.DEFAULT);
    }

    public ResolvedDomain(TaskDomain domain, boolean fallback) {
        this(domain, fallback, fallback ? ResolutionSource.FALLBACK : ResolutionSource.DEFAULT);
    }
}
