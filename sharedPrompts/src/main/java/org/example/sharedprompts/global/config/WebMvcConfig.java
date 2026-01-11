package org.example.sharedprompts.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        boolean isWildcard = origins.size() == 1 && "*".equals(origins.get(0));
        
        if (isWildcard) {
            throw new IllegalStateException(
                "CORS 설정 오류: 와일드카드(*) origin과 allowCredentials(true)는 동시에 사용할 수 없습니다.");
        }

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(new String[0]))
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("Content-Type","Authorization","Accept","Origin")
                .maxAge(3600)
                .allowCredentials(true);
    }

}
