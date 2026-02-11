package org.example.sharedprompts.module.domain.production.service.job.process.execution.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.service.format.FormatConverterRegistry;
import org.example.sharedprompts.module.domain.production.service.format.FormatConverterRegistry.ConvertedContent;
import org.example.sharedprompts.module.domain.production.service.job.process.exception.ContentRenderException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentFormatter {

    private final FormatConverterRegistry formatConverterRegistry;

    public ConvertedContent format(String renderedContent, String outputFormat, String baseFileName) {
        log.info("Formatting content - outputFormat: {}, baseFileName: {}", outputFormat, baseFileName);

        try {
            return formatConverterRegistry.convert(renderedContent, outputFormat, baseFileName);
        } catch (Exception e) {
            log.error("Failed to format content - outputFormat: {}, baseFileName: {}", outputFormat, baseFileName, e);
            throw new ContentRenderException("Failed to format content: " + e.getMessage(), e);
        }
    }
}

