package org.example.sharedprompts.domain.payment.service.notification.credentials;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.config.PaymentProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultFcmCredentialsProvider implements FcmCredentialsProvider {

    private final PaymentProperties paymentProperties;
    private final FcmCredentialsResolver credentialsResolver;

    @Override
    public GoogleCredentials load() throws IOException {
        try (InputStream stream = credentialsResolver.resolve(paymentProperties)) {
            return GoogleCredentials.fromStream(stream)
                    .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
        }
    }
}

