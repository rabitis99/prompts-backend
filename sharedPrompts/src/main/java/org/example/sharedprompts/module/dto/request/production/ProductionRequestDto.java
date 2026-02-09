package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @NotNull(message = "Command를 입력해주세요.")
    @Valid
    private CommandDto command;
    @JsonProperty("user_input")
    private UserInputDto userInput;

    public ProductionCommandType getCommandType() {
        if (command == null) {
            return null;
        }
        if (command instanceof BlogCommandDto) {
            return ProductionCommandType.BLOG;
        } else if (command instanceof EmailCommandDto) {
            return ProductionCommandType.EMAIL;
        } else if (command instanceof TextCommandDto) {
            return ProductionCommandType.TEXT;
        } else if (command instanceof ImageCommandDto) {
            return ProductionCommandType.IMAGE;
        }
        return null;
    }

    public ProductionCommand toProductionCommand() {
        String commandId = UUID.randomUUID().toString();
        ProductionCommandType commandType = getCommandType();

        return switch (commandType) {
            case BLOG -> {
                BlogCommandDto cmd = (BlogCommandDto) command;
                yield new BlogCommand(commandId, cmd.getTitle(), cmd.getTags());
            }
            case EMAIL -> {
                EmailCommandDto cmd = (EmailCommandDto) command;
                yield new EmailCommand(commandId, cmd.getSubject(), cmd.getRecipient());
            }
            case TEXT -> {
                TextCommandDto cmd = (TextCommandDto) command;
                yield new TextCommand(commandId, cmd.getFileName(), cmd.getFormat());
            }
            case IMAGE -> {
                ImageCommandDto cmd = (ImageCommandDto) command;
                yield new ImageCommand(commandId, cmd.getPrompt(), cmd.getWidth(), cmd.getHeight());
            }
            case DOCUMENT -> throw new UnsupportedOperationException("DOCUMENT 타입은 아직 지원하지 않습니다.");
        };
    }
}
