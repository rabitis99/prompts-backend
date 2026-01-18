package org.example.sharedprompts.dto.follow.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.follow.FollowStatus;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowResponseDto {

    @JsonProperty("status")
    private String status;

    public static FollowResponseDto from(FollowStatus status) {
        return FollowResponseDto.builder()
                .status(status != null ? status.name() : null)
                .build();
    }
}

