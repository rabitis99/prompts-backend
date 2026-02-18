package org.example.sharedprompts.module.domain.production.service.ai.exception;

import org.example.sharedprompts.module.domain.production.service.ai.ContentType;
import org.example.sharedprompts.module.exception.BaseException;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.UNSUPPORTED_CONTENT_TYPE;

/**
 * 지원하지 않는 콘텐츠 타입 예외
 */
public class UnsupportedContentTypeException extends BaseException {
    
    public UnsupportedContentTypeException(ContentType contentType) {
        super(UNSUPPORTED_CONTENT_TYPE, null, 
                String.format("지원하지 않는 콘텐츠 타입입니다: %s", contentType));
    }
}

