package org.example.sharedprompts.module.domain.production.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.global.util.SensitiveDataMasker;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommandType;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.springframework.stereotype.Component;

/**
 * ImageCommand 검증기
 */
@Component
@Slf4j
public class ImageCommandValidator implements ProductionValidator {
    
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 4096;
    
    @Override
    public void validate(ProductionCommand command) throws ValidationException {
        if (!(command instanceof ImageCommand imageCommand)) {
            throw new ValidationException(
                    "Command is not instance of ImageCommand: " + command.getClass().getName());
        }

        // prompt는 JobProcessor에서 PromptTemplateService.mergePrompt()로 생성되어
        // AIJobExecutor에 별도 파라미터로 전달됩니다.
        // ImageCommand의 prompt 필드는 FileNameGenerator에서만 사용되며,
        // null/blank일 경우 기본값("image-output")을 사용합니다.
        // 따라서 여기서는 prompt 검증을 건너뜁니다.
        if (imageCommand.prompt() == null || imageCommand.prompt().isBlank()) {
            log.debug("ImageCommand prompt is null or blank - will use default filename in FileNameGenerator");
        }
        
        if (imageCommand.width() < MIN_SIZE || imageCommand.width() > MAX_SIZE) {
            throw new ValidationException(
                    String.format("width must be between %d and %d, but was %d", 
                            MIN_SIZE, MAX_SIZE, imageCommand.width()));
        }
        
        if (imageCommand.height() < MIN_SIZE || imageCommand.height() > MAX_SIZE) {
            throw new ValidationException(
                    String.format("height must be between %d and %d, but was %d", 
                            MIN_SIZE, MAX_SIZE, imageCommand.height()));
        }
        
        validateAspectRatio(imageCommand.width(), imageCommand.height());
        
        log.debug("ImageCommand validation passed - prompt: {}, size: {}x{}", 
                SensitiveDataMasker.mask(imageCommand.prompt()), imageCommand.width(), imageCommand.height());
    }
    
    @Override
    public boolean supports(ProductionCommandType commandType) {
        return commandType == ProductionCommandType.IMAGE;
    }
    
    /**
     * 종횡비 검증
     */
    private void validateAspectRatio(int width, int height) {
        // 최소/최대 비율 제한 (예: 1:4 ~ 4:1)
        double ratio = (double) width / height;
        if (ratio < 0.25 || ratio > 4.0) {
            throw new ValidationException(
                    String.format("Aspect ratio must be between 1:4 and 4:1, but was %.2f:1", ratio));
        }
    }
}

