package org.example.sharedprompts.dto.user.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.follow.PublicFollowState;
import org.example.sharedprompts.domain.user.User;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicProfileDto {

    private Long id;
    private String nickname;
    private String thumbnail;
    
    @JsonProperty("followers_count")
    private Long followersCount;
    
    @JsonProperty("following_count")
    private Long followingCount;
    
    @JsonProperty("follow_state")
    private PublicFollowState followState;

    public static UserPublicProfileDto from(
            User user,
            Long followersCount,
            Long followingCount,
            PublicFollowState publicFollowState
    ) {
        return UserPublicProfileDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .thumbnail(user.getThumbnail())
                .followersCount(followersCount)
                .followingCount(followingCount)
                .followState(publicFollowState)
                .build();
    }
}

