package org.example.sharedprompts.module.dto.request.production;

import org.example.sharedprompts.module.domain.production.api.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.module.blog.BlogCommand;
import org.example.sharedprompts.module.domain.production.module.email.EmailCommand;
import org.example.sharedprompts.module.domain.production.module.image.ImageCommand;
import org.example.sharedprompts.module.domain.production.module.text.TextCommand;
import org.example.sharedprompts.module.utils.ValidationUtils;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * CommandDto를 ProductionCommand로 변환하는 매퍼 클래스
 */
@Component
public class ProductionCommandMapper {

    /**
     * CommandDto를 ProductionCommand로 변환한다.
     */
    public ProductionCommand toProductionCommand(CommandDto commandDto) {
        if (commandDto == null) {
            throw new IllegalArgumentException("CommandDto는 null일 수 없습니다.");
        }

        String commandId = UUID.randomUUID().toString();
        ProductionCommandType commandType = commandDto.getCommandType();

        if (commandType == null) {
            throw new IllegalArgumentException(
                    "Command 타입이 지정되지 않았습니다: " + commandDto.getClass().getSimpleName()
            );
        }

        return switch (commandType) {
            case BLOG -> {
                BlogCommandDto cmd = (BlogCommandDto) commandDto;
                yield new BlogCommand(commandId, cmd.getTitle(), cmd.getTags());
            }
            case EMAIL -> {
                EmailCommandDto cmd = (EmailCommandDto) commandDto;
                yield new EmailCommand(commandId, cmd.getSubject(), cmd.getRecipient());
            }
            case TEXT -> {
                TextCommandDto cmd = (TextCommandDto) commandDto;
                yield new TextCommand(commandId, cmd.getFileName(), cmd.getFormat());
            }
            case IMAGE -> {
                ImageCommandDto cmd = (ImageCommandDto) commandDto;
                yield new ImageCommand(
                        commandId,
                        cmd.getPrompt(),
                        ValidationUtils.requireNonNull(cmd.getWidth(), "width는 필수 값입니다."),
                        ValidationUtils.requireNonNull(cmd.getHeight(), "height는 필수 값입니다.")
                );
            }
            case DOCUMENT ->
                    throw new UnsupportedOperationException("DOCUMENT 타입은 아직 지원하지 않습니다.");
        };
    }
}
