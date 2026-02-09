package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextCommandDto implements CommandDto {
    @JsonProperty("file_name")
    private String fileName;
    private String format;
}

