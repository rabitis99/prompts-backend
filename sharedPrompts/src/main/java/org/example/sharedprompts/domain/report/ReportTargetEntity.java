package org.example.sharedprompts.domain.report;

import lombok.Getter;
import org.example.sharedprompts.domain.comment.Comment;
import org.example.sharedprompts.domain.prompt.Prompt;

@Getter
public class ReportTargetEntity {
    private final Prompt prompt;
    private final Comment comment;

    public ReportTargetEntity(Prompt prompt, Comment comment) {
        this.prompt = prompt;
        this.comment = comment;
    }
}

