package org.example.sharedprompts.domain.production.module.text;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.exception.ProductionException;
import org.example.sharedprompts.domain.production.exception.CommandValidationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TextProductionModule implements ProductionModule {
    
    // TODO: infra 계층의 TextFileWriter 주입 필요
    // private final TextFileWriter textFileWriter;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.TEXT;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof TextCommand textCommand)) {
            throw new CommandValidationException(
                "Expected TextCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            // TODO: PromptResponseDto를 context에서 가져오는 로직 구현
            // PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            // TODO: textFileWriter.write() 호출하여 파일 생성
            // String filePath = textFileWriter.write(
            //     promptResult.getContent(),
            //     textCommand.getFileName(),
            //     textCommand.getFormat()
            // );
            
            Instant completedAt = Instant.now();
            
            // TODO: 실제 파일 경로로 FileArtifact 생성
            // ProductionArtifact artifact = new FileArtifact(filePath);
            // return TextResult.success(artifact, startedAt, completedAt);
            
            throw new UnsupportedOperationException("TODO: Implement text file writing logic");
            
        } catch (Exception e) {
            return TextResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}

