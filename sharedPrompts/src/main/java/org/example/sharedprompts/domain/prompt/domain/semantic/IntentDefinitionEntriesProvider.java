package org.example.sharedprompts.domain.prompt.domain.semantic;

import java.util.List;

/**
 * Provides intent definition entries.
 * <p>
 * Data ownership lives in providers; {@link IntentDefinitionDataSource} merges entries.
 * Production registration: {@link IntentDefinitionProviderAssembly}.
 */
public interface IntentDefinitionEntriesProvider {

    List<IntentDefinitionDataSource.IntentDefinitionEntry> entries();
}

