package org.example.sharedprompts.module.domain.delivery.api;

public interface DeliveryResult {
    boolean isSuccess();
    String getErrorMessage();
}

