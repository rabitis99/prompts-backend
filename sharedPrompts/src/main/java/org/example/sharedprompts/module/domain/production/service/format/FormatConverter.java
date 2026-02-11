package org.example.sharedprompts.module.domain.production.service.format;

/**
 * 렌더링된 텍스트 콘텐츠를 특정 파일 포맷으로 변환하는 인터페이스
 */
public interface FormatConverter {

    /**
     * 텍스트 콘텐츠를 대상 포맷의 바이트 배열로 변환
     *
     * @param content  렌더링된 텍스트 콘텐츠
     * @param fileName 출력 파일명 (확장자 없이)
     * @return 변환된 바이트 배열
     */
    byte[] convert(String content, String fileName);

    /**
     * 이 포맷의 MIME Content-Type
     */
    String getContentType();

    /**
     * 이 포맷의 파일 확장자 (점 포함, 예: ".xlsx")
     */
    String getFileExtension();

    /**
     * 주어진 포맷 문자열을 지원하는지 확인
     */
    boolean supports(String format);
}
