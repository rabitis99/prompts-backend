package org.example.sharedprompts.module.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
                authUser.getId(), request.getFileName(), request.getContentType());

        String presignedUrl = storageCommandService.generateUploadPresignedUrl(
                authUser.getId(),
                request.getJobId(),
                request.getFileName(),
                request.getContentType()
        );

        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 다운로드용 Presigned URL 생성
     * POST /storage/download-url
     */
    @PostMapping("/download-url")
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generateDownloadUrl(
            @Valid @RequestBody DownloadUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Download presigned URL requested - userId: {}, s3Key: {}",
                authUser.getId(), request.getS3Key());

        String presignedUrl = storageCommandService.generateDownloadPresignedUrl(request.getS3Key());
        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    /**
     * 미리보기용 Presigned URL 생성
     * POST /storage/preview-url
     */
    @PostMapping("/preview-url")
    public ResponseEntity<CustomResponse<PresignedUrlResponse>> generatePreviewUrl(
            @Valid @RequestBody PreviewUrlRequest request,
            @CurrentUser AuthUser authUser
    ) {
        log.info("Preview presigned URL requested - userId: {}, s3Key: {}",
                authUser.getId(), request.getS3Key());

        String presignedUrl = storageCommandService.generatePreviewPresignedUrl(request.getS3Key());
        PresignedUrlResponse response = new PresignedUrlResponse(presignedUrl);
        return CustomResponseHelper.ok(response);
    }

    @Data
    public static class UploadUrlRequest {
        @NotBlank(message = "jobId is required")
        private String jobId;

        @NotBlank(message = "fileName is required")
        private String fileName;

        @NotBlank(message = "contentType is required")
        private String contentType;
    }

    @Data
    public static class DownloadUrlRequest {
        @NotBlank(message = "s3Key is required")
        private String s3Key;
    }

    @Data
    public static class PreviewUrlRequest {
        @NotBlank(message = "s3Key is required")
        private String s3Key;
    }

    @Data
    public static class PresignedUrlResponse {
        private String presignedUrl;

        public PresignedUrlResponse(String presignedUrl) {
            this.presignedUrl = presignedUrl;
        }
    }
}

