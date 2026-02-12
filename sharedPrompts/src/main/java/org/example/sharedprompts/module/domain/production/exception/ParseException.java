package org.example.sharedprompts.module.domain.production.exception;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.AI_PARSE_ERROR;

public class ParseException extends BaseException {
    
    public ParseException(String message) {
        super(AI_PARSE_ERROR, null, message);
    }

    public ParseException(String message, Throwable cause) {
        super(AI_PARSE_ERROR, null, message, cause);
    }

    public ParseException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public ParseException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}

