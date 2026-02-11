package org.example.sharedprompts.module.domain.production.application.factory;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;

/**
 * ProductionCommand 생성 전략 인터페이스
 * 
 * <p>각 Production 타입별로 ProductionRequest를 Command로 변환하는 책임을 가진다.
 * 명시적 타입 계약을 통해 컴파일 타임 타입 안정성을 보장한다.</p>
 */
public interface ProductionCommandFactory {
    
    /**
     * 이 Factory가 지원하는 Command 타입을 반환한다.
     */
    ProductionCommandType getSupportedCommandType();
    
    /**
     * 이 Factory가 지원하는 Request 타입을 반환한다.
     * 명시적 계약을 통해 런타임 탐색 없이 Factory를 찾을 수 있다.
     */
    Class<? extends ProductionRequest> getSupportedRequestType();
    
    /**
     * ProductionRequest를 ProductionCommand로 변환한다.
     * 
     * @param request ProductionRequest 인스턴스
     * @return ProductionCommand 인스턴스
     * @throws IllegalArgumentException request가 지원하지 않는 타입이거나 null인 경우
     */
    ProductionCommand createCommand(ProductionRequest request);
}

