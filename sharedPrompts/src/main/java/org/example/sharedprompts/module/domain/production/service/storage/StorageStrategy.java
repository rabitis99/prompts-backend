package org.example.sharedprompts.module.domain.production.service.storage;

/**
 * 파일 저장 전략 인터페이스
 * 
 * <p><strong>책임:</strong>
 * <ul>
 *   <li>저장 위치 전략 분리 (Local / S3 등)</li>
 *   <li>경로 생성 전략 (userId/jobId 기반)</li>
 *   <li>파일 저장 실패 시 retry 가능</li>
 *   <li>checksum 생성</li>
 * </ul>
 */
public interface StorageStrategy {

    /**
     * 텍스트 콘텐츠를 파일로 저장
     */
    String store(String content, Long userId, String jobId, String fileName);

    /**
     * 바이너리 데이터를 파일로 저장 (Excel, PDF 등)
     */
    String store(byte[] data, String contentType, Long userId, String jobId, String fileName);

    /**
     * 파일의 checksum 생성
     */
    String generateChecksum(String content);

    /**
     * 저장 전략 타입
     */
    StorageType getStorageType();
}

