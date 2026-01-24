package org.example.sharedprompts.controller.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.comment.service.CommentService;
import org.example.sharedprompts.dto.comment.request.CommentRequestDto;
import org.example.sharedprompts.dto.comment.request.CommentUpdateDto;
import org.example.sharedprompts.dto.comment.response.CommentResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/prompts/{promptId}/comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CustomResponse<CommentResponseDto>> createComment(
            @PathVariable Long promptId,
            @Valid @RequestBody CommentRequestDto request,
            @CurrentUser AuthUser authUser
    ) {
        return CustomResponseHelper.created(
                commentService.createComment(authUser.getId(), promptId, request)
        );
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<CommentResponseDto>>> getComments(
            @PathVariable Long promptId
    ) {
        return CustomResponseHelper.ok(
                commentService.getCommentsByPrompt(promptId)
        );
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<CustomResponse<CommentResponseDto>> updateComment(
            @PathVariable Long promptId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateDto request,
            @CurrentUser AuthUser authUser
    ) {
        return CustomResponseHelper.ok(
                commentService.updateComment(authUser.getId(), promptId, commentId, request)
        );
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long promptId,
            @PathVariable Long commentId,
            @CurrentUser AuthUser authUser
    ) {
        commentService.deleteComment(authUser.getId(), promptId, commentId);
        return CustomResponseHelper.noContent();
    }
}
