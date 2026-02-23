package org.example.sharedprompts.module.domain.production.service.literary.pipeline;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LiteraryPipelineResult {

    private final String originalTxtKey;
    private final String previewHtmlKey;
    private final String finalPdfKey;
}
