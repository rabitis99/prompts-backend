package org.example.sharedprompts.global.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.auth.security.constant.SecurityConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.Callable;

/**
 * 비동기 요청 처리 시 SecurityContext를 유지하기 위한 설정
 * 
 * <p>WebAsyncTask와 DeferredResult를 사용하는 비동기 요청에서
 * SecurityContext가 스레드 간 전파되도록 보장합니다.
 */
@Slf4j
@Configuration
public class WebAsyncSecurityConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.registerCallableInterceptors(new SecurityContextCallableInterceptor());
        configurer.registerDeferredResultInterceptors(new SecurityContextDeferredResultInterceptor());
    }

    /**
     * HttpServletRequest에서 SecurityContext를 복원합니다
     */
    private static void restoreSecurityContext(NativeWebRequest request) {
        HttpServletRequest httpRequest = request.getNativeRequest(HttpServletRequest.class);
        if (httpRequest != null) {
            SecurityContext context = (SecurityContext) httpRequest.getAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME);
            if (context != null) {
                SecurityContextHolder.setContext(context);
            }
        }
    }

    /**
     * Callable 기반 비동기 요청에서 SecurityContext를 관리하는 인터셉터
     */
    private static class SecurityContextCallableInterceptor implements CallableProcessingInterceptor {
        @Override
        public <T> void beforeConcurrentHandling(@NotNull NativeWebRequest request, @NotNull Callable<T> task) {
            // 비동기 처리가 시작되기 전에 현재 SecurityContext를 저장
            SecurityContext context = SecurityContextHolder.getContext();
            if (context.getAuthentication() != null) {
                HttpServletRequest httpRequest = request.getNativeRequest(HttpServletRequest.class);
                if (httpRequest != null) {
                    httpRequest.setAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME, context);
                    if (log.isDebugEnabled()) {
                        log.debug("[WebAsyncSecurityConfig] SecurityContext 저장: thread={}, uri={}, hasAuth=true",
                                Thread.currentThread().getName(), httpRequest.getRequestURI());
                    }
                }
            }
        }

        @Override
        public <T> void preProcess(@NotNull NativeWebRequest request, @NotNull Callable<T> task) {
            // 비동기 작업 실행 전에 SecurityContext 복원
            restoreSecurityContext(request);
        }

        @Override
        public <T> void postProcess(@NotNull NativeWebRequest request, @NotNull Callable<T> task, Object concurrentResult) {
            // 비동기 작업 완료 후 SecurityContext 복원 및 유지
            // 응답 생성 시 SecurityContext가 필요하므로 복원합니다
            restoreSecurityContext(request);
        }

        @Override
        public <T> Object handleTimeout(@NotNull NativeWebRequest request, @NotNull Callable<T> task) {
            // 타임아웃 발생 시 SecurityContext 복원
            restoreSecurityContext(request);
            return null;
        }

        @Override
        public <T> Object handleError(@NotNull NativeWebRequest request, @NotNull Callable<T> task, Throwable t) {
            // 에러 발생 시 SecurityContext 복원
            restoreSecurityContext(request);
            return null;
        }

        @Override
        public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) {
            // 비동기 작업 완료 후 SecurityContext 정리
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * DeferredResult 기반 비동기 요청에서 SecurityContext를 관리하는 인터셉터
     */
    private static class SecurityContextDeferredResultInterceptor implements DeferredResultProcessingInterceptor {
        @Override
        public <T> void beforeConcurrentHandling(NativeWebRequest request, org.springframework.web.context.request.async.DeferredResult<T> deferredResult) {
            // 비동기 처리가 시작되기 전에 현재 SecurityContext를 저장
            SecurityContext context = SecurityContextHolder.getContext();
            if (context.getAuthentication() != null) {
                HttpServletRequest httpRequest = request.getNativeRequest(HttpServletRequest.class);
                if (httpRequest != null) {
                    httpRequest.setAttribute(SecurityConstants.SPRING_SECURITY_CONTEXT_ATTRIBUTE_NAME, context);
                    if (log.isDebugEnabled()) {
                        log.debug("[WebAsyncSecurityConfig] SecurityContext 저장 (DeferredResult): thread={}, hasAuth=true",
                                Thread.currentThread().getName());
                    }
                }
            }
        }

        @Override
        public <T> void preProcess(NativeWebRequest request, org.springframework.web.context.request.async.DeferredResult<T> deferredResult) {
            // 비동기 작업 실행 전에 SecurityContext 복원
            restoreSecurityContext(request);
        }

        @Override
        public <T> void postProcess(NativeWebRequest request, org.springframework.web.context.request.async.DeferredResult<T> deferredResult, Object concurrentResult) {
            // 비동기 작업 완료 후 SecurityContext 복원
            restoreSecurityContext(request);
        }

        @Override
        public <T> boolean handleTimeout(NativeWebRequest request, org.springframework.web.context.request.async.DeferredResult<T> deferredResult) {
            // 타임아웃 발생 시 SecurityContext 복원
            restoreSecurityContext(request);
            return true;
        }

        @Override
        public <T> void afterCompletion(NativeWebRequest request, org.springframework.web.context.request.async.DeferredResult<T> deferredResult) {
            // 비동기 작업 완료 후 SecurityContext 정리
            SecurityContextHolder.clearContext();
        }
    }
}
