package org.example.sharedprompts.module.domain.production.service.renderer;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;

/**
 * Production Renderer 인터페이스
 */
public interface ProductionRenderer {

    /**
     * 파싱된 AI 응답을 사용자 요청 포맷으로 렌더링
     */
    String render(JsonNode parsedResponse);

    /**
     * 지원하는 CommandType 확인
     */
    boolean supports(ProductionCommandType commandType);
}

