package org.example.sharedprompts.domain.admin.validator;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.domain.user.repository.UserRepository;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 관리자 작업에 대한 검증 로직을 담당하는 Validator
 */
@Component
@RequiredArgsConstructor
public class AdminValidator {

    private final UserRepository userRepository;

    /**
     * 관리자 계정인지 검증
     * @param user 검증할 사용자
     * @param errorCode 관리자인 경우 발생시킬 에러 코드
     * @throws ApiException 사용자가 관리자인 경우
     */
    public void validateNotAdmin(User user, ErrorCode errorCode) {
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new ApiException(errorCode);
        }
    }

    /**
     * 자기 자신에 대한 작업인지 검증
     * @param adminId 관리자 ID
     * @param targetUserId 대상 사용자 ID
     * @param errorCode 자기 자신인 경우 발생시킬 에러 코드
     * @throws ApiException 자기 자신에 대한 작업인 경우
     */
    public void validateNotSelf(Long adminId, Long targetUserId, ErrorCode errorCode) {
        if (adminId.equals(targetUserId)) {
            throw new ApiException(errorCode);
        }
    }

    /**
     * 마지막 관리자 계정인지 검증
     * 마지막 관리자 계정의 권한 변경/삭제를 방지
     * @throws ApiException 마지막 관리자 계정인 경우
     */
    public void validateNotLastAdmin() {
        long adminCount = userRepository.countActiveAdmins();
        if (adminCount <= 1) {
            throw new ApiException(ErrorCode.LAST_ADMIN_CANNOT_BE_MODIFIED);
        }
    }

    /**
     * 관리자 권한 변경 검증
     * 관리자에서 일반 사용자로 권한을 변경하려는 경우 마지막 관리자 검증 수행
     * @param currentRole 현재 권한
     * @param newRole 새로운 권한
     */
    public void validateRoleChange(Role currentRole, Role newRole) {
        if (currentRole == Role.ROLE_ADMIN && newRole != Role.ROLE_ADMIN) {
            validateNotLastAdmin();
        }
    }

    /**
     * 동일 권한으로 변경 시도하는지 검증
     * @param currentRole 현재 권한
     * @param newRole 새로운 권한
     * @throws ApiException 동일 권한인 경우
     */
    public void validateNotSameRole(Role currentRole, Role newRole) {
        if (currentRole == newRole) {
            throw new ApiException(ErrorCode.SAME_ROLE);
        }
    }

    /**
     * 동일 공개 상태로 변경 시도하는지 검증
     * @param currentVisibility 현재 공개 상태
     * @param newVisibility 새로운 공개 상태
     * @throws ApiException 동일 상태인 경우
     */
    public void validateNotSameVisibility(boolean currentVisibility, boolean newVisibility) {
        if (currentVisibility == newVisibility) {
            throw new ApiException(ErrorCode.SAME_VISIBILITY);
        }
    }

    /**
     * 날짜 범위 검증
     * 시작일이 종료일보다 이후인지 검증
     * @param startDate 시작일
     * @param endDate 종료일
     * @throws ApiException 시작일이 종료일보다 이후인 경우
     */
    public void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ApiException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    /**
     * 페이징 사이즈 검증
     * 최대 사이즈를 초과하는지 검증
     * @param pageable 페이징 정보
     * @param maxSize 최대 사이즈
     * @throws ApiException 사이즈가 최대값을 초과하는 경우
     */
    public void validatePageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            throw new ApiException(ErrorCode.PAGE_SIZE_EXCEEDED);
        }
    }
}

