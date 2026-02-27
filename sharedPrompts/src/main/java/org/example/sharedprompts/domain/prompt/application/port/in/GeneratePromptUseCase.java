package org.example.sharedprompts.domain.prompt.application.port.in;

/**
 * 프롬프트 생성 유즈케이스 포트 — 4단계 파이프라인(Clarify → Solve → Verify → Repair)의 진입점.
 *
 * <p>Adapter(Controller)는 이 인터페이스만 의존한다.
 * domain/application 클래스를 직접 조립·침범하지 않는다.
 */
public interface GeneratePromptUseCase {

    /**
     * 프롬프트를 생성한다.
     *
     * @param command 사용자 입력 커맨드
     * @return 생성 결과 (배지 포함, 수치 지표는 내부용)
     */
    GeneratePromptResult generate(GeneratePromptCommand command);
}
