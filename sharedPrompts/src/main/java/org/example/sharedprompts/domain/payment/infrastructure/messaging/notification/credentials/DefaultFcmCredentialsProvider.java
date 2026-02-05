package org.example.sharedprompts.domain.payment.infrastructure.messaging.notification.credentials;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.properties.FcmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "payment.enabled", havingValue = "true")
public class DefaultFcmCredentialsProvider implements FcmCredentialsProvider {

    private final FcmProperties fcmProperties;
    private final FcmCredentialsResolver credentialsResolver;

    @Override
    public GoogleCredentials load() throws IOException {
        try (InputStream stream = credentialsResolver.resolve(fcmProperties)) {
            return GoogleCredentials.fromStream(stream)
                    .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
        }
    }
}

