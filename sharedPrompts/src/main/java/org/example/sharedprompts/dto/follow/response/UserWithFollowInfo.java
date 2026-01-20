package org.example.sharedprompts.dto.follow.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.follow.Follow;
import org.example.sharedprompts.domain.user.User;

/**
 * User와 양방향 Follow 정보를 함께 담는 내부 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithFollowInfo {
    private User user;
    private Follow forwardFollow; // viewer → target
    private Follow reverseFollow; // target → viewer
}

