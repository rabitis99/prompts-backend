package org.example.sharedprompts.dto.comment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 5000, message = "댓글 내용은 최대 5000자까지 입력해주세요.")
    private String content;

    public void apply(Comment comment) {
        comment.updateContent(content);
    }
}
