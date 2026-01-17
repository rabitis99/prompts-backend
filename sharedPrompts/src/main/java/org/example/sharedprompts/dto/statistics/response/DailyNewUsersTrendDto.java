package org.example.sharedprompts.dto.statistics.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일별 신규 가입자 추이 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyNewUsersTrendDto {

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("count")
    private Long count;
}
