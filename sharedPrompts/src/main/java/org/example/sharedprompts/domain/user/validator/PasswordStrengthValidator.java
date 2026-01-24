package org.example.sharedprompts.domain.user.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.security.config.properties.PasswordPolicyProperties;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 강도 검증을 수행하는 Validator.
 * 
 * PasswordPolicyProperties를 통해 설정 가능한 정책을 적용합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordStrengthValidator {

    private final PasswordPolicyProperties properties;

    /**
     * 비밀번호 강도를 검증합니다.
     * 
     * @param password 검증할 비밀번호
     * @throws ApiException 비밀번호가 강도 요구사항을 만족하지 않을 경우
     */
    public void validate(String password) {
        // 길이 검증
        if (password == null || password.length() < properties.getMinLength()) {
            log.warn("Password validation failed - reason=LENGTH_TOO_SHORT, length={}, minLength={}", 
                    password != null ? password.length() : 0, properties.getMinLength());
            throw new ApiException(ErrorCode.INVALID_PASSWORD_STRENGTH);
        }

        if (password.length() > properties.getMaxLength()) {
            log.warn("Password validation failed - reason=LENGTH_TOO_LONG, length={}, maxLength={}", 
                    password.length(), properties.getMaxLength());
            throw new ApiException(ErrorCode.INVALID_PASSWORD_STRENGTH);
        }

        // 복잡도 검증
        boolean hasUpperCase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowerCase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        boolean hasSpecial = false;
        if (properties.isRequireSpecialChar()) {
            String specialChars = properties.getSpecialChars();
            hasSpecial = password.chars().anyMatch(ch -> 
                    specialChars.indexOf(ch) >= 0);
        }

        // 필수 조건 검증
        boolean validationFailed = false;
        if (properties.isRequireUpperCase() && !hasUpperCase) {
            log.warn("Password validation failed - reason=NO_UPPER_CASE");
            validationFailed = true;
        }
        if (properties.isRequireLowerCase() && !hasLowerCase) {
            log.warn("Password validation failed - reason=NO_LOWER_CASE");
            validationFailed = true;
        }
        if (properties.isRequireDigit() && !hasDigit) {
            log.warn("Password validation failed - reason=NO_DIGIT");
            validationFailed = true;
        }
        if (properties.isRequireSpecialChar() && !hasSpecial) {
            log.warn("Password validation failed - reason=NO_SPECIAL_CHAR");
            validationFailed = true;
        }

        if (validationFailed) {
            log.warn("Password validation failed - reason=INSUFFICIENT_COMPLEXITY, " +
                    "hasUpperCase={}, hasLowerCase={}, hasDigit={}, hasSpecial={}",
                    hasUpperCase, hasLowerCase, hasDigit, hasSpecial);
            throw new ApiException(ErrorCode.INVALID_PASSWORD_STRENGTH);
        }
    }
}

