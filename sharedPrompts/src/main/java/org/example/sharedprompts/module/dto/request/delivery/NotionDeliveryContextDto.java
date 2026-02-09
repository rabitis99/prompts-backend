package org.example.sharedprompts.module.dto.request.delivery;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class NotionDeliveryContextDto implements DeliveryContextDto {
    @NotNull(message = "페이지 ID를 입력해주세요.")
    private String pageId;
    private String title;
    private Map<String, Object> additionalAttributes;
}

