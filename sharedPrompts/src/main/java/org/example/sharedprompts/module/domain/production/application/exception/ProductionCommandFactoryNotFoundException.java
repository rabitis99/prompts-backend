package org.example.sharedprompts.module.domain.production.application.exception;

/**
 * ProductionCommandFactory를 찾을 수 없을 때 발생하는 예외
 */
public class ProductionCommandFactoryNotFoundException extends ProductionApplicationException {
    
    public ProductionCommandFactoryNotFoundException(String message) {
        super(message);
    }
    
    public ProductionCommandFactoryNotFoundException(Class<?> requestType) {
        super("ProductionCommandFactory not found for request type: " + requestType.getName());
    }
}




