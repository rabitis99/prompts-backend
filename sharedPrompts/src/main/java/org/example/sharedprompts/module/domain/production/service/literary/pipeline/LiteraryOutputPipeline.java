package org.example.sharedprompts.module.domain.production.service.literary.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.entity.job.JobEntity;
import org.example.sharedprompts.module.domain.production.service.format.FormatConverterRegistry;
import org.example.sharedprompts.module.domain.production.service.format.FormatConverterRegistry.ConvertedContent;
import org.example.sharedprompts.module.domain.production.service.format.markdown.MarkdownToHtmlConverter;
import org.example.sharedprompts.module.domain.production.service.job.process.execution.content.ContentStorageService;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.ContentRenderException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiteraryOutputPipeline {

    private static final String ORIGINAL_TXT = "original.txt";
    private static final String PREVIEW_HTML = "preview.html";
    private static final String FINAL_PDF = "final.pdf";

    private final MarkdownSanitizer markdownSanitizer;
    private final MarkdownToHtmlConverter markdownToHtmlConverter;
    private final FormatConverterRegistry formatConverterRegistry;
    private final ContentStorageService contentStorageService;

    public LiteraryPipelineResult run(String markdownContent, JobEntity job) {
        String sanitized = markdownSanitizer.sanitize(markdownContent);
        if (sanitized.isBlank()) {
            throw new ContentRenderException("Literary content is blank after sanitization");
        }

        String originalKey = storeOriginal(sanitized, job);
        String previewKey = storePreview(sanitized, job);
        String pdfKey = storePdf(sanitized, job);

        log.info("Literary pipeline completed - jobId: {}, original: {}, preview: {}, pdf: {}",
                job.getJobId(), originalKey, previewKey, pdfKey);

        return LiteraryPipelineResult.builder()
                .originalTxtKey(originalKey)
                .previewHtmlKey(previewKey)
                .finalPdfKey(pdfKey)
                .build();
    }

    private String storeOriginal(String sanitized, JobEntity job) {
        byte[] data = sanitized.getBytes(StandardCharsets.UTF_8);
        return contentStorageService.store(data, "text/plain", job, ORIGINAL_TXT);
    }

    private String storePreview(String sanitized, JobEntity job) {
        String html = markdownToHtmlConverter.convert(sanitized);
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        return contentStorageService.store(data, "text/html", job, PREVIEW_HTML);
    }

    private String storePdf(String sanitized, JobEntity job) {
        ConvertedContent converted = formatConverterRegistry.convert(sanitized, "pdf", "final");
        return contentStorageService.store(converted.data(), converted.contentType(), job, FINAL_PDF);
    }
}
