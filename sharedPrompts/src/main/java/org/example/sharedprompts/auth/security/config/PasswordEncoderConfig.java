package org.example.sharedprompts.auth.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

/**
 * 비밀번호 인코더 설정.
 * 
 * DelegatingPasswordEncoder를 사용하여 향후 다른 인코더(Argon2 등) 추가가 가능하도록 구성합니다.
 */
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        String idForEncode = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(idForEncode, new BCryptPasswordEncoder());
        
        // 향후 Argon2 등 다른 인코더 추가 가능
        // encoders.put("argon2", new Argon2PasswordEncoder());
        
        return new DelegatingPasswordEncoder(idForEncode, encoders);
    }
}

