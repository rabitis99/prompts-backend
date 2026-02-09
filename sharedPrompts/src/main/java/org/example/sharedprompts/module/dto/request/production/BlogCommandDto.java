package org.example.sharedprompts.module.dto.request.production;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogCommandDto implements CommandDto {

    @NotBlank(message = "블로그 제목은 필수입니다.")
    private String title;

    @NotNull(message = "태그 목록을 입력해주세요.")
    private List<String> tags;

    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.BLOG;
    }
}
