package org.example.sharedprompts.domain.comment.service;

import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.entity.Prompt;
import org.example.sharedprompts.domain.user.User;
import org.example.sharedprompts.domain.user.enums.Role;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class CommentPermissionChecker {

    public void check(User user, Prompt prompt, Comment comment) {
        if (!comment.getPrompt().equals(prompt)) {
            throw new ApiException(ErrorCode.COMMENT_NOT_BELONG_TO_PROMPT);
        }

        boolean isAuthor = comment.getUser().equals(user);
        boolean isPromptAuthor = prompt.getAuthor().equals(user);
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!(isAuthor || isPromptAuthor || isAdmin)) {
            throw new ApiException(ErrorCode.COMMENT_FORBIDDEN);
        }
    }
}


