package org.example.sharedprompts.module.domain.production.service.production;

import java.util.List;
import java.util.Map;

public interface ArtifactAccessService {
    /**
     * 이미지 미리보기용 Presigned URL 생성
     */
    String generatePreviewUrl(String filePath);

    /**
     * P1-4: 여러 파일 경로에 대한 Presigned URL을 일괄 생성
     * Redis 캐시를 효율적으로 활용하여 목록 조회 시 성능 개선
     * 
     * @param filePaths 파일 경로 목록
     * @return 파일 경로를 키로 하는 Presigned URL 맵
     */
    default Map<String, String> generatePreviewUrls(List<String> filePaths) {
        return filePaths.stream()
                .collect(java.util.stream.Collectors.toMap(
                        path -> path,
                        this::generatePreviewUrl,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * 파일 다운로드용 Presigned URL 생성
     */
    String generateDownloadUrl(String filePath);

    /**
     * CDN URL 생성 (CDN 비활성화 시 null 반환)
     */
    default String generateCdnUrl(String filePath) {
        return null;
    }
}
