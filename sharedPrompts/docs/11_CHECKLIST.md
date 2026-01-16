# 체크리스트

## 우선순위 정책

1. **보안** 🔴 (최우선)
2. **성능 개선** 🟠
3. **문서화** 🟡
4. **테스트** 🟢

---

## 1. 보안 🔴 **최우선**

### 1.1 Rate Limiting
- [ ] Resilience4j 또는 Bucket4j 도입
- [ ] 로그인 Rate Limit (5회/분)
- [ ] 회원가입 Rate Limit (3회/분)
- [ ] 프롬프트 생성 Rate Limit (10회/분)
- [ ] Redis 기반 분산 Rate Limiting

### 1.2 입력값 Sanitization
- [ ] OWASP Java HTML Sanitizer 도입
- [ ] 프롬프트 내용 HTML 태그 필터링
- [ ] 댓글 내용 HTML 태그 필터링
- [ ] 사용자 입력값 검증 강화

### 1.3 보안 헤더 추가
- [ ] X-Frame-Options: DENY
- [ ] X-Content-Type-Options: nosniff
- [ ] Strict-Transport-Security
- [ ] Content-Security-Policy 설정

### 1.4 인증/인가 로깅
- [ ] 실패한 로그인 시도 로깅
- [ ] 권한 부족 접근 시도 로깅
- [ ] 비정상적인 패턴 탐지

### 1.5 토큰 보안 강화
- [ ] Refresh Token Rotation 구현
- [ ] HttpOnly Cookie 고려
- [ ] 토큰 탈취 감지 메커니즘

---

## 2. 성능 개선 🟠

### 2.1 캐싱 전략 확대
- [ ] @Cacheable 어노테이션 도입
- [ ] 프롬프트 목록 캐싱
- [ ] 사용자 정보 캐싱
- [ ] TTL 적절히 설정 (인기 프롬프트 5분, 일반 1분, 사용자 10분)

### 2.2 Connection Pool 최적화
- [ ] HikariCP 설정 튜닝
- [ ] maximum-pool-size 설정
- [ ] minimum-idle 설정
- [ ] connection-timeout 설정
- [ ] DB 커넥션 모니터링

### 2.3 DB 인덱스 최적화
- [ ] 쿼리 성능 분석 (EXPLAIN)
- [ ] 복합 인덱스 추가
- [ ] 불필요한 인덱스 제거

### 2.4 비동기 처리 범위 확대
- [ ] 이메일 발송 비동기화
- [ ] 알림 전송 비동기화
- [ ] @Async 또는 CompletableFuture 활용
- [ ] 스레드 풀 크기 설정

### 2.5 쿼리 결과 최적화
- [ ] DTO Projection 활용
- [ ] @EntityGraph 활용 범위 확대

---

## 3. 문서화 🟡

### 3.1 README.md 보완
- [ ] 프로젝트 구조 설명
- [ ] 실행 방법 (로컬 개발 환경 설정)
- [ ] 테스트 방법
- [ ] API 엔드포인트 요약

### 3.2 API 문서화
- [ ] Swagger/Spring REST Docs 도입
- [ ] 주요 API 엔드포인트 문서화
- [ ] 요청/응답 예시 포함

### 3.3 JavaDoc 추가
- [ ] 모든 public 클래스에 JavaDoc
- [ ] 모든 public 메서드에 JavaDoc
- [ ] @param, @return, @throws 태그 활용

### 3.4 ADR (Architecture Decision Records)
- [ ] 주요 설계 결정 사항 문서화
- [ ] 결정 배경 및 대안 검토 내용 기록

---

## 4. 테스트 🟢

### 4.1 단위 테스트 작성
- [ ] Service 계층 테스트 (80% 이상 목표)
- [ ] Mockito 활용한 의존성 모킹
- [ ] Given-When-Then 패턴 적용
- [ ] 테스트 클래스명 규칙 ({ClassName}Test)
- [ ] 테스트 메서드명 규칙 (메서드명_조건_예상결과)

### 4.2 통합 테스트 작성
- [ ] Repository 테스트 (@DataJpaTest)
- [ ] Controller 테스트 (@WebMvcTest)
- [ ] TestContainers 또는 H2 인메모리 DB 사용

### 4.3 테스트 커버리지 측정
- [ ] JaCoCo 도구 도입
- [ ] CI/CD 파이프라인에서 커버리지 측정
- [ ] 커버리지 임계값 설정 (80% 미만 시 빌드 실패)

### 4.4 테스트 데이터 관리
- [ ] 테스트 픽스처 생성 유틸리티 (Builder 패턴)
- [ ] 테스트 데이터베이스 초기화 전략

---

## 5. 신규 기능 개발 🟡

### 5.1 관리자 페이지
- [ ] 관리자 권한 인증/인가
- [ ] 사용자 관리 (조회, 차단, 권한 변경)
- [ ] 프롬프트 관리 (조회, 삭제, 공개/비공개 전환)
- [ ] 신고 처리 (신고된 콘텐츠 검토, 조치)
- [ ] 통계 대시보드 연동

### 5.2 통계 기능
- [ ] 사용자 통계 (가입 수, 활성 사용자, 신규 가입자 추이)
- [ ] 프롬프트 통계 (생성 수, 조회 수, 좋아요 수, 인기 태그)
- [ ] AI 호출 통계 (호출 횟수, 성공률, 평균 응답 시간)
- [ ] 통계 데이터 집계 (스케줄러 또는 이벤트 기반)
- [ ] 통계 데이터 캐싱 (Redis)
- [ ] 통계 API 엔드포인트

### 5.3 알림 기능
- [x] 알림 이벤트 발행 (Spring Events)
- [x] 알림 저장소 (DB)
- [x] 댓글 알림
- [x] 좋아요 알림
- [x] 실시간 알림 (WebSocket 또는 SSE)
- [x] 알림 읽음 처리

### 5.4 신고 기능
- [x] 신고 엔티티 및 저장소
- [x] 콘텐츠 신고 (프롬프트, 댓글)
- [x] 신고 사유 선택
- [x] 신고 API 엔드포인트
- [x] 신고 처리 워크플로우
- [ ] 관리자 신고 처리 UI 연동 (백엔드 API 완료, 프론트엔드 연동 필요)

---

## 6. 중장기 개선 🟢

### 6.1 아키텍처 고도화
- [ ] DDD 패턴 적용 심화 (Aggregate Root, Value Object)
- [ ] 도메인 이벤트 패턴 고도화
- [ ] Port and Adapter 패턴 적용 검토

### 6.2 코드 품질 개선
- [ ] 코드 복잡도 감소
- [ ] 매직 넘버/문자열 상수화
- [ ] 일관된 코딩 컨벤션 (Checkstyle, SpotBugs)
- [ ] 불변성 강화

### 6.3 예외 처리 개선
- [ ] 에러 코드 체계 고도화
- [ ] 예외 로깅 강화
- [ ] 예외 계층 구조 명확화
- [ ] 예외 처리 테스트

### 6.4 운영 도구 도입
- [ ] 로깅 중앙화 (ELK Stack)
- [ ] 모니터링 대시보드 (Grafana + Prometheus)
- [ ] CI/CD 파이프라인 구축
- [ ] 서킷브레이커 알림 연계

---

## 완료된 항목 (제거 대상)

다음 항목들은 이미 구현이 완료되어 로드맵에서 제거되었습니다:

- ✅ **AI 호출 안정성**: CircuitBreaker, Fallback, Metrics 구현 완료
- ✅ **서비스 인터페이스 일관성**: 모든 주요 Service에 인터페이스 분리 완료
- ✅ **Controller Mono 노출 제거**: Facade 계층 도입으로 일관성 확보

[← 목차로 돌아가기](../CODE_IMPROVEMENT_GUIDE.md)
