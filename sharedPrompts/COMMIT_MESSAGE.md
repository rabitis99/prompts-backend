refactor(production): P1 중기 개선 사항 완료 및 코드 구조 정리

## 주요 변경사항

### 1. P1 중기 개선 사항 완료

#### PDF 변환 개선
- `PdfFormatConverter`: Markdown → HTML → PDF 파이프라인 통합
  - Markdown 콘텐츠 자동 감지 및 변환
  - FlexMark를 사용한 Markdown 파싱 (제목, 리스트, 코드 블록, 테이블 등 지원)
  - OpenHTMLToPDF를 사용한 HTML → PDF 변환
  - CSS 스타일링 지원
  - 일반 텍스트도 HTML로 감싸서 PDF 변환 지원

#### StorageStrategy 인터페이스 확장
- `read()`, `exists()`, `delete()`, `generateAccessUrl()` 메서드 구현
- `S3StorageStrategy`, `LocalStorageStrategy` 모두 지원

#### ArtifactHandler 전략 패턴
- `ArtifactHandlerRegistry`로 타입별 Handler 관리
- `TextArtifactHandler`, `FileArtifactHandler`, `ImageArtifactHandler` 구현

### 2. 코드 구조 정리

#### 폴더 구조 개선
- PDF 관련 코드를 `format/pdf/` 서브폴더로 분리
  - `HtmlToPdfConverter.java`
  - `OpenHtmlToPdfConverterImpl.java`
  - `PdfCssProvider.java`
  - `DefaultPdfCssProvider.java`

- Markdown 관련 코드를 `format/markdown/` 서브폴더로 분리
  - `MarkdownToHtmlConverter.java`
  - `FlexMarkMarkdownToHtmlConverter.java`

#### 주석 최소화
- 불필요한 JavaDoc 주석 제거
- 코드 자체로 의도가 명확하도록 정리
- `StorageStrategy`, `ArtifactHandler` 인터페이스 주석 제거

### 3. 의존성 업데이트
- OpenHTMLToPDF 버전 업그레이드: `1.1.24` → `1.1.37`
- 패키지 경로 변경: `com.openhtmltopdf` → `io.github.openhtmltopdf`

## 변경된 파일

### 신규 생성
- `format/pdf/HtmlToPdfConverter.java`
- `format/pdf/OpenHtmlToPdfConverterImpl.java`
- `format/pdf/PdfCssProvider.java`
- `format/pdf/DefaultPdfCssProvider.java`
- `format/markdown/MarkdownToHtmlConverter.java`
- `format/markdown/FlexMarkMarkdownToHtmlConverter.java`

### 수정
- `PdfFormatConverter.java` (Markdown → HTML → PDF 파이프라인 통합)
- `StorageStrategy.java` (주석 제거)
- `ArtifactHandler.java` (주석 제거)
- `LocalStorageStrategy.java` (주석 제거)
- `build.gradle` (OpenHTMLToPDF 의존성 업데이트)

### 삭제
- `format/HtmlToPdfConverter.java` (pdf 서브폴더로 이동)
- `format/OpenHtmlToPdfConverterImpl.java` (pdf 서브폴더로 이동)
- `format/PdfCssProvider.java` (pdf 서브폴더로 이동)
- `format/DefaultPdfCssProvider.java` (pdf 서브폴더로 이동)
- `format/MarkdownToHtmlConverter.java` (markdown 서브폴더로 이동)
- `format/FlexMarkMarkdownToHtmlConverter.java` (markdown 서브폴더로 이동)

## 효과
- 코드 구조 명확화: PDF/Markdown 관련 코드가 논리적으로 그룹화됨
- 확장성 향상: 새로운 변환 타입 추가 시 서브폴더 구조로 관리 용이
- 코드 가독성 개선: 불필요한 주석 제거로 핵심 로직에 집중
- PDF 변환 품질 향상: Markdown 구조 완벽 지원 및 CSS 스타일링 적용
