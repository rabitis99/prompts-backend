package org.example.sharedprompts.module.domain.production.model.literary;

/**
 * Supported output format for literary production requests.
 * Used to constrain allowed values and avoid runtime errors from unsupported format strings.
 */
public enum LiteraryFormat {
    /** Standard literary output (txt, html, pdf pipeline). */
    STANDARD
}
