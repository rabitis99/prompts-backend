package org.example.sharedprompts.domain.delivery.api;

public interface DeliveryResult {
    boolean isSuccess();
    String getErrorMessage();
}

