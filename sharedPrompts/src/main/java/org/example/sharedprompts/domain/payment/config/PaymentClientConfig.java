package org.example.sharedprompts.domain.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 결제 관련 HTTP 클라이언트 설정
 */
@Configuration
public class PaymentClientConfig {

    @Bean(name = "paymentRestTemplate")
    public RestTemplate paymentRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초
        factory.setReadTimeout(10000); // 10초
        return factory;
    }
}

