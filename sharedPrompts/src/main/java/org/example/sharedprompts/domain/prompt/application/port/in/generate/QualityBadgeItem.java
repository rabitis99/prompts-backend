package org.example.sharedprompts.domain.prompt.application.port.in.generate;

/**
 * Port-level view of a quality badge for API boundary.
 * Prevents adapter (api) from depending on domain.value.quality.QualityBadge.
 */
public record QualityBadgeItem(String key, String displayName) {
    public QualityBadgeItem {
        key = key != null ? key : "";
        displayName = displayName != null ? displayName : "";
    }
}
