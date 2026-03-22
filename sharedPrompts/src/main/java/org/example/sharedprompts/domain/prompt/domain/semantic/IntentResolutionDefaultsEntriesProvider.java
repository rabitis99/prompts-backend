package org.example.sharedprompts.domain.prompt.domain.semantic;

import java.util.List;

/**
 * Provides intent resolution-default entries (objective / output needs / response shape).
 * <p>
 * Data ownership lives in providers; {@link IntentDefinitionDataSource} merges entries.
 * Production registration: {@link IntentDefinitionProviderAssembly}.
 */
public interface IntentResolutionDefaultsEntriesProvider {

    List<IntentDefinitionDataSource.IntentResolutionDefaultEntry> entries();
}

