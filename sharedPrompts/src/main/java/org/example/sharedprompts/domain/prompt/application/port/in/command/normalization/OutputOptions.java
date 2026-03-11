package org.example.sharedprompts.domain.prompt.application.port.in.command.normalization;

import org.example.sharedprompts.domain.prompt.common.enums.engine.EngineMode;

/** 출력 설정 (schema, engine mode) */
public record OutputOptions(
        String jsonSchema,
        EngineMode engineMode
) {
    public OutputOptions {
        engineMode = engineMode != null ? engineMode : EngineMode.AUTO;
    }
}
