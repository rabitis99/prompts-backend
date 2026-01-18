package org.example.sharedprompts.global.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.sharedprompts.domain.audit.enums.AuditAction;
import org.example.sharedprompts.domain.audit.util.AuditLogPublisher;
import org.example.sharedprompts.global.annotation.AuditLogging;
import org.example.sharedprompts.global.aspect.util.ActorExtractor;
import org.example.sharedprompts.global.aspect.util.AuditActionResolver;
import org.example.sharedprompts.global.aspect.util.DescriptionBuilder;
import org.example.sharedprompts.global.aspect.util.ParameterExtractor;
import org.example.sharedprompts.global.util.HttpRequestUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * @AuditLogging 어노테이션이 적용된 메서드에 대해 자동으로 감사 로그를 기록하는 AOP Aspect
 * 
 * 메서드 실행 후 결과를 바탕으로 AuditEvent를 생성하여 발행합니다.
 * JPA Entity를 조회하지 않고 스냅샷 값(actorId, actorIdentifier)만 추출하여 이벤트에 포함합니다.
 * 
 * 책임:
 * - 메서드 파라미터에서 필요한 정보 추출
 * - AuditEvent 생성 및 발행
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // AdminAuthAspect 이후에 실행되도록 설정
public class AuditLogAspect {

    private final AuditLogPublisher auditLogPublisher;
    private final ActorExtractor actorExtractor;

    @Around("@annotation(auditLogging)")
    public Object logAudit(ProceedingJoinPoint joinPoint, AuditLogging auditLogging) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = signature.getMethod().getParameters();

        // 메서드 실행 전 정보 수집
        Long actorId = actorExtractor.extractActorId(auditLogging, args, parameters);
        String actorIdentifier = actorExtractor.extractActorIdentifier(args, parameters);
        Long entityId = extractEntityId(auditLogging.entityIdParam(), args, parameters);
        Object beforeState = ParameterExtractor.extractByName(
                auditLogging.beforeStateParam(), args, parameters);

        // 메서드 실행
        Object returnValue = joinPoint.proceed();

        // 메서드 실행 후 감사 로그 이벤트 발행 (예외 발생 여부와 관계없이)
        publishAuditEvent(auditLogging, args, parameters, actorId, actorIdentifier,
                entityId, beforeState, returnValue);

        return returnValue;
    }

    /**
     * 감사 로그 이벤트 발행
     */
    private void publishAuditEvent(AuditLogging auditLogging, Object[] args, Parameter[] parameters,
                                   Long actorId, String actorIdentifier, Long entityId,
                                   Object beforeState, Object returnValue) {
        if (actorId == null || entityId == null) {
            logMissingInfo(actorId, entityId, auditLogging);
            return;
        }

        try {
            Object afterState = extractAfterState(
                    auditLogging.afterStateParam(), args, parameters, returnValue);
            AuditAction action = AuditActionResolver.resolve(auditLogging.action(), afterState);
            String description = DescriptionBuilder.build(
                    auditLogging.description(), action, args, parameters, entityId);

            auditLogPublisher.publish(
                    actorId,
                    actorIdentifier,
                    auditLogging.entityType(),
                    entityId,
                    action,
                    description,
                    beforeState,
                    afterState,
                    HttpRequestUtils.getClientIpAddress(),
                    HttpRequestUtils.getUserAgent()
            );
        } catch (Exception e) {
            // 감사 로그 이벤트 발행 실패는 메인 트랜잭션에 영향을 주지 않도록 처리
            log.error("감사 로그 이벤트 발행 실패: entityType={}, action={}, entityId={}, actorId={}",
                    auditLogging.entityType(), auditLogging.action(), entityId, actorId, e);
        }
    }

    /**
     * 누락된 정보 로깅
     */
    private void logMissingInfo(Long actorId, Long entityId, AuditLogging auditLogging) {
        if (actorId == null) {
            log.warn("감사 로그 기록 생략: actorId를 찾을 수 없음");
        }
        if (entityId == null) {
            log.warn("감사 로그 기록 생략: entityId를 찾을 수 없음. entityIdParam={}",
                    auditLogging.entityIdParam());
        }
    }


    /**
     * 엔티티 ID 추출
     * Annotation 기반 (명시적 paramName) 또는 타입 기반 (Long 타입 첫 번째 파라미터)으로 추출
     */
    private Long extractEntityId(String paramName, Object[] args, Parameter[] parameters) {
        // 1. 명시적 파라미터 이름이 지정된 경우 (Annotation 기반)
        if (paramName != null && !paramName.isEmpty()) {
            Long value = ParameterExtractor.extractLongByName(paramName, args, parameters);
            if (value != null) {
                return value;
            }
        }

        // 2. 타입 기반: Long 타입 파라미터 자동 감지 (첫 번째 Long 파라미터)
        return ParameterExtractor.extractFirstLong(args, parameters);
    }

    /**
     * 변경 후 상태 추출
     * afterStateParam이 "return"이면 메서드 반환값을 사용하고,
     * 파라미터 이름이면 해당 파라미터를 사용합니다.
     */
    private Object extractAfterState(String afterStateParam, Object[] args,
                                     Parameter[] parameters, Object returnValue) {
        if (afterStateParam == null || afterStateParam.isEmpty()) {
            return null;
        }

        if ("return".equals(afterStateParam)) {
            return returnValue;
        }

        return ParameterExtractor.extractByName(afterStateParam, args, parameters);
    }
}

