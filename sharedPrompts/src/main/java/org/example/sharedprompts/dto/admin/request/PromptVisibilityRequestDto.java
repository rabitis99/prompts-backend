package org.example.sharedprompts.dto.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptVisibilityRequestDto {

    @NotNull(message = "공개 여부는 필수입니다.")
    @JsonProperty("is_public")
    private Boolean isPublic;
}

