package org.example.sharedprompts.domain.prompt.application.exception;

/** 품질 파이프라인 비활성화 옵션 미지원 */
public class UnsupportedQualityPipelineOptionException extends PromptDomainException {

    public UnsupportedQualityPipelineOptionException() {
        super("disableQualityPipeline 옵션은 현재 지원되지 않습니다.");
    }
}

