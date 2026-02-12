package org.example.sharedprompts.module.domain.production.service.format;

import org.example.sharedprompts.module.exception.BaseException;
import org.example.sharedprompts.module.exception.ModuleErrorCode;

import static org.example.sharedprompts.module.exception.ModuleErrorCode.FORMAT_CONVERSION_ERROR;

public class FormatConversionException extends BaseException {

    public FormatConversionException(String message) {
        super(FORMAT_CONVERSION_ERROR, null, message);
    }

    public FormatConversionException(String message, Throwable cause) {
        super(FORMAT_CONVERSION_ERROR, null, message, cause);
    }

    public FormatConversionException(ModuleErrorCode errorCode, String message) {
        super(errorCode, null, message);
    }

    public FormatConversionException(ModuleErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, null, message, cause);
    }
}
