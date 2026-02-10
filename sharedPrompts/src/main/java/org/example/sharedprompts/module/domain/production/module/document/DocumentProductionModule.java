package org.example.sharedprompts.module.domain.production.module.document;

import org.example.sharedprompts.module.domain.production.api.artifact.FileArtifact;
import org.example.sharedprompts.module.domain.production.api.artifact.ProductionArtifact;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.api.model.DefaultModuleProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ModuleProductionResult;
import org.example.sharedprompts.module.domain.production.api.model.ProductionContext;
import org.example.sharedprompts.module.domain.production.api.module.ProductionModule;
import org.example.sharedprompts.module.domain.production.exception.CommandValidationException;
import org.example.sharedprompts.module.domain.production.exception.ProductionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    public ModuleProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof DocumentCommand documentCommand)) {
            throw new CommandValidationException(
                "Expected DocumentCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        String fileName = documentCommand.getFileName();
        String format = documentCommand.getFormat();
        if (fileName == null || format == null) {
            throw new ProductionException("File name and format must not be null");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\") ||
            format.contains("..") || format.contains("/") || format.contains("\\")) {
            throw new ProductionException("Invalid file name or format: path traversal detected");
        }
        
        Path baseDirPath = Paths.get(outputDir).normalize().toAbsolutePath();
        Path filePathObj = baseDirPath.resolve(String.format("%s.%s", fileName, format)).normalize();
        if (!filePathObj.startsWith(baseDirPath)) {
            throw new ProductionException("Invalid output directory: path traversal detected");
        }
        
        String filePath = filePathObj.toString();
        
        Instant completedAt = Instant.now();
        
        ProductionArtifact artifact = new FileArtifact(filePath);
        return DefaultModuleProductionResult.success(artifact, startedAt, completedAt);
    }
}

