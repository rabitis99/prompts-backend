package org.example.sharedprompts.domain.payment.service.notification.credentials;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;

public interface FcmCredentialsProvider {
    GoogleCredentials load() throws IOException;
}