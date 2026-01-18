package org.example.sharedprompts.dto.follow.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowCountResponseDto {

    @JsonProperty("followers_count")
    private Long followersCount;

    @JsonProperty("following_count")
    private Long followingCount;
}

