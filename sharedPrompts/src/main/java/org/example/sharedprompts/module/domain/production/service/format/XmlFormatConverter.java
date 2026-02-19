package org.example.sharedprompts.module.domain.production.service.format;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class XmlFormatConverter implements FormatConverter {

    @Override
    public byte[] convert(String content, String fileName) {
        if (content == null) {
            throw new FormatConversionException("Content must not be null for file: " + fileName);
        }
        
        log.debug("Converting content to XML - fileName: {}, contentLength: {}", fileName, content.length());
        
        // XML 선언부가 없으면 추가
        String xmlContent = content.trim();
        if (!xmlContent.startsWith("<?xml")) {
            xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlContent;
        }
        
        byte[] result = xmlContent.getBytes(StandardCharsets.UTF_8);
        log.debug("XML conversion completed - fileName: {}, xmlSize: {} bytes", fileName, result.length);
        return result;
    }

    @Override
    public String getContentType() {
        return "application/xml";
    }

    @Override
    public String getFileExtension() {
        return ".xml";
    }

    @Override
    public boolean supports(String format) {
        boolean supports = "xml".equals(format);
        log.debug("XmlFormatConverter.supports({}) = {}", format, supports);
        return supports;
    }
}

