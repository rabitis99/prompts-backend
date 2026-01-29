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

    /**
     * Provides a RestTemplate bean for payment-related HTTP calls.
     *
     * @return the RestTemplate configured with the payment HTTP request factory
     */
    @Bean(name = "paymentRestTemplate")
    public RestTemplate paymentRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * Creates a ClientHttpRequestFactory configured for payment HTTP calls.
     *
     * Configures a connection timeout of 5000 ms and a read timeout of 10000 ms.
     *
     * @return a ClientHttpRequestFactory with the connection timeout set to 5000 ms and the read timeout set to 10000 ms
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5초
        factory.setReadTimeout(10000); // 10초
        return factory;
    }
}
