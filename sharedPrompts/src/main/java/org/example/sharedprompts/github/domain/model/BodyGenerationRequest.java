package org.example.sharedprompts.github.domain.model;

/**
 * 본문 생성 요청 (내부용 도메인 모델).
 *
 * Webhook 페이로드 파싱 후 GitHubBodyRequestDto로 변환하기 직전의 중간 표현.
 * 이 객체는 도메인 로직이 자유롭게 사용할 수 있는 형태.
 *
 * 관계:
 * - WebhookPayloadHandler가 parse() 메서드에서 생성해서 반환
 * - BodyGenerationService가 수신해서 GitHubBodyRequestDto로 변환
 */
public record BodyGenerationRequest(
    String jobId,
    String deliveryId,
    String sha,
    String repoFullName,
    String branch,
    String baseBranch,
    String title,
    String author,
    String date,
    String commits,
    String files,
    String tenantId
) {
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String jobId;
    private String deliveryId;
    private String sha;
    private String repoFullName;
    private String branch;
    private String baseBranch;
    private String title;
    private String author;
    private String date;
    private String commits = "";
    private String files = "";
    private String tenantId;

    public Builder jobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    public Builder deliveryId(String deliveryId) {
      this.deliveryId = deliveryId;
      return this;
    }

    public Builder sha(String sha) {
      this.sha = sha;
      return this;
    }

    public Builder repoFullName(String repoFullName) {
      this.repoFullName = repoFullName;
      return this;
    }

    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    public Builder baseBranch(String baseBranch) {
      this.baseBranch = baseBranch;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder author(String author) {
      this.author = author;
      return this;
    }

    public Builder date(String date) {
      this.date = date;
      return this;
    }

    public Builder commits(String commits) {
      this.commits = commits != null ? commits : "";
      return this;
    }

    public Builder files(String files) {
      this.files = files != null ? files : "";
      return this;
    }

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public BodyGenerationRequest build() {
      return new BodyGenerationRequest(
          jobId, deliveryId, sha, repoFullName, branch, baseBranch,
          title, author, date, commits, files, tenantId
      );
    }
  }
}
