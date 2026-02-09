package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

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
        return command.getCommandType();
    }
}
