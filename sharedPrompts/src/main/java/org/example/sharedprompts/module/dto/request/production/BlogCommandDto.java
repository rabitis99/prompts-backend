package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class BlogCommandDto implements CommandDto {
    private String title;
    @NotNull(message = "태그 목록을 입력해주세요.")
    private List<String> tags;
}

