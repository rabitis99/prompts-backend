package org.example.sharedprompts.domain.payment.service;

import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.domain.enums.PaymentStatus;
import org.example.sharedprompts.domain.payment.domain.enums.UserTier;
import org.example.sharedprompts.domain.payment.domain.policy.TierLimitPolicy;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.PaymentJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.adapter.UserTierHistoryJpaAdapter;
import org.example.sharedprompts.domain.payment.infrastructure.persistence.repository.moduleusage.ModuleUsageRepository;
import org.example.sharedprompts.domain.payment.service.user.tier.TierUpgradePolicy;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private PaymentJpaAdapter paymentJpaAdapter;

    @Mock
    private UserTierHistoryJpaAdapter tierHistoryJpaAdapter;

    @Mock
    private ModuleUsageRepository moduleUsageRepository;

    @Mock
    private TierUpgradePolicy tierUpgradePolicy;

    @Mock
    private TierLimitPolicy tierLimitPolicy;

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
    @DisplayName("티어 정보 조회 성공 (통합 한도, 모듈별 차감량 반영)")
    void getTierInfo_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentJpaAdapter.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(2L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(anyLong(), any(ModuleType.class))).thenReturn(0L);
        when(tierLimitPolicy.getDailyLimit(UserTier.FREE)).thenReturn(15);
        when(tierLimitPolicy.getConsumptionAmount(any(ModuleType.class))).thenReturn(1);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.LITERARY)).thenReturn(2);

        TierInfoResponseDto result = userTierService.getTierInfo(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTier()).isEqualTo(UserTier.FREE);
        assertThat(result.getDailyLimit()).isEqualTo(15);
        assertThat(result.getTodayUsedCount()).isEqualTo(2);
        assertThat(result.getRemainingCount()).isEqualTo(13);
        assertThat(result.getRemainingByModuleType()).isNotNull();
        assertThat(result.getRemainingByModuleType().get("LITERARY")).isEqualTo(11);
    }

    @Test
    @DisplayName("티어 변경 성공")
    void changeTier_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentJpaAdapter.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(0L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(anyLong(), any(ModuleType.class))).thenReturn(0L);
        when(tierLimitPolicy.getDailyLimit(any(UserTier.class))).thenReturn(100);
        when(tierLimitPolicy.getConsumptionAmount(any(ModuleType.class))).thenReturn(1);

        TierChangeRequestDto request = TierChangeRequestDto.builder()
                .tier(UserTier.PRO)
                .reason("업그레이드")
                .build();

        userTierService.changeTier(1L, request, 999L);

        verify(tierHistoryJpaAdapter).save(any());
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

    @Test
    @DisplayName("모듈 사용 1회 차감 성공 (통합 한도, LITERARY 2점 차감)")
    void consumeModuleUsage_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tierLimitPolicy.getDailyLimit(UserTier.FREE)).thenReturn(15);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.LITERARY)).thenReturn(2);
        when(paymentJpaAdapter.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(0L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(eq(1L), any(ModuleType.class)))
                .thenAnswer(inv -> inv.getArgument(1) == ModuleType.LITERARY ? 5L : 0L);

        var response = userTierService.consumeModuleUsage(1L, ModuleType.LITERARY);

        verify(moduleUsageRepository).save(any());
        assertThat(response.getModuleType()).isEqualTo(ModuleType.LITERARY);
        assertThat(response.getLimit()).isEqualTo(15);
        assertThat(response.getConsumptionAmount()).isEqualTo(2);
        assertThat(response.getUsedToday()).isEqualTo(12);
        assertThat(response.getRemaining()).isEqualTo(3);
    }

    @Test
    @DisplayName("모듈 사용 차감 시 통합 한도 초과면 MODULE_DAILY_LIMIT_EXCEEDED")
    void consumeModuleUsage_DailyLimitExceeded() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tierLimitPolicy.getDailyLimit(UserTier.FREE)).thenReturn(15);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.LITERARY)).thenReturn(2);
        when(paymentJpaAdapter.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(0L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(eq(1L), any(ModuleType.class)))
                .thenAnswer(inv -> inv.getArgument(1) == ModuleType.LITERARY ? 7L : 0L);

        assertThatThrownBy(() -> userTierService.consumeModuleUsage(1L, ModuleType.LITERARY))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MODULE_DAILY_LIMIT_EXCEEDED);
        verify(moduleUsageRepository, never()).save(any());
    }

    @Test
    @DisplayName("LITERARY 2회 사용 시 통합 한도에서 4 소비, EMAIL 조회 시 동일 remaining")
    void consumeModuleUsage_countsByModuleType() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tierLimitPolicy.getDailyLimit(UserTier.FREE)).thenReturn(15);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.LITERARY)).thenReturn(2);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.EMAIL)).thenReturn(1);
        when(tierLimitPolicy.getConsumptionAmount(any(ModuleType.class))).thenReturn(1);
        when(paymentJpaAdapter.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(0L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(1L, any(ModuleType.class))).thenReturn(0L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(1L, ModuleType.LITERARY)).thenReturn(0L, 1L, 2L, 2L, 2L);

        userTierService.consumeModuleUsage(1L, ModuleType.LITERARY);
        userTierService.consumeModuleUsage(1L, ModuleType.LITERARY);

        TierInfoResponseDto info = userTierService.getTierInfo(1L, ModuleType.EMAIL);
        assertThat(info.getRemainingCount()).isEqualTo(11);
        assertThat(info.getTodayUsedCount()).isEqualTo(4);

        TierInfoResponseDto literaryInfo = userTierService.getTierInfo(1L, ModuleType.LITERARY);
        assertThat(literaryInfo.getRemainingCount()).isEqualTo(11);
        assertThat(literaryInfo.getTodayUsedCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("통합 한도 소진 시 LITERARY(2점)는 막히고 EMAIL(1점)은 가능")
    void consumeModuleUsage_DailyLimitExceeded_perModule() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(tierLimitPolicy.getDailyLimit(UserTier.FREE)).thenReturn(15);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.LITERARY)).thenReturn(2);
        when(tierLimitPolicy.getConsumptionAmount(ModuleType.EMAIL)).thenReturn(1);
        when(paymentJpaAdapter.countTodaySuccessfulPayments(1L, PaymentStatus.SUCCESS)).thenReturn(0L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(1L, ModuleType.LITERARY)).thenReturn(7L);
        when(moduleUsageRepository.countTodayByUserIdAndModuleType(1L, any(ModuleType.class))).thenReturn(0L);

        assertThatThrownBy(() -> userTierService.consumeModuleUsage(1L, ModuleType.LITERARY))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MODULE_DAILY_LIMIT_EXCEEDED);

        var response = userTierService.consumeModuleUsage(1L, ModuleType.EMAIL);
        assertThat(response.getModuleType()).isEqualTo(ModuleType.EMAIL);
        assertThat(response.getRemaining()).isEqualTo(0);
        verify(moduleUsageRepository, times(1)).save(any());
    }
}