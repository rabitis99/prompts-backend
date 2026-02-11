package org.example.sharedprompts.module.domain.production.service.format;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class HtmlFormatConverter implements FormatConverter {

    @Override
    public byte[] convert(String content, String fileName) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getContentType() {
        return "text/html";
    }

    @Override
    public String getFileExtension() {
        return ".html";
    }

    @Override
    public boolean supports(String format) {
        return "html".equals(format) || "htm".equals(format);
    }
}
