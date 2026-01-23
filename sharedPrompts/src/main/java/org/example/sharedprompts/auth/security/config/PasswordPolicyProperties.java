package org.example.sharedprompts.auth.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 정책 설정 Properties.
 * 
 * application.yml의 password.policy 설정을 바인딩합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "password.policy")
public class PasswordPolicyProperties {
    
    /**
     * 최소 비밀번호 길이
     */
    private int minLength = 10;
    
    /**
     * 최대 비밀번호 길이
     */
    private int maxLength = 100;
    
    /**
     * 대문자 필수 여부
     */
    private boolean requireUpperCase = true;
    
    /**
     * 소문자 필수 여부
     */
    private boolean requireLowerCase = true;
    
    /**
     * 숫자 필수 여부
     */
    private boolean requireDigit = true;
    
    /**
     * 특수문자 필수 여부
     */
    private boolean requireSpecialChar = true;
    
    /**
     * 허용되는 특수문자 목록
     */
    private String specialChars = "!@#$%^&*()_+-=[]{}|;:,.<>?";
}

