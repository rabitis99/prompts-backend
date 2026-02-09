package org.example.sharedprompts.infra.production.text;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 텍스트 파일을 작성하는 Writer.
 * Production 계층에서 사용되며, 외부 시스템 통신 없이 로컬 파일 시스템에만 접근한다.
 */
@Component
public class TextFileWriter {
    
    private static final String OUTPUT_DIR = "output/text";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 텍스트 파일을 작성한다.
     */
    public String write(String content, String fileName, String format) throws IOException {
        if (content == null || fileName == null || format == null) {
            throw new IllegalArgumentException("content, fileName, and format must not be null");
        }
        
        Path outputDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputDir);
        
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String fullFileName = String.format("%s_%s.%s", fileName, timestamp, format);
        Path filePath = outputDir.resolve(fullFileName);
        
        // 경로 탐색 방지: resolve 후 결과 경로가 outputDir 하위에 있는지 확인
        if (!filePath.normalize().startsWith(outputDir.normalize())) {
            throw new IOException("Invalid file path: path traversal detected");
        }
        
        Files.writeString(filePath, content);
        
        return filePath.toString();
    }
}

