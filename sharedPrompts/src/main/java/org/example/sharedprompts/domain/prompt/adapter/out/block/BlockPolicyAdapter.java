package org.example.sharedprompts.domain.prompt.adapter.out.block;

import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.follow.policy.FollowBlockPolicy;
import org.example.sharedprompts.domain.prompt.application.port.out.block.BlockPolicyPort;
import org.springframework.stereotype.Component;

// Dependency-direction adapter (no semantic translation)
/** Adapter: bridges to follow/block policy. Forwards calls without translation (types align). */
@Component
@RequiredArgsConstructor
public class BlockPolicyAdapter implements BlockPolicyPort {

    private final FollowBlockPolicy followBlockPolicy;

    @Override
    public boolean isBlocked(Long viewerId, Long authorId) {
        return followBlockPolicy.isBlocked(viewerId, authorId);
    }
}
