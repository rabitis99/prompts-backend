package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Service;

@Service
public class KoreanGuidelineBuilder extends PromptGuidelineBuilder{
    @Override
    protected String definePrinciples(InputRequestDto request) {
        return """
        # 행동 원칙 (필수 준수)

        다음 원칙은 반드시 지켜야 하며, 위반해서는 안 됩니다:

        1. 사용자 수준:
           - %s

        2. 전문 역할:
           - %s

        3. 품질 기준:
           - 추측, 가정, 불확실한 정보 제공 금지
           - 즉시 활용 가능한 정보만 제공

        4. 맥락 적합성:
           - 사용자 요청 범위를 벗어난 설명 금지
        """.formatted(
                request.getExperience().getGuidelineKo(),
                request.getPromptCategory().getGuidelineKo()
        );
    }


    @Override
    protected String defineWorkingStyle(InputRequestDto request) {
        return """
        # 작업 방식 (강제 규칙)

        - 커뮤니케이션 톤:
          %s

        - 표현 스타일:
          %s

        - 구성 규칙:
          모든 응답은 논리적으로 구조화되어야 하며,
          제목, 목록, 단계 구분을 적극 활용하십시오.
        """.formatted(
                request.getTone().getGuidelineKo(),
                request.getStyle().getGuidelineKo()
        );
    }

    @Override
    protected String defineResponseGuidelines(InputRequestDto request) {
        return """
        # 응답 가이드라인 (위반 불가)

        - 명확성:
          모호한 표현 금지

        - 실용성:
          실전 적용 우선

        - 형식 제한:
          인사말, 불필요한 서론 금지
        """;
    }
}
