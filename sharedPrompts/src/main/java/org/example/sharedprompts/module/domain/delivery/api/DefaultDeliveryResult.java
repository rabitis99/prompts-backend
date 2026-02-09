package org.example.sharedprompts.module.domain.delivery.api;

public class DefaultDeliveryResult implements DeliveryResult {
    private final boolean success;
    private final String errorMessage;
    
    private DefaultDeliveryResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public static DefaultDeliveryResult success() {
        return new DefaultDeliveryResult(true, null);
    }
    
    public static DefaultDeliveryResult failure(String errorMessage) {
        return new DefaultDeliveryResult(false, errorMessage);
    }
    
    @Override
    public boolean isSuccess() {
        return success;
    }
    
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}

