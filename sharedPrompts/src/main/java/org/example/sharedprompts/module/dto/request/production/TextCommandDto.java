package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextCommandDto implements CommandDto {

    @NotBlank(message = "파일 이름은 필수입니다.")
    @JsonProperty("file_name")
    private String fileName;

    @NotBlank(message = "파일 포맷은 필수입니다. (ex: txt, md, html)")
    private String format;

    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.TEXT;
    }
}

