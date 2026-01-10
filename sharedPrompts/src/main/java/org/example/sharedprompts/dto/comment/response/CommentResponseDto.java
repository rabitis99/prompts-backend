package org.example.sharedprompts.dto.comment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("user_response_dto")
    private UserResponseDto userResponseDto;
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("reply_count")
    private Long replyCount;
    @JsonProperty("parent_id")
    private Long parentId;
    private List<CommentResponseDto> replies;

    @JsonProperty("like_count")
    private Long likeCount;

    public static CommentResponseDto from(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userResponseDto(UserResponseDto.from(comment.getUser()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replyCount(comment.getReplyCount())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replies(
                        comment.getChildren().stream()
                                .map(CommentResponseDto::from)
                                .toList()
                )
                .likeCount(comment.getLikeCount())
                .build();
    }
}