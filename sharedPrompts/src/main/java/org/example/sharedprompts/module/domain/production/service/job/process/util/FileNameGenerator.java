package org.example.sharedprompts.module.domain.production.service.job.process.util;

import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.executor.blog.BlogCommand;
import org.example.sharedprompts.module.domain.production.model.executor.document.DocumentCommand;
import org.example.sharedprompts.module.domain.production.model.executor.email.EmailCommand;
import org.example.sharedprompts.module.domain.production.model.executor.image.ImageCommand;
import org.example.sharedprompts.module.domain.production.model.executor.text.TextCommand;
import org.springframework.stereotype.Component;

@Component
public class FileNameGenerator {

    public String generate(ProductionCommand command) {
        if (command instanceof TextCommand textCommand) {
            return textCommand.fileName() != null && !textCommand.fileName().isBlank()
                    ? textCommand.fileName()
                    : "text-output";
        } else if (command instanceof ImageCommand imageCommand) {
            String prompt = imageCommand.prompt();
            if (prompt != null && !prompt.isBlank()) {
                String sanitized = prompt.substring(0, Math.min(20, prompt.length()))
                        .replaceAll("[^a-zA-Z0-9가-힣]", "-")
                        .toLowerCase();
                return sanitized.isEmpty() ? "image-output" : sanitized;
            }
            return "image-output";
        } else if (command instanceof BlogCommand blogCommand) {
            String title = blogCommand.title();
            if (title != null && !title.isBlank()) {
                String sanitized = title.replaceAll("[^a-zA-Z0-9가-힣]", "-")
                        .toLowerCase();
                return sanitized.isEmpty() ? "blog-output" : sanitized;
            }
            return "blog-output";
        } else if (command instanceof EmailCommand emailCommand) {
            String subject = emailCommand.subject();
            if (subject != null && !subject.isBlank()) {
                String sanitized = subject.replaceAll("[^a-zA-Z0-9가-힣]", "-")
                        .toLowerCase();
                return sanitized.isEmpty() ? "email-output" : sanitized;
            }
            return "email-output";
        } else if (command instanceof DocumentCommand documentCommand) {
            return documentCommand.fileName() != null && !documentCommand.fileName().isBlank()
                    ? documentCommand.fileName()
                    : "document-output";
        }
        return "output";
    }
}

