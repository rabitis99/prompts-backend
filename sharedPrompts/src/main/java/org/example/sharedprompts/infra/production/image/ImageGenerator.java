package org.example.sharedprompts.infra.production.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 이미지를 생성하는 Generator.
 * Production 계층에서 사용되며, 외부 시스템 통신 없이 로컬 파일 시스템에만 접근한다.
 * 
 * 현재는 기본 구현으로, 실제 이미지 생성은 추후 구현 예정.
 */
@Component
public class ImageGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private static final String OUTPUT_DIR = "output/image";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 이미지를 생성한다.
     * 
     * @param prompt 이미지 생성 프롬프트
     * @param width 이미지 너비
     * @param height 이미지 높이
     * @return 생성된 이미지 파일 경로
     * @throws IOException 이미지 생성 실패 시
     */
    public String generate(String prompt, int width, int height) throws IOException {
        log.info("이미지 생성 시도: prompt={}, width={}, height={}", prompt, width, height);
        
        // 출력 디렉토리 생성
        Path outputDir = Paths.get(OUTPUT_DIR);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }
        
        // 파일명 생성 (타임스탬프 포함)
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String fileName = String.format("image_%s.png", timestamp);
        Path filePath = outputDir.resolve(fileName);
        
        // TODO: 실제 이미지 생성 로직 구현
        // - DALL-E, Stable Diffusion 등 AI 이미지 생성 API 연동
        // - 또는 로컬 이미지 생성 라이브러리 사용
        
        // 현재는 플레이스홀더 파일 생성 (실제 이미지 생성 대신)
        String placeholderContent = String.format(
            "Placeholder image for prompt: %s\nDimensions: %dx%d\nGenerated at: %s",
            prompt, width, height, timestamp
        );
        Files.writeString(filePath, placeholderContent);
        
        log.info("이미지 생성 완료 (플레이스홀더): path={}", filePath);
        return filePath.toString();
    }
}

