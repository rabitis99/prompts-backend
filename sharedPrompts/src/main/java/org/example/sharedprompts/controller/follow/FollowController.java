package org.example.sharedprompts.controller.follow;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.follow.FollowStatus;
import org.example.sharedprompts.domain.follow.service.FollowService;
import org.example.sharedprompts.dto.follow.response.FollowCountResponseDto;
import org.example.sharedprompts.dto.follow.response.FollowResponseDto;
import org.example.sharedprompts.dto.user.response.UserResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.example.sharedprompts.global.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/users/{followingId}/follow")
    public ResponseEntity<CustomResponse<Void>> requestFollow(
            @PathVariable Long followingId,
            @CurrentUser AuthUser authUser
    ) {
        followService.requestFollow(authUser.getId(), followingId);
        return CustomResponseHelper.created(null);
    }

    @PostMapping("/users/{followerId}/follow/accept")
    public ResponseEntity<CustomResponse<Void>> acceptFollow(
            @PathVariable Long followerId,
            @CurrentUser AuthUser authUser
    ) {
        followService.acceptFollow(followerId, authUser.getId());
        return CustomResponseHelper.ok(null);
    }

    @PostMapping("/users/{followerId}/follow/reject")
    public ResponseEntity<Void> rejectFollow(
            @PathVariable Long followerId,
            @CurrentUser AuthUser authUser
    ) {
        followService.rejectFollow(followerId, authUser.getId());
        return CustomResponseHelper.noContent();
    }

    @PostMapping("/users/{followingId}/follow/block")
    public ResponseEntity<CustomResponse<Void>> blockFollow(
            @PathVariable Long followingId,
            @CurrentUser AuthUser authUser
    ) {
        followService.blockFollow(authUser.getId(), followingId);
        return CustomResponseHelper.ok(null);
    }

    @DeleteMapping("/users/{followingId}/follow/block")
    public ResponseEntity<Void> unblockFollow(
            @PathVariable Long followingId,
            @CurrentUser AuthUser authUser
    ) {
        followService.unblockFollow(authUser.getId(), followingId);
        return CustomResponseHelper.noContent();
    }

    @DeleteMapping("/users/{followingId}/follow")
    public ResponseEntity<Void> unfollow(
            @PathVariable Long followingId,
            @CurrentUser AuthUser authUser
    ) {
        followService.unfollow(authUser.getId(), followingId);
        return CustomResponseHelper.noContent();
    }

    @GetMapping("/users/{followingId}/follow")
    public ResponseEntity<CustomResponse<FollowResponseDto>> getFollowStatus(
            @PathVariable Long followingId,
            @CurrentUser AuthUser authUser
    ) {
        FollowResponseDto response = followService.getFollowStatus(authUser.getId(), followingId);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/users/{userId}/followers")
    public ResponseEntity<CustomResponse<PageResponse<UserResponseDto>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(required = false) FollowStatus status,
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<UserResponseDto> response = followService.getFollowers(userId, status, pageable);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/users/{userId}/following")
    public ResponseEntity<CustomResponse<PageResponse<UserResponseDto>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(required = false) FollowStatus status,
            @CurrentUser AuthUser authUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<UserResponseDto> response = followService.getFollowing(userId, status, pageable);
        return CustomResponseHelper.ok(response);
    }

    @GetMapping("/users/{userId}/follow/count")
    public ResponseEntity<CustomResponse<FollowCountResponseDto>> getFollowCount(
            @PathVariable Long userId,
            @RequestParam(required = false) FollowStatus status,
            @CurrentUser AuthUser authUser
    ) {
        FollowCountResponseDto response = followService.getFollowCount(userId, status);
        return CustomResponseHelper.ok(response);
    }
}

