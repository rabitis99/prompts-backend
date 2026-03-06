package org.example.sharedprompts.domain.prompt.application.exception;

/**
 * 지원되지 않는 품질 파이프라인 옵션 사용 시 던지는 예외.
 */
public class UnsupportedQualityPipelineOptionException extends PromptDomainException {

    public UnsupportedQualityPipelineOptionException() {
        super("disableQualityPipeline 옵션은 현재 지원되지 않습니다.");
    }
}

