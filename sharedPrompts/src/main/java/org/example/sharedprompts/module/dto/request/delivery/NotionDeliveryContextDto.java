package org.example.sharedprompts.module.dto.request.delivery;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
    private String pageId;
    private String title;
    private Map<String, Object> additionalAttributes;
}

