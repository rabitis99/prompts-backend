package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.module.blog.BlogCommand;
import org.example.sharedprompts.module.domain.production.module.email.EmailCommand;
import org.example.sharedprompts.module.domain.production.module.image.ImageCommand;
import org.example.sharedprompts.module.domain.production.module.text.TextCommand;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionRequestDto {
    
    @NotNull(message = "Command 타입을 입력해주세요.")
    private ProductionCommandType commandType;
    
    @NotNull(message = "Command를 입력해주세요.")
    @Valid
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "commandType", visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BlogCommandDto.class, name = "BLOG"),
        @JsonSubTypes.Type(value = EmailCommandDto.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = TextCommandDto.class, name = "TEXT"),
        @JsonSubTypes.Type(value = ImageCommandDto.class, name = "IMAGE")
    })
    private CommandDto command;
    
    private UserInputDto userInput;
    
    public ProductionCommand toProductionCommand() {
        String commandId = UUID.randomUUID().toString();
        
        return switch (commandType) {
            case BLOG -> {
                BlogCommandDto blogCmd = (BlogCommandDto) command;
                yield new BlogCommand(commandId, blogCmd.getTitle(), blogCmd.getTags());
            }
            case EMAIL -> {
                EmailCommandDto emailCmd = (EmailCommandDto) command;
                yield new EmailCommand(commandId, emailCmd.getSubject(), emailCmd.getRecipient());
            }
            case TEXT -> {
                TextCommandDto textCmd = (TextCommandDto) command;
                yield new TextCommand(commandId, textCmd.getFileName(), textCmd.getFormat());
            }
            case IMAGE -> {
                ImageCommandDto imageCmd = (ImageCommandDto) command;
                yield new ImageCommand(commandId, imageCmd.getPrompt(), imageCmd.getWidth(), imageCmd.getHeight());
            }
            case DOCUMENT -> throw new UnsupportedOperationException("DOCUMENT 타입은 아직 지원하지 않습니다.");
        };
    }
}

