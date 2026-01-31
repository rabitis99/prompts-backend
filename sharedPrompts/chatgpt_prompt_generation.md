# ChatGPT 프롬프트 생성 요청

## 목표
각 카테고리별로 실용적이고 다양한 AI 프롬프트를 생성하여 데이터베이스에 저장할 예정입니다.
총 15개 카테고리 × 약 667개씩 = 약 10,000개의 프롬프트를 생성해야 합니다.

## 출력 형식
다음 JSON 형식으로 출력해주세요:
```json
[
  {
    "title": "프롬프트 제목 (최대 200자)",
    "description": "프롬프트 설명 (최대 5000자)",
    "content": "실제 사용할 프롬프트 내용 (TEXT, 실용적이고 구체적)",
    "category": "카테고리명"
  }
]
```

## 카테고리별 가이드라인

### 1. PRODUCTIVITY (생산성)
- **한국어 설명**: 업무 효율 향상, 시간 관리, 자동화 등 생산성을 높이는 전략과 아이디어를 제공합니다.
- **영어 가이드라인**: Focus on improving productivity through workflow optimization, time management, and automation strategies.
- **생성 예시**: 
  - 업무 자동화, 시간 관리, 효율성 향상, 업무 프로세스 개선
  - 일정 관리, 우선순위 설정, 집중력 향상, 습관 형성
  - 업무 도구 활용, 협업 효율화, 스트레스 관리

### 2. DEVELOPMENT (개발)
- **한국어 설명**: 소프트웨어 개발 관련 지식, 기술 스택, 프로젝트 구조와 최적화 방법을 다룹니다.
- **영어 가이드라인**: Provide expertise on software development, including technology stacks, system architecture, and optimization techniques.
- **생성 예시**:
  - 아키텍처 설계, 기술 스택 선택, 성능 최적화
  - 개발 방법론, 코드 리뷰, 테스트 전략
  - DevOps, CI/CD, 인프라 관리

### 3. CODING (코딩)
- **한국어 설명**: 깨끗하고 유지보수 가능한 코드를 작성하며, 모범 사례와 디자인 패턴에 정통합니다. 복잡한 기술적 문제를 단순하고 우아하게 해결합니다.
- **영어 가이드라인**: Write clean, maintainable code while applying best practices and design patterns to solve complex technical problems elegantly.
- **생성 예시**:
  - 코드 리팩토링, 디자인 패턴 적용, 알고리즘 구현
  - 버그 수정, 코드 최적화, 가독성 향상
  - 특정 언어/프레임워크별 코딩 가이드

### 4. PROGRAMMING (프로그래밍)
- **한국어 설명**: 프로그래밍 언어, 알고리즘, 자료구조 등 개발 전반에 대한 실용적 지식을 제공합니다.
- **영어 가이드라인**: Provide practical knowledge across programming languages, algorithms, and data structures.
- **생성 예시**:
  - 프로그래밍 언어 학습, 알고리즘 문제 해결
  - 자료구조 이해, 프로그래밍 개념 설명
  - 코드 예제 생성, 문법 질문

### 5. ANALYSIS (분석)
- **한국어 설명**: 데이터 분석, 통계, 인사이트 도출 등 정보 해석과 문제 해결을 위한 분석적 접근을 다룹니다.
- **영어 가이드라인**: Apply analytical approaches to data analysis, statistics, and insight extraction for problem solving.
- **생성 예시**:
  - 데이터 분석, 통계 해석, 트렌드 분석
  - 비즈니스 인사이트, 시장 분석, 성과 평가
  - 문제 해결 접근법, 의사결정 지원

### 6. MARKETING (마케팅)
- **한국어 설명**: 마케팅 전략, 브랜딩, 광고, 시장 조사 등을 포함하여 고객과 비즈니스 성장을 지원합니다.
- **영어 가이드라인**: Support business growth through marketing strategies, branding, advertising, and market research.
- **생성 예시**:
  - 마케팅 전략 수립, 브랜드 포지셔닝, 광고 카피 작성
  - 타겟 고객 분석, 경쟁사 분석, 마케팅 캠페인 기획
  - SEO, 디지털 마케팅, 고객 유지 전략

### 7. CONTENT (콘텐츠 제작)
- **한국어 설명**: 블로그, SNS, 영상 등 다양한 매체에 맞는 콘텐츠 기획과 작성법, 아이디어 개발을 안내합니다.
- **영어 가이드라인**: Guide content planning, writing, and idea generation tailored to blogs, social media, and video platforms.
- **생성 예시**:
  - 블로그 포스트 작성, SNS 콘텐츠 기획, 영상 스크립트 작성
  - 콘텐츠 아이디어 생성, 에디토리얼 캘린더, 콘텐츠 전략
  - 매체별 최적화, 바이럴 콘텐츠, 스토리텔링

### 8. CREATIVE (창작)
- **한국어 설명**: 예술적, 창의적 아이디어와 창작물 개발에 관한 조언과 실습 가이드를 제공합니다.
- **영어 가이드라인**: Provide guidance and practical advice for developing creative and artistic ideas.
- **생성 예시**:
  - 창의적 아이디어 발상, 스토리 창작, 캐릭터 디자인
  - 예술 작품 기획, 창작 과정 가이드, 영감 얻기
  - 다양한 예술 분야별 창작 지원

### 9. STUDY (학습)
- **한국어 설명**: 효율적인 학습 방법, 자료 정리, 이해력 향상 및 학습 계획 수립을 돕습니다.
- **영어 가이드라인**: Help design effective study methods, learning plans, and knowledge organization.
- **생성 예시**:
  - 학습 방법론, 암기 전략, 노트 정리법
  - 학습 계획 수립, 집중력 향상, 복습 전략
  - 과목별 학습 가이드, 시험 대비, 지식 정리

### 10. EDUCATION (교육)
- **한국어 설명**: 교육 자료 설계, 교수법, 학습자 맞춤형 교육 전략을 제공합니다.
- **영어 가이드라인**: Design educational materials and teaching strategies tailored to learners.
- **생성 예시**:
  - 교육 커리큘럼 설계, 교수법 개발, 학습자 맞춤 교육
  - 교육 자료 제작, 평가 방법, 학습 동기 부여
  - 연령대별 교육 전략, 주제별 교육 가이드

### 11. RESEARCH (연구)
- **한국어 설명**: 과학적, 학문적 연구 방법, 실험 설계, 논문 작성과 데이터 해석을 지원합니다.
- **영어 가이드라인**: Support academic and scientific research including methodology, experiment design, and paper writing.
- **생성 예시**:
  - 연구 방법론, 실험 설계, 논문 작성
  - 데이터 수집 및 분석, 문헌 조사, 연구 계획
  - 학술적 글쓰기, 연구 결과 해석, 논문 구조화

### 12. BUSINESS (비즈니스)
- **한국어 설명**: 기획서 작성, 보고서, 사업 전략, 프로젝트 관리 등 실무 비즈니스 활동을 지원합니다.
- **영어 가이드라인**: Assist with business activities such as proposals, reports, strategy planning, and project management.
- **생성 예시**:
  - 비즈니스 기획서 작성, 보고서 작성, 사업 계획 수립
  - 프로젝트 관리, 리스크 관리, 의사결정 지원
  - 협상 전략, 프레젠테이션, 경영 전략

### 13. DESIGN (디자인)
- **한국어 설명**: UI/UX, 그래픽 디자인, 제품 디자인 등 사용자 경험을 고려한 설계를 다룹니다.
- **영어 가이드라인**: Focus on user-centered design including UI/UX, graphic, and product design.
- **생성 예시**:
  - UI/UX 디자인, 사용자 경험 개선, 인터페이스 설계
  - 그래픽 디자인, 브랜드 아이덴티티, 시각적 커뮤니케이션
  - 제품 디자인, 사용성 테스트, 디자인 시스템

### 14. WRITING (글쓰기)
- **한국어 설명**: 다양한 형식의 글 작성과 효과적인 메시지 전달 방법을 안내합니다.
- **영어 가이드라인**: Guide writing across formats with an emphasis on clarity and persuasive messaging.
- **생성 예시**:
  - 에세이 작성, 논설문 작성, 보고서 작성
  - 문체 개선, 논리적 글쓰기, 설득력 있는 글쓰기
  - 형식별 글쓰기 가이드, 문장 다듬기, 스토리텔링

### 15. ETC (기타)
- **한국어 설명**: 위 카테고리에 속하지 않는 다양한 주제와 아이디어를 포괄적으로 다룹니다.
- **영어 가이드라인**: Handle a wide range of topics that do not fall into predefined categories.
- **생성 예시**:
  - 일상 생활, 취미, 건강, 여행, 요리 등 다양한 주제
  - 개인 발전, 관계, 심리, 철학 등

## 생성 규칙

1. **다양성**: 각 카테고리 내에서 다양한 주제와 접근 방식을 포함하세요.
2. **실용성**: 실제로 사용할 수 있는 구체적이고 실행 가능한 프롬프트를 생성하세요.
3. **품질**: 제목은 명확하고, 설명은 이해하기 쉽게, 내용은 구체적으로 작성하세요.
4. **중복 방지**: 유사한 프롬프트가 반복되지 않도록 주의하세요.
5. **언어**: 한국어와 영어를 적절히 혼용하여 작성하되, content는 주로 영어로 작성하세요 (AI 프롬프트이므로).
6. **길이**: 
   - title: 10-50자 정도
   - description: 50-300자 정도
   - content: 20-500자 정도 (실용적이고 구체적으로)

## 요청 예시

"PRODUCTIVITY 카테고리로 50개의 프롬프트를 생성해주세요. JSON 형식으로 출력하고, 다양한 생산성 향상 주제를 다뤄주세요."

---

## 사용 방법

1. 위의 가이드라인을 ChatGPT에 복사하여 전달
2. 각 카테고리별로 순차적으로 요청 (예: "PRODUCTIVITY 카테고리로 700개 생성")
3. 생성된 JSON을 파일로 저장
4. 데이터베이스에 일괄 삽입

## 주의사항

- 한 번에 너무 많은 프롬프트를 요청하면 품질이 떨어질 수 있으므로, 카테고리별로 나눠서 요청하는 것을 권장합니다.
- 각 카테고리당 500-700개 정도씩 생성하면 총 10,000개에 근접합니다.
- 생성된 프롬프트는 검토 후 필요시 수정하여 사용하세요.

