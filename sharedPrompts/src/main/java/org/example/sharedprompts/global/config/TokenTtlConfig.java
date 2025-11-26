package org.example.sharedprompts.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class TokenTtlConfig {

    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public TokenTtlConfig(
            @Value("${jwt.access.expiration}") long accessTokenValidity,
            @Value("${jwt.refresh.expiration}") long refreshTokenValidity
    ) {
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

}