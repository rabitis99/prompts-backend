package org.example.sharedprompts.domain.payment.application.command.service.metadata;

public final class PaymentMetadataKeys {
    public static final String PRODUCT_NAME = "product_name";
    public static final String NEXT_REDIRECT_PC_URL = "next_redirect_pc_url";
    public static final String REDIRECT_URL = "redirect_url";
    public static final String TID = "tid";
    
    private static final String DEFAULT_PRODUCT_NAME = "상품";
    
    public static String getDefaultProductName() {
        return DEFAULT_PRODUCT_NAME;
    }
    
    private PaymentMetadataKeys() {
        throw new AssertionError("Utility class");
    }
}

