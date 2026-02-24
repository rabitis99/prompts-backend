package org.example.sharedprompts.module.github.port.out;

import java.time.Duration;

/**
 * S3 스토리지 포트.
 * 마크다운 파일의 저장, 다운로드, 프리사인드 URL 생성을 추상화.
 *
 * 구현 책임:
 * - {@link org.example.sharedprompts.module.github.adapter.out.storage.S3StorageAdapter}
 *
 * LSP 계약:
 * - 저장 실패: exception 발생
 * - 다운로드 미존재: "" (empty string) 반환, null 불가
 * - 프리사인드 URL 실패: exception 발생
 */
public interface StoragePort {

  /**
   * S3에 마크다운 파일 저장.
   *
   * @param s3Key S3 object key (절대 경로)
   * @param content 파일 내용 (마크다운)
   * @return 저장된 S3 key
   * @throws IllegalArgumentException key/content null 또는 blank
   * @throws StorageException S3 업로드 실패
   */
  String saveMarkdown(String s3Key, String content);

  /**
   * S3에서 마크다운 파일 다운로드.
   *
   * @param s3Key S3 object key
   * @return 파일 내용 (UTF-8), 미존재시 "" (빈 문자열)
   * @throws StorageException S3 다운로드 실패
   */
  String downloadBody(String s3Key);

  /**
   * S3 프리사인드 다운로드 URL 생성.
   *
   * @param s3Key S3 object key
   * @param ttl URL 유효 시간
   * @return 비공개 프리사인드 URL
   * @throws IllegalArgumentException key null 또는 ttl null/negative
   * @throws StorageException URL 생성 실패
   */
  String generatePresignedUrl(String s3Key, Duration ttl);

  /**
   * S3 object 존재 여부 확인.
   *
   * @param s3Key S3 object key
   * @return true if exists
   * @throws StorageException S3 조회 실패
   */
  boolean exists(String s3Key);

  /**
   * S3 object 삭제.
   *
   * @param s3Key S3 object key
   * @throws IllegalArgumentException key null
   * @throws StorageException S3 삭제 실패
   */
  void delete(String s3Key);
}
