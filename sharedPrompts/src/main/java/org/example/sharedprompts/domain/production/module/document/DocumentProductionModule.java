package org.example.sharedprompts.domain.production.module.document;

import org.example.sharedprompts.domain.production.api.DefaultProductionResult;
import org.example.sharedprompts.domain.production.api.FileArtifact;
import org.example.sharedprompts.domain.production.api.ProductionArtifact;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.exception.CommandValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class DocumentProductionModule implements ProductionModule {
    
    private final String outputDir;
    
    // TODO: 아키텍처 문서(PRODUCTION_ARCHITECTURE.md 섹션 4.5)에 명시된 의존성 추가 필요:
    // - DocumentGenerator (Apache POI 사용, 문서 포맷팅)
    // - TextAiClient (텍스트 생성 AI, 문서 내용 생성용)
    // - DocumentStorage (파일 저장용)
    // 현재는 실제 문서 생성 로직 없이 파일 경로만 반환하는 미구현 상태입니다.
    
    public DocumentProductionModule(@Value("${document.output.dir:output/document}") String outputDir) {
        this.outputDir = outputDir;
    }
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.DOCUMENT;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof DocumentCommand documentCommand)) {
            throw new CommandValidationException(
                "Expected DocumentCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            // TODO: 실제 문서 생성 로직 구현 필요
            // 1. PromptResponseDto import 추가 필요
            // 2. PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            //    - promptResult null 체크 필요
            //    - promptResult.getContent()를 사용하여 문서 내용 생성
            // 3. TextAiClient를 사용하여 문서 내용 생성 (필요 시)
            // 4. DocumentGenerator를 사용하여 Apache POI로 문서 포맷팅 (DOCX/XLSX)
            // 5. DocumentStorage를 사용하여 파일 저장
            // 현재는 존재하지 않는 파일 경로만 반환합니다. (스텁 구현)
            
            // Path traversal 방지: fileName과 format에 경로 조작 시퀀스가 포함되지 않도록 검증
            String fileName = documentCommand.getFileName();
            String format = documentCommand.getFormat();
            if (fileName == null || format == null) {
                return DefaultProductionResult.failure("File name and format must not be null", startedAt, Instant.now());
            }
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") ||
                format.contains("..") || format.contains("/") || format.contains("\\")) {
                return DefaultProductionResult.failure("Invalid file name or format: path traversal detected", startedAt, Instant.now());
            }
            
            String filePath = String.format("%s/%s.%s", outputDir, fileName, format);
            
            Instant completedAt = Instant.now();
            
            ProductionArtifact artifact = new FileArtifact(filePath);
            return DefaultProductionResult.success(artifact, startedAt, completedAt);
            
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return DefaultProductionResult.failure(errorMsg, startedAt, Instant.now());
        }
    }
}

