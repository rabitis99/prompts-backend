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
     * 
     * @param content 파일에 작성할 내용
     * @param fileName 파일명 (확장자 제외)
     * @param format 파일 형식 (txt, md 등)
     * @return 생성된 파일의 경로
     * @throws IOException 파일 작성 실패 시
     */
    public String write(String content, String fileName, String format) throws IOException {
        // 출력 디렉토리 생성
        Path outputDir = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        
        // 파일명 생성 (타임스탬프 포함)
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String fullFileName = String.format("%s_%s.%s", fileName, timestamp, format);
        Path filePath = outputDir.resolve(fullFileName);
        
        // 파일 작성
        Files.writeString(filePath, content);
        
        return filePath.toString();
    }
}

