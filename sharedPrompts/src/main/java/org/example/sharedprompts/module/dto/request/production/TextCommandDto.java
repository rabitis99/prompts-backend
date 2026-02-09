package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("file_name")
    private String fileName;
    private String format;
    
    @Override
    public ProductionCommandType getCommandType() {
        return ProductionCommandType.TEXT;
    }
}

