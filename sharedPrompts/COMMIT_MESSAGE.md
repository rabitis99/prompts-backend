fix(production): 코드 리뷰 2차 피드백 반영 - 보안/안전성 추가 개선

## 주요 변경사항

### 1. 보안 개선

#### LocalStorageStrategy
- store() 메서드 Path Traversal 취약점 수정: fileName에 `../` 포함 시 basePath 외부 쓰기 방지
- validateWithinBasePath() 헬퍼 메서드 추가 (store 전용 경로 검증)
- generateAccessUrl()에 운영 환경 사용 경고 로그 추가 (내부 파일시스템 경로 노출 방지)

### 2. 버그 수정

#### TextArtifactHandler
- createDetail() 오버라이드 추가: TEXT 아티팩트의 content 필드가 null로 남는 문제 해결
- storageStrategy.read()로 파일 내용을 읽어 INLINE_TEXT로 저장

#### ProductionArtifactService
- ZoneId.systemDefault() → ZoneId.of("Asia/Seoul")로 변경 (환경별 시간대 차이 제거)
- job.getCreatedAt() null 안전성 추가 (NPE 방지)

### 3. 코드 정리

#### ArtifactHandler 인터페이스
- createDetail()에서 사용되지 않는 JobEntity job 파라미터 제거

## 변경된 파일

- `LocalStorageStrategy.java` - store() 경로 검증 추가, generateAccessUrl() 경고 로그
- `ArtifactHandler.java` - createDetail() 미사용 job 파라미터 제거
- `TextArtifactHandler.java` - createDetail() 오버라이드로 content 설정
- `ProductionArtifactService.java` - 명시적 타임존 + null 안전성 + 호출부 파라미터 수정
