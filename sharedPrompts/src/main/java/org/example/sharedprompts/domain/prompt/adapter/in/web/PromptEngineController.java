package org.example.sharedprompts.domain.prompt.adapter.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptCommand;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptResult;
import org.example.sharedprompts.domain.prompt.application.port.in.GeneratePromptUseCase;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import java.util.concurrent.Callable;

/**
 * 정확도 중심 프롬프트 생성 엔진 컨트롤러 (v2).
 *
 * <p><b>설계 규칙:</b>
 * <ul>
 *   <li>이 컨트롤러는 {@link GeneratePromptUseCase}만 호출한다.</li>
 *   <li>domain/application 클래스를 직접 조립·침범하지 않는다.</li>
 *   <li>응답에는 수치 지표 없이 배지만 포함된다.</li>
 * </ul>
 *
 * <p>기존 {@code POST /prompts} 엔드포인트는 v1으로 유지되며 별도 컨트롤러(PromptController)가 담당한다.
 * 새 클라이언트는 이 엔드포인트({@code POST /api/v2/prompts/generate})를 사용한다.
 */
@RestController
@RequestMapping("/api/v2/prompts")
@RequiredArgsConstructor
public class PromptEngineController {

    private static final long ASYNC_TIMEOUT_MS = 60_000L;

    private final GeneratePromptUseCase generatePromptUseCase;
    private final BadgeResponseAssembler badgeResponseAssembler;

    /**
     * 프롬프트 생성 (정확도 엔진 v2).
     * 4단계 파이프라인(Clarify → Solve → Verify → Repair)을 실행하고
     * 배지 형태의 품질 정보와 함께 결과를 반환한다.
     */
    @PostMapping("/generate")
    public WebAsyncTask<ResponseEntity<CustomResponse<GeneratePromptResponse>>> generate(
            @Valid @RequestBody GeneratePromptRequest request,
            @CurrentUser AuthUser authUser
    ) {
        GeneratePromptCommand command = request.toCommand(authUser.getId());

        Callable<ResponseEntity<CustomResponse<GeneratePromptResponse>>> callable = () -> {
            GeneratePromptResult result = generatePromptUseCase.generate(command);
            GeneratePromptResponse response = badgeResponseAssembler.assemble(result);
            return CustomResponseHelper.created(response);
        };

        WebAsyncTask<ResponseEntity<CustomResponse<GeneratePromptResponse>>> asyncTask =
                new WebAsyncTask<>(ASYNC_TIMEOUT_MS, callable);

        asyncTask.onTimeout(() -> {
            ApiException timeoutEx = new ApiException(
                    ErrorCode.AI_GENERATION_FAILED,
                    "프롬프트 생성이 시간 초과되었습니다. 잠시 후 다시 시도해주세요.");
            return ResponseEntity.status(timeoutEx.getErrorCode().getHttpStatus())
                    .body(CustomResponse.fail(timeoutEx));
        });

        return asyncTask;
    }
}
