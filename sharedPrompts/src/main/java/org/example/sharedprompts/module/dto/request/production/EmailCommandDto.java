package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.Email;
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
public class EmailCommandDto implements CommandDto {
    @NotNull(message = "제목을 입력해주세요.")
    private String subject;
    
    @NotNull(message = "수신자를 입력해주세요.")
    @Email(message = "올바른 이메일 형식을 입력해주세요.")
    private String recipient;
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.EMAIL;
    }
}

