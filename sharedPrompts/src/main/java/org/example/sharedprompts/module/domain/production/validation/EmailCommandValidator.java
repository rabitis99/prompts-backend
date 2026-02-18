package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.email.EmailCommand;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * EmailCommand 검증기
 */
@Component
@Slf4j
public class EmailCommandValidator implements ProductionValidator {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    
    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof EmailCommand emailCommand)) {
            throw new ValidationException(
                    "Command is not instance of EmailCommand: " + command.getClass().getName());
        }

        if (emailCommand.subject() == null || emailCommand.subject().isBlank()) {
            throw new ValidationException("subject must not be blank");
        }
        
        if (emailCommand.recipient() == null || emailCommand.recipient().isBlank()) {
            throw new ValidationException("recipient must not be blank");
        }
        
        validateEmailFormat(emailCommand.recipient());
        
        log.debug("EmailCommand validation passed - subject: {}, recipient: {}", 
                emailCommand.subject(), SensitiveDataMasker.maskEmail(emailCommand.recipient()));
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.EMAIL;
    }
    
    /**
     * 이메일 형식 검증
     */
    private void validateEmailFormat(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(
                    "Invalid email format: " + SensitiveDataMasker.maskEmail(email));
        }
    }
}

