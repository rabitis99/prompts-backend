package org.example.sharedprompts.module.domain.production.application.factory;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.application.exception.ProductionCommandFactoryNotFoundException;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProductionCommandFactory 레지스트리
 * 
 * <p>명시적 매핑 기반으로 Factory를 등록하고 조회한다.
 * getSupportedRequestType()을 통해 런타임 탐색 없이 O(1) 조회가 가능하다.</p>
 */
@Component
@Slf4j
public class ProductionCommandFactoryRegistry {
    
    private final Map<Class<? extends ProductionRequest>, ProductionCommandFactory> factoryByRequestType;
    private final Map<ProductionCommandType, ProductionCommandFactory> factoryByCommandType;
    
    /**
     * 모든 Factory 구현체를 자동 주입받아 명시적으로 등록한다.
     */
    public ProductionCommandFactoryRegistry(List<ProductionCommandFactory> factories) {
        this.factoryByRequestType = new ConcurrentHashMap<>();
        this.factoryByCommandType = new ConcurrentHashMap<>();
        
        for (ProductionCommandFactory factory : factories) {
            ProductionCommandType commandType = factory.getSupportedCommandType();
            Class<? extends ProductionRequest> requestType = factory.getSupportedRequestType();
            
            // 명시적 매핑 등록
            factoryByCommandType.put(commandType, factory);
            factoryByRequestType.put(requestType, factory);
            
            log.info("Registered ProductionCommandFactory: {} for CommandType: {}, RequestType: {}", 
                    factory.getClass().getSimpleName(), commandType, requestType.getSimpleName());
        }
    }
    
    /**
     * Request 타입에 따라 Factory를 조회한다.
     * 명시적 매핑을 사용하므로 O(1) 조회 성능을 보장한다.
     * 
     * @param requestType ProductionRequest의 Class
     * @return 해당하는 Factory
     * @throws ProductionCommandFactoryNotFoundException 지원하지 않는 Request 타입인 경우
     */
    public ProductionCommandFactory getFactory(Class<? extends ProductionRequest> requestType) {
        ProductionCommandFactory factory = factoryByRequestType.get(requestType);
        if (factory == null) {
            throw new ProductionCommandFactoryNotFoundException(requestType);
        }
        return factory;
    }
    
    /**
     * Command 타입에 따라 Factory를 조회한다.
     * 
     * @param commandType ProductionCommandType
     * @return 해당하는 Factory
     * @throws ProductionCommandFactoryNotFoundException 지원하지 않는 Command 타입인 경우
     */
    public ProductionCommandFactory getFactory(ProductionCommandType commandType) {
        ProductionCommandFactory factory = factoryByCommandType.get(commandType);
        if (factory == null) {
            throw new ProductionCommandFactoryNotFoundException(
                    "ProductionCommandFactory not found for command type: " + commandType);
        }
        return factory;
    }
}

