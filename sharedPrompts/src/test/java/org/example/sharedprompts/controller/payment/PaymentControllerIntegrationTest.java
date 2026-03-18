package org.example.sharedprompts.controller.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentMethod;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentUserType;
import org.example.sharedprompts.domain.prompt.infrastructure.config.TestActionTypeMetadataConfig;
import org.example.sharedprompts.dto.payment.request.PaymentRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 결제 컨트롤러 통합 테스트
 */
@SpringBootTest
@Import(TestActionTypeMetadataConfig.class)
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("결제 컨트롤러 통합 테스트")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("결제 요청 통합 테스트")
    void requestPayment_IntegrationTest() throws Exception {
        // given
        PaymentRequestDto request = PaymentRequestDto.builder()
                .amount(new BigDecimal("10000"))
                .currency("KRW")
                .paymentMethod(PaymentMethod.KAKAO_PAY)
                .userType(PaymentUserType.PERSONAL)
                .build();

        // TODO: 실제 인증 토큰을 사용하여 테스트
        // when & then
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // 인증 없이 호출 시 401
    }

    @Test
    @DisplayName("결제 취소 스모크 - 인증 없이 401")
    void cancelPayment_Unauthorized() throws Exception {
        mockMvc.perform(post("/payments/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"1\",\"reason\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("결제 상태 조회 스모크 - 인증 없이 401")
    void checkPaymentStatus_Unauthorized() throws Exception {
        mockMvc.perform(get("/payments/1/status"))
                .andExpect(status().isUnauthorized());
    }
}

