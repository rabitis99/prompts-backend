package org.example.sharedprompts.module.domain.production.service.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.springframework.stereotype.Component;

/**
 * Image Renderer
 * 
 * <p>Image Command의 파싱된 응답을 HTML img 태그로 렌더링
 */
@Component
@Slf4j
public class ImageRenderer implements ProductionRenderer {

    @Override
    public String render(JsonNode parsedResponse) {
        log.debug("Rendering image content");

        // content 필드에서 이미지 URL 또는 경로 추출
        String imagePath = parsedResponse.has("content") 
                ? parsedResponse.get("content").asText() 
                : "";

        if (imagePath == null || imagePath.isBlank()) {
            log.warn("Image path is empty in parsed response");
            return "";
        }

        // 이미지 경로를 그대로 반환 (HTML 변환 없이)
        // PNG 파일 경로를 그대로 저장하여 presigned URL 적용
        log.debug("Image path extracted - path: {}", imagePath);
        return imagePath;
    }

    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.IMAGE;
    }

}

