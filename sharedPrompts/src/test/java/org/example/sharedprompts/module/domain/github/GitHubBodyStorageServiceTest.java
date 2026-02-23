package org.example.sharedprompts.module.domain.github;

import org.example.sharedprompts.module.domain.production.application.storage.StorageFacade;
import org.example.sharedprompts.module.domain.production.infra.storage.S3KeyGenerator;
import org.example.sharedprompts.module.dto.request.github.GitHubBodyRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubBodyStorageService 키 생성 규칙 및 tryGetExisting 테스트")
class GitHubBodyStorageServiceTest {

    @Mock
    private StorageFacade storageFacade;
    @Mock
    private S3KeyGenerator keyGenerator;

    @Test
    @DisplayName("tryGetExistingKeys: 둘 다 존재하면 StoredKeys 반환")
    void tryGetExistingKeys_bothExist_returnsKeys() {
        when(keyGenerator.generateKey(eq("github"), eq(0L), eq("job-1"), eq("issue-job-1.md")))
                .thenReturn("production/github/0/job-1/issue-job-1.md");
        when(keyGenerator.generateKey(eq("github"), eq(0L), eq("job-1"), eq("pr-job-1.md")))
                .thenReturn("production/github/0/job-1/pr-job-1.md");
        when(storageFacade.exists("production/github/0/job-1/issue-job-1.md")).thenReturn(true);
        when(storageFacade.exists("production/github/0/job-1/pr-job-1.md")).thenReturn(true);

        GitHubBodyStorageService service = new GitHubBodyStorageService(storageFacade, keyGenerator);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "job-1", null, null, null, null, null, null, null, null, null, null, null);

        Optional<GitHubBodyStorageService.StoredKeys> result = service.tryGetExistingKeys(request);

        assertThat(result).isPresent();
        assertThat(result.get().storedIssueFileKey()).isEqualTo("production/github/0/job-1/issue-job-1.md");
        assertThat(result.get().storedPrFileKey()).isEqualTo("production/github/0/job-1/pr-job-1.md");
    }

    @Test
    @DisplayName("저장 키 규칙: prefix/tenantId/0/jobId/fileName 형식")
    void keyFormat_prefix_tenantId_zero_jobId_fileName() {
        when(keyGenerator.generateKey(eq("github"), eq(0L), eq("abc123"), eq("issue-abc123.md")))
                .thenReturn("production/github/0/abc123/issue-abc123.md");
        when(keyGenerator.generateKey(eq("github"), eq(0L), eq("abc123"), eq("pr-abc123.md")))
                .thenReturn("production/github/0/abc123/pr-abc123.md");
        when(storageFacade.exists(anyString())).thenReturn(true);

        GitHubBodyStorageService service = new GitHubBodyStorageService(storageFacade, keyGenerator);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "abc123", null, null, null, null, null, null, null, null, null, null, "github");

        Optional<GitHubBodyStorageService.StoredKeys> result = service.tryGetExistingKeys(request);

        assertThat(result).isPresent();
        assertThat(result.get().storedIssueFileKey()).startsWith("production/").contains("github/0/abc123/").endsWith("issue-abc123.md");
        assertThat(result.get().storedPrFileKey()).startsWith("production/").contains("github/0/abc123/").endsWith("pr-abc123.md");
    }

    @Test
    @DisplayName("PR 저장 실패 시 보상: 이미 저장된 issue 파일 delete 호출")
    void saveBothAsMarkdown_prFails_compensationDeleteIssue() {
        when(storageFacade.upload(anyString(), eq("github"), eq(0L), eq("j1"), eq("issue-j1.md")))
                .thenReturn("production/github/0/j1/issue-j1.md");
        when(storageFacade.upload(anyString(), eq("github"), eq(0L), eq("j1"), eq("pr-j1.md")))
                .thenThrow(new RuntimeException("S3 pr upload failed"));

        GitHubBodyStorageService service = new GitHubBodyStorageService(storageFacade, keyGenerator);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "j1", null, null, null, null, null, null, null, null, null, null, "github");

        assertThatThrownBy(() -> service.saveBothAsMarkdown("issue", "pr", request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 pr upload failed");

        verify(storageFacade).delete("production/github/0/j1/issue-j1.md");
    }

    @Test
    @DisplayName("tryGetExistingKeys: 하나라도 없으면 empty")
    void tryGetExistingKeys_oneMissing_returnsEmpty() {
        when(keyGenerator.generateKey(anyString(), eq(0L), anyString(), anyString()))
                .thenReturn("production/github/0/j1/issue-j1.md")
                .thenReturn("production/github/0/j1/pr-j1.md");
        when(storageFacade.exists("production/github/0/j1/issue-j1.md")).thenReturn(true);
        when(storageFacade.exists("production/github/0/j1/pr-j1.md")).thenReturn(false);

        GitHubBodyStorageService service = new GitHubBodyStorageService(storageFacade, keyGenerator);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "j1", null, null, null, null, null, null, null, null, null, null, null);

        assertThat(service.tryGetExistingKeys(request)).isEmpty();
    }

    @Test
    @DisplayName("StorageFacade null이면 tryGetExistingKeys empty")
    void tryGetExistingKeys_noFacade_returnsEmpty() {
        GitHubBodyStorageService service = new GitHubBodyStorageService(null, keyGenerator);
        GitHubBodyRequestDto request = new GitHubBodyRequestDto(
                "j1", null, null, null, null, null, null, null, null, null, null, null);
        assertThat(service.tryGetExistingKeys(request)).isEmpty();
    }
}
