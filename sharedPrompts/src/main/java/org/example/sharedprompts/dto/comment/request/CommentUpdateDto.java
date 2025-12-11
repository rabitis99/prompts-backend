package org.example.sharedprompts.dto.comment.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.comment.Comment;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateDto {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;

    public void apply(Comment comment) {
        comment.updateContent(content);
    }
}
