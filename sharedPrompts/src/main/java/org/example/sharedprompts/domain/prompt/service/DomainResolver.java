package org.example.sharedprompts.domain.prompt.service;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.prompt.enums.TaskDomain;
import org.example.sharedprompts.domain.prompt.enums.guideline.DomainResolution;
import org.example.sharedprompts.dto.prompt.request.InputRequestDto;

import java.util.Optional;

/**
 * 도메인 결정 유틸리티 클래스
 * <p>PromptGenerator와 PromptGuidelineBuilder에서 공통으로 사용하는 도메인 결정 로직을 중앙화한다.</p>
 */
@Slf4j
public class DomainResolver {

    /**
     * 도메인 결정 우선순위:
     * 1순위 — ActionType이 명시적으로 지정한 도메인 (GENERAL이 아닌 경우)
     * 2순위 — PromptCategory의 기본 도메인
     * 3순위 — ActionType이 명시적 GENERAL (의도적 — 로그 불필요)
     * 폴백  — 진짜 미매핑 → GENERAL + 경고 로그 + isFallback=true
     *
     * @param request 입력 요청 DTO
     * @return 도메인 해결 결과 (도메인 + 폴백 여부)
     */
    public static DomainResolution resolveDomain(InputRequestDto request) {
        Optional<TaskDomain> fromAction = request.getActionType().getTaskDomain();

        if (fromAction.isPresent() && fromAction.get() != TaskDomain.GENERAL) {
            return new DomainResolution(fromAction.get(), false);
        }

        TaskDomain fromCategory = request.getPromptCategory().getDefaultDomain();
        if (fromCategory != TaskDomain.GENERAL) {
            return new DomainResolution(fromCategory, false);
        }

        if (fromAction.isPresent()) {
            return new DomainResolution(TaskDomain.GENERAL, false); // 의도적
        }

        log.warn("Unmapped ActionType: {} (category={}) — GENERAL fallback. 매핑 추가 필요.",
                 request.getActionType().name(), request.getPromptCategory());
        return new DomainResolution(TaskDomain.GENERAL, true); // 미매핑 폴백
    }

    /**
     * 도메인만 반환하는 간단한 버전 (PromptGenerator용)
     *
     * @param request 입력 요청 DTO
     * @return 결정된 도메인
     */
    public static TaskDomain resolveDomainSimple(InputRequestDto request) {
        return resolveDomain(request).domain();
    }
}

