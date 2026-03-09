package org.example.sharedprompts.domain.prompt.application.port.in.command.normalization;

import org.example.sharedprompts.domain.prompt.common.enums.EngineMode;

/**
 * Output configuration only: schema and engine mode.
 * Does not define semantic meaning; output contract is applied after semantic axes are fixed.
 */
public record OutputOptions(
        String jsonSchema,
        EngineMode engineMode
) {
    public OutputOptions {
        engineMode = engineMode != null ? engineMode : EngineMode.AUTO;
    }
}
