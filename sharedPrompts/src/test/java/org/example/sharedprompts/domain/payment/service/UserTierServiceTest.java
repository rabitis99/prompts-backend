package org.example.sharedprompts.domain.payment.service;

import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.payment.PaymentRepository;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.userTier.UserTierHistoryRepository;
import org.example.sharedprompts.domain.payment.service.user.tier.UserTierServiceImpl;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.payment.request.TierChangeRequestDto;
import org.example.sharedprompts.dto.payment.response.TierInfoResponseDto;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 사용자 티어 서비스 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 티어 서비스 테스트")
class UserTierServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserTierHistoryRepository tierHistoryRepository;

    @InjectMocks
    private UserTierServiceImpl userTierService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .provider(Provider.LOCAL)
                .providerId("test-provider-id")
                .nickname("testuser")
                .role(Role.ROLE_USER)
                .tier(UserTier.FREE)
                .signupCompleted(true)
                .blocked(false)
                .build();
    }

    @Test
    @DisplayName("티어 정보 조회 성공")
    void getTierInfo_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentRepository.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(2L);

        // when
        TierInfoResponseDto result = userTierService.getTierInfo(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTier()).isEqualTo(UserTier.FREE);
        assertThat(result.getDailyLimit()).isEqualTo(0);
        assertThat(result.getTodayUsedCount()).isEqualTo(2);
        assertThat(result.getRemainingCount()).isEqualTo(-2); // 제한 초과
    }

    @Test
    @DisplayName("티어 변경 성공")
    void changeTier_Success() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tierHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TierChangeRequestDto request = TierChangeRequestDto.builder()
                .tier(UserTier.PRO)
                .reason("업그레이드")
                .build();

        // when
        userTierService.changeTier(1L, request, 999L); // 관리자 ID

        // then
        verify(userRepository).save(any(User.class));
        verify(tierHistoryRepository).save(any());
    }

    @Test
    @DisplayName("동일한 티어로 변경 시 예외 발생")
    void changeTier_SameTier() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        TierChangeRequestDto request = TierChangeRequestDto.builder()
                .tier(UserTier.FREE) // 현재 티어와 동일
                .reason("변경 없음")
                .build();

        // when & then
        assertThatThrownBy(() -> userTierService.changeTier(1L, request, 999L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_TIER);
    }

    @Test
    @DisplayName("사용자 없음 시 예외 발생")
    void getTierInfo_UserNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userTierService.getTierInfo(1L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("티어 변경 시 사용자 없음 예외 발생")
    void changeTier_UserNotFound() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        TierChangeRequestDto request = TierChangeRequestDto.builder()
                .tier(UserTier.PRO)
                .reason("업그레이드")
                .build();

        // when & then
        assertThatThrownBy(() -> userTierService.changeTier(1L, request, 999L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }
}

