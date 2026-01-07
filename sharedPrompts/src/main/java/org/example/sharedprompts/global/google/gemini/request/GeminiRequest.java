package org.example.sharedprompts.global.google.gemini.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    public static GeminiRequest fromUserPrompt(String prompt) {
        return new GeminiRequest(
                List.of(
                        new Content(
                                "user",
                                List.of(new Part(prompt))
                        )
                )
        );
    }
}
