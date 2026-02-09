package org.example.sharedprompts.infra.production.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 이미지를 생성하는 Generator.
 */
@Component
public class ImageGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(ImageGenerator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    private final String outputDir;
    
    public ImageGenerator(@Value("${image.output.dir:output/image}") String outputDir) {
        this.outputDir = outputDir;
    }
    
    /**
     * 이미지를 생성한다.
     */
    public String generate(String prompt, int width, int height) throws IOException {
        log.info("이미지 생성 시도: promptLength={}, width={}, height={}", 
                prompt != null ? prompt.length() : 0, width, height);
        
        // 출력 디렉토리 생성
        Path outputDirPath = Paths.get(outputDir);
        Files.createDirectories(outputDirPath);
        
        // 파일명 생성 (타임스탬프 + UUID로 고유성 보장)
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String fileName = String.format("image_%s_%s.png", timestamp, uniqueId);
        Path filePath = outputDirPath.resolve(fileName);
        
        // TODO: 실제 이미지 생성 로직 구현
        // - DALL-E, Stable Diffusion 등 AI 이미지 생성 API 연동
        // - 또는 로컬 이미지 생성 라이브러리 사용
        
        // 현재는 플레이스홀더 파일 생성 (실제 이미지 생성 대신)
        // 보안: 프롬프트 전문 대신 길이만 기록하여 민감 정보가 디스크에 잔류하지 않도록 함
        int promptLength = prompt != null ? prompt.length() : 0;
        String placeholderContent = String.format(
            "Placeholder image for prompt (length: %d)\nDimensions: %dx%d\nGenerated at: %s",
            promptLength, width, height, timestamp
        );
        Files.writeString(filePath, placeholderContent);
        
        log.info("이미지 생성 완료 (플레이스홀더): path={}", filePath);
        return filePath.toString();
    }
}

