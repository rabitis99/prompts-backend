package org.example.sharedprompts.domain.statistics.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.statistics.util.StatisticsDateUtils;
import org.example.sharedprompts.domain.user.repository.DailyUserCountProjection;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.dto.statistics.response.DailyNewUsersTrendDto;
import org.example.sharedprompts.dto.statistics.response.UserStatisticsResponseDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 통계 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserStatisticsServiceImpl implements UserStatisticsService {

    private static final String CACHE_NAME = "statistics";
    private static final int ACTIVE_USER_DAYS = 30;

    private final UserRepository userRepository;

    @Override
    @Cacheable(value = CACHE_NAME, key = "'user'", unless = "#result == null")
    public UserStatisticsResponseDto getUserStatistics() {
        log.debug("사용자 통계 조회");

        StatisticsDateUtils.DateRange todayRange = StatisticsDateUtils.todayRange();
        StatisticsDateUtils.DateRange weeklyRange = StatisticsDateUtils.lastDaysRange(7);
        StatisticsDateUtils.DateRange monthlyRange = StatisticsDateUtils.lastDaysRange(ACTIVE_USER_DAYS);

        // 전체 가입자 수
        Long totalUsers = userRepository.count();

        // 활성 사용자 수 (최근 30일 내 활동)
        Long activeUsers = userRepository.countActiveUsersSince(monthlyRange.getStart());

        // 오늘 신규 가입자 수
        Long todayNewUsers = userRepository.countNewUsersBetween(
                todayRange.getStart(), todayRange.getEnd());

        // 최근 7일 신규 가입자 수
        Long weeklyNewUsers = userRepository.countNewUsersBetween(
                weeklyRange.getStart(), weeklyRange.getEnd());

        // 최근 30일 신규 가입자 수
        Long monthlyNewUsers = userRepository.countNewUsersBetween(
                monthlyRange.getStart(), monthlyRange.getEnd());

        // 일별 신규 가입자 추이 (최근 30일)
        List<DailyNewUsersTrendDto> dailyNewUsersTrend = convertDailyNewUsersTrend(
                userRepository.countDailyNewUsersLast30Days());

        return UserStatisticsResponseDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .todayNewUsers(todayNewUsers)
                .weeklyNewUsers(weeklyNewUsers)
                .monthlyNewUsers(monthlyNewUsers)
                .dailyNewUsersTrend(dailyNewUsersTrend)
                .build();
    }

    /**
     * 일별 신규 가입자 추이 데이터 변환
     * Projection 인터페이스를 사용하여 타입 안전하게 변환
     */
    private List<DailyNewUsersTrendDto> convertDailyNewUsersTrend(List<DailyUserCountProjection> results) {
        return results.stream()
                .map(projection -> DailyNewUsersTrendDto.builder()
                        .date(projection.getDate())
                        .count(projection.getCount())
                        .build())
                .collect(Collectors.toList());
    }
}

