package org.example.sharedprompts.module.domain.production.service.format;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JsonFormatConverter implements FormatConverter {

    @Override
    public byte[] convert(String content, String fileName) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    @Override
    public String getFileExtension() {
        return ".json";
    }

    @Override
    public boolean supports(String format) {
        return "json".equals(format);
    }
}
