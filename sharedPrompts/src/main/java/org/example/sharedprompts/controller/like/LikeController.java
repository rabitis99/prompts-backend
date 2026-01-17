package org.example.sharedprompts.controller.like;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.like.service.LikeService;
import org.example.sharedprompts.dto.like.response.CommentLikeResponseDto;
import org.example.sharedprompts.dto.like.response.PromptLikeResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/prompts/{promptId}/likes")
    public ResponseEntity<CustomResponse<Void>> likePrompt(
            @PathVariable Long promptId,
            @CurrentUser AuthUser authUser
    ) {
        likeService.likePrompt(authUser.getId(), promptId);
        return CustomResponseHelper.ok(null);
    }

    @GetMapping("/prompts/{promptId}/likes")
    public ResponseEntity<CustomResponse<PromptLikeResponseDto>> checkPromptLike(
            @PathVariable Long promptId,
            @CurrentUser AuthUser authUser
    ) {
        PromptLikeResponseDto response = likeService.checkPromptLike(authUser.getId(), promptId);
        return CustomResponseHelper.ok(response);
    }

    @DeleteMapping("/prompts/{promptId}/likes")
    public ResponseEntity<CustomResponse<Void>> unlikePrompt(
            @PathVariable Long promptId,
            @CurrentUser AuthUser authUser
    ) {
        likeService.unlikePrompt(authUser.getId(), promptId);
        return CustomResponseHelper.ok(null);
    }

    // Comment Like
    @PostMapping("/comments/{commentId}/likes")
    public ResponseEntity<CustomResponse<Void>> likeComment(
            @PathVariable Long commentId,
            @CurrentUser AuthUser authUser
    ) {
        likeService.likeComment(authUser.getId(), commentId);
        return CustomResponseHelper.ok(null);
    }

    @GetMapping("/comments/{commentId}/likes")
    public ResponseEntity<CustomResponse<CommentLikeResponseDto>> checkCommentLike(
            @PathVariable Long commentId,
            @CurrentUser AuthUser authUser
    ) {
        CommentLikeResponseDto response = likeService.checkCommentLike(authUser.getId(), commentId);
        return CustomResponseHelper.ok(response);
    }

    @DeleteMapping("/comments/{commentId}/likes")
    public ResponseEntity<CustomResponse<Void>> unlikeComment(
            @PathVariable Long commentId,
            @CurrentUser AuthUser authUser
    ) {
        likeService.unlikeComment(authUser.getId(), commentId);
        return CustomResponseHelper.ok(null);
    }
}
