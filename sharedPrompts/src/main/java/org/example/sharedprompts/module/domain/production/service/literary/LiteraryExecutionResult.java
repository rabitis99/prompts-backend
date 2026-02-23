package org.example.sharedprompts.module.domain.production.service.literary;

/**
 * Result of a literary AI execution, including content and model observability data.
 */
public record LiteraryExecutionResult(
        String content,
        String modelName,
        String tokenUsage
) {}
