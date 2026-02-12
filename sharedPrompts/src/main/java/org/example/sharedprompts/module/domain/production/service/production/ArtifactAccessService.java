package org.example.sharedprompts.module.domain.production.service.production;

public interface ArtifactAccessService {
    /**
     * 이미지 미리보기용 Presigned URL 생성
     */
    String generatePreviewUrl(String filePath);

    /**
     * 파일 다운로드용 Presigned URL 생성
     */
    String generateDownloadUrl(String filePath);
}

