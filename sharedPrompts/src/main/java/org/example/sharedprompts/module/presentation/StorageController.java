package org.example.sharedprompts.module.presentation;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.module.domain.production.application.storage.StorageCommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Storage Controller
 * Presigned URL 생성을 담당합니다.
 * 실제 파일 전송은 서버를 거치지 않고 클라이언트에서 직접 S3로 업로드/다운로드합니다.
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageCommandService storageCommandService;

    /**
     * 업로드용 Presigned URL 생성
     * POST /storage/upload-url
     */
    @PostMapping("/upload-url")
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generateUploadUrl(
            @Valid @RequestBody UploadUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Upload presigned URL requested - userId: {}, fileName: {}, contentType: {}",
                authUser.getId(), request.fileName(), request.contentType());

        String presignedUrl = storageCommandService.generateUploadPresignedUrl(
                authUser.getId(),
                request.jobId(),
                request.fileName(),
                request.contentType()
        );

        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 다운로드용 Presigned URL 생성
     * POST /storage/download-url
     * 아티팩트 ID를 통해 소유권을 검증한 후 Presigned URL을 생성합니다.
     */
    @PostMapping("/download-url")
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generateDownloadUrl(
            @Valid @RequestBody DownloadUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Download presigned URL requested - userId: {}, artifactId: {}",
                authUser.getId(), request.artifactId());

        String presignedUrl = storageCommandService.generateDownloadPresignedUrl(
                request.artifactId(), authUser.getId());
        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 미리보기용 Presigned URL 생성
     * POST /storage/preview-url
     * 아티팩트 ID를 통해 소유권을 검증한 후 Presigned URL을 생성합니다.
     */
    @PostMapping("/preview-url")
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generatePreviewUrl(
            @Valid @RequestBody PreviewUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Preview presigned URL requested - userId: {}, artifactId: {}",
                authUser.getId(), request.artifactId());

        String presignedUrl = storageCommandService.generatePreviewPresignedUrl(
                request.artifactId(), authUser.getId());
        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 업로드용 Presigned URL 생성 요청 DTO
     */
    public record UploadUrlRequest(
            @JsonProperty("jobId")
            @NotBlank(message = "jobId is required")
            String jobId,

            @JsonProperty("fileName")
            @NotBlank(message = "fileName is required")
            String fileName,

            @JsonProperty("contentType")
            @NotBlank(message = "contentType is required")
            String contentType
    ) {
    }

    /**
     * 다운로드용 Presigned URL 생성 요청 DTO
     */
    public record DownloadUrlRequest(
            @JsonProperty("artifactId")
            @NotNull(message = "artifactId is required")
            Long artifactId
    ) {
    }

    /**
     * 미리보기용 Presigned URL 생성 요청 DTO
     */
    public record PreviewUrlRequest(
            @JsonProperty("artifactId")
            @NotNull(message = "artifactId is required")
            Long artifactId
    ) {
    }

    @Data
    public static class PresignedUrlResponse {
        private String presignedUrl;

        public PresignedUrlResponse(String presignedUrl) {
            this.presignedUrl = presignedUrl;
        }
    }
}

