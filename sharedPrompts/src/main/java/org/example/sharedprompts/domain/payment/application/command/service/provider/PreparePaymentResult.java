package org.example.sharedprompts.domain.payment.application.command.service.provider;

public record PreparePaymentResult(boolean required, String tid, String redirectUrl) {

    public PreparePaymentResult {
        if (required) {
            if (tid == null || tid.isBlank()) {
                throw new IllegalArgumentException("tid는 필수입니다");
            }
            if (redirectUrl == null || redirectUrl.isBlank()) {
                throw new IllegalArgumentException("redirectUrl은 필수입니다");
            }
        }
    }

    public static PreparePaymentResult notRequired() {
        return new PreparePaymentResult(false, null, null);
    }

    public static PreparePaymentResult success(String tid, String redirectUrl) {
        return new PreparePaymentResult(true, tid, redirectUrl);
    }

    public boolean isValid() {
        return !required || (tid != null && !tid.isBlank() && redirectUrl != null && !redirectUrl.isBlank());
    }
}


