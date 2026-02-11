package org.example.sharedprompts.module.domain.production.service.job.idempotencykey;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.exception.IdempotencyKeyGenerationException;
import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.hash.HashStrategy;
import org.example.sharedprompts.module.domain.production.service.job.idempotencykey.serializer.ProductionCommandSerializer;
import org.springframework.stereotype.Service;

/**
 * 멱등성 키 생성 서비스
 * 
 * <p><strong>책임:</strong>
 * <ul>
 *   <li>멱등성 키 생성 오케스트레이션</li>
 *   <li>입력 조합 및 해시 기반 키 생성</li>
 * </ul>
 * 
 * <p><strong>전략:</strong>
 * <ul>
 *   <li>결정론적 키 생성: 동일한 입력에 대해 항상 같은 키 반환</li>
 *   <li>해시 기반: promptId + userId + command + userInput 조합</li>
 *   <li>동일 요청은 동일 키로 AI는 1회만 호출됨</li>
 * </ul>
 * 
 * <p><strong>키 형식:</strong>
 * <ul>
 *   <li>production:{hash} 형식</li>
 *   <li>hash는 해시 전략에 의한 16진수 표현</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class IdempotencyKeyGenerator {

    private static final String KEY_PREFIX = "production:";

    private final ProductionCommandSerializer commandSerializer;
    private final HashStrategy hashStrategy;

    /**
     * 멱등성 키 생성
     */
    public String generate(
            Long promptId,
            Long userId,
            ProductionCommand command,
            String userInput
    ) {
        try {
            String commandJson = commandSerializer.serialize(command);
            String input = combineInput(promptId, userId, commandJson, userInput);
            String hash = hashStrategy.hash(input);
            return KEY_PREFIX + hash;
        } catch (IdempotencyKeyGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new IdempotencyKeyGenerationException(
                    "Failed to generate idempotency key: " + e.getMessage(), e);
        }
    }

    /**
     * 입력값을 조합하여 단일 문자열로 반환
     * null 안정성과 성능 최적화를 고려한 구현
     */
    private String combineInput(Long promptId, Long userId, String commandJson, String userInput) {
        return (promptId != null ? promptId : "") +
                ":" +
                (userId != null ? userId : "") +
                ":" +
                (commandJson != null ? commandJson : "") +
                ":" +
                (userInput != null ? userInput : "");
    }
}

