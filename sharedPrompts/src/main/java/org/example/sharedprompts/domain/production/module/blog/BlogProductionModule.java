package org.example.sharedprompts.domain.production.module.blog;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.production.api.ProductionCommand;
import org.example.sharedprompts.domain.production.api.ProductionCommandType;
import org.example.sharedprompts.domain.production.api.ProductionContext;
import org.example.sharedprompts.domain.production.api.ProductionModule;
import org.example.sharedprompts.domain.production.api.ProductionResult;
import org.example.sharedprompts.domain.production.exception.ProductionException;
import org.example.sharedprompts.domain.production.api.TextArtifact;
import org.example.sharedprompts.domain.production.exception.CommandValidationException;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BlogProductionModule implements ProductionModule {
    
    // TODO: infra 계층의 BlogComposer 주입 필요
    // private final BlogComposer blogComposer;
    
    @Override
    public ProductionCommandType getSupportedCommandType() {
        return ProductionCommandType.BLOG;
    }
    
    @Override
    public ProductionResult produce(
            ProductionCommand command, 
            ProductionContext context
    ) {
        if (!(command instanceof BlogCommand blogCommand)) {
            throw new CommandValidationException(
                "Expected BlogCommand, but got: " + command.getClass()
            );
        }
        
        Instant startedAt = Instant.now();
        
        try {
            // TODO: PromptResponseDto를 context에서 가져오는 로직 구현
            // PromptResponseDto promptResult = context.getAttribute("promptResult", PromptResponseDto.class);
            
            // TODO: blogComposer.compose() 호출하여 블로그 콘텐츠 생성
            // String blogContent = blogComposer.compose(
            //     blogCommand.getTitle(),
            //     promptResult.getContent(),
            //     blogCommand.getTags()
            // );
            
            Instant completedAt = Instant.now();
            
            // TODO: 실제 블로그 콘텐츠로 TextArtifact 생성
            // ProductionArtifact artifact = new TextArtifact(blogContent);
            // return BlogResult.success(artifact, startedAt, completedAt);
            
            throw new UnsupportedOperationException("TODO: Implement blog content composition logic");
            
        } catch (Exception e) {
            return BlogResult.failure(e.getMessage(), startedAt, Instant.now());
        }
    }
}

