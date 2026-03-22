package org.example.sharedprompts.domain.prompt.domain.semantic;

import java.util.List;

/**
 * Provides intent definition entries.
 * <p>
 * Data ownership lives in providers; {@link IntentDefinitionDataSource} only aggregates.
 */
public interface IntentDefinitionEntriesProvider {

    List<IntentDefinitionDataSource.IntentDefinitionEntry> entries();
}

