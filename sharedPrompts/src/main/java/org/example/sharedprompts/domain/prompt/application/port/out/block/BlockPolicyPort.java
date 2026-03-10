package org.example.sharedprompts.domain.prompt.application.port.out.block;

/** 차단 정책 아웃바운드 포트 (접근 제어용) */
public interface BlockPolicyPort {

    boolean isBlocked(Long viewerId, Long authorId);
}
