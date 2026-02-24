package org.example.sharedprompts.module.github.dto.webhook.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * GitHub Webhook payload 구조 (필요 필드만). push / pull_request 등 이벤트별로 사용.
 */
public final class GitHubWebhookPayloads {

    private GitHubWebhookPayloads() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Repository {
        @JsonProperty("full_name")
        private String fullName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PushPayload {
        @JsonProperty("ref")
        private String ref;
        @JsonProperty("repository")
        private Repository repository;
        @JsonProperty("head_commit")
        private HeadCommit headCommit;
        @JsonProperty("commits")
        private List<HeadCommit> commits;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeadCommit {
        @JsonProperty("id")
        private String id;
        @JsonProperty("message")
        private String message;
        @JsonProperty("timestamp")
        private String timestamp;
        @JsonProperty("author")
        private Author author;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        @JsonProperty("name")
        private String name;
        @JsonProperty("username")
        private String username;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequestPayload {
        @JsonProperty("action")
        private String action;
        @JsonProperty("pull_request")
        private PullRequest pullRequest;
        @JsonProperty("repository")
        private Repository repository;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PullRequest {
        @JsonProperty("title")
        private String title;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("user")
        private User user;
        @JsonProperty("head")
        private Ref head;
        @JsonProperty("base")
        private Ref base;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ref {
        @JsonProperty("ref")
        private String ref;
        @JsonProperty("sha")
        private String sha;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        @JsonProperty("login")
        private String login;
    }
}
