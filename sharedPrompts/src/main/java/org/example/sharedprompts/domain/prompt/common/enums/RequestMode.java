package org.example.sharedprompts.domain.prompt.common.enums;

/**
 * Request mode discriminator for unified prompt generation.
 * Used by semantic resolution to distinguish EXTRACTION (schema-driven) from SIMPLE/ADVANCED (category+intent).
 */
public enum RequestMode {

    SIMPLE,
    EXTRACTION,
    ADVANCED
}
