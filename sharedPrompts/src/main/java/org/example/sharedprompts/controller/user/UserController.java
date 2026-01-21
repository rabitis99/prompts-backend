package org.example.sharedprompts.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.user.service.UserService;
import org.example.sharedprompts.dto.user.request.PasswordChangeRequestDto;
import org.example.sharedprompts.dto.user.request.UserUpdateRequestDto;
import org.example.sharedprompts.dto.user.response.UserPublicProfileDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /* =========================
       자신의 정보 관리 (My Info)
       ========================= */

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<CustomResponse<UserResponseDto>> getMyInfo(@CurrentUser AuthUser authUser) {
        UserResponseDto response = userService.getMyInfo(authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 내 정보 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<CustomResponse<UserResponseDto>> updateMyInfo(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody UserUpdateRequestDto requestDto
    ) {
        UserResponseDto response = userService.updateMyInfo(authUser.getId(), requestDto);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 비밀번호 변경
     */
    @PatchMapping("/me/password")
    public ResponseEntity<CustomResponse<UserResponseDto>> changePassword(
            @CurrentUser AuthUser authUser,
            @Valid @RequestBody PasswordChangeRequestDto requestDto
    ) {
        UserResponseDto response = userService.changePassword(authUser.getId(), requestDto);
        return CustomResponseHelper.ok(response);
    }

    /* =========================
       다른 사용자 정보 (Public Profile)
       ========================= */

    /**
     * 다른 사용자의 공개 프로필 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<CustomResponse<UserPublicProfileDto>> getPublicProfile(
            @PathVariable Long userId,
            @CurrentUser AuthUser authUser
    ) {
        UserPublicProfileDto response = userService.getPublicProfile(userId, authUser.getId());
        return CustomResponseHelper.ok(response);
    }

    /**
     * 사용자 삭제 (탈퇴)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @CurrentUser AuthUser authUser
    ) {
        userService.deleteUser(userId, authUser.getId());
        return CustomResponseHelper.noContent();
    }
}
