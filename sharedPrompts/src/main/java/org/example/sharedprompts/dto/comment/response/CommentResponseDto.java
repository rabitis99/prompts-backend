package org.example.sharedprompts.dto.comment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.dto.user.response.UserResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponseDto {

    private Long id;
    private String content;
    private UserResponseDto  user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long replyCount;
    private Long parentId;
    private List<CommentResponseDto> replies;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(UserResponseDto.from(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount(comment.getReplyCount())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(
                        comment.getChildren().stream()
                                .map(CommentResponseDto::from)
                                .toList()
                )
                .build();
    }
}