package org.example.sharedprompts.module.domain.production.service.format;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * FormatConverter 레지스트리
 * 포맷 문자열에 맞는 Converter를 찾아 변환 수행
 */
@Component
@Slf4j
public class FormatConverterRegistry {

    private final List<FormatConverter> converters;

    public FormatConverterRegistry(List<FormatConverter> converters) {
        this.converters = converters;
        for (FormatConverter converter : converters) {
            log.info("Registered FormatConverter: {} (extension: {})",
                    converter.getClass().getSimpleName(), converter.getFileExtension());
        }
    }

    /**
     * 포맷에 맞는 Converter 조회
     */
    public Optional<FormatConverter> getConverter(String format) {
        if (format == null || format.isBlank()) {
            return Optional.empty();
        }
        String lowerFormat = format.toLowerCase();
        Optional<FormatConverter> converter = converters.stream()
                .filter(c -> c.supports(lowerFormat))
                .findFirst();
        
        if (converter.isEmpty()) {
            log.warn("No converter found for format: {} (lowercase: {}). Available converters: {}", 
                    format, lowerFormat, 
                    converters.stream()
                            .map(c -> c.getClass().getSimpleName())
                            .toList());
        } else {
            log.debug("Found converter: {} for format: {}", 
                    converter.get().getClass().getSimpleName(), format);
        }
        
        return converter;
    }

    /**
     * 콘텐츠를 지정된 포맷으로 변환
     * Converter가 없으면 원본 텍스트를 UTF-8 바이트로 반환
     */
    public ConvertedContent convert(String content, String format, String fileName) {
        Optional<FormatConverter> converter = getConverter(format);

        if (converter.isPresent()) {
            FormatConverter fc = converter.get();
            byte[] data = fc.convert(content, fileName);
            String outputFileName = appendExtension(fileName, fc.getFileExtension());
            return new ConvertedContent(data, fc.getContentType(), outputFileName);
        }

        // 지원하지 않는 포맷이면 텍스트 그대로 저장
        String outputFileName = appendExtension(fileName, ".txt");
        return new ConvertedContent(
                content.getBytes(StandardCharsets.UTF_8),
                "text/plain",
                outputFileName
        );
    }

    private String appendExtension(String fileName, String extension) {
        if (fileName == null || fileName.isBlank()) {
            return "output" + extension;
        }
        // 이미 확장자가 있으면 그대로
        if (fileName.contains(".")) {
            return fileName;
        }
        return fileName + extension;
    }

    /**
     * 변환 결과
     */
    public record ConvertedContent(
            byte[] data,
            String contentType,
            String fileName
    ) {}
}
