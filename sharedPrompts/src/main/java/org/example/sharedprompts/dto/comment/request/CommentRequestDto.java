package org.example.sharedprompts.dto.comment.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;
import org.example.sharedprompts.domain.user.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequestDto {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    private Long parentId;

    public Comment toEntity(User user, Prompt prompt, Comment parent) {
        return Comment.builder()
                .content(content)
                .user(user)
                .prompt(prompt)
                .parent(parent)
                .build();
    }
}