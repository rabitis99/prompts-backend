package org.example.sharedprompts.dto.prompt.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.common.enums.*;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeSerializer;

import java.util.List;

/**
 * 프롬프트 생성에 필요한 입력 데이터 DTO
 * <p>
 * <strong>필수 필드:</strong>
 * <ul>
 *   <li>{@code input} - 사용자 입력 (null이면 빈 문자열로 처리)</li>
 *   <li>{@code actionType} - 작업 유형 (null이면 {@link org.example.sharedprompts.domain.prompt.common.enums.action.EtcActionType#GENERAL_CONSULTATION} 사용)</li>
 *   <li>{@code roleType} - 역할 유형 (null이면 {@link org.example.sharedprompts.domain.prompt.common.enums.role.EtcRoleType#GENERAL_CONSULTANT} 사용)</li>
 *   <li>{@code promptCategory} - 프롬프트 카테고리 (null이면 도메인 결정 실패 가능)</li>
 * </ul>
 * <p>
 * <strong>선택 필드 (기본값 제공):</strong>
 * <ul>
 *   <li>{@code tone} - 톤 타입 (null이면 {@link org.example.sharedprompts.domain.prompt.common.enums.ToneType#NEUTRAL} 사용)</li>
 *   <li>{@code style} - 스타일 타입 (null이면 {@link org.example.sharedprompts.domain.prompt.common.enums.StyleType#NARRATIVE} 사용)</li>
 *   <li>{@code experience} - 경험 수준 (null이면 {@link org.example.sharedprompts.domain.prompt.common.enums.ExperienceLevel#INTERMEDIATE} 사용)</li>
 *   <li>{@code language} - 언어 타입 (null이면 {@link org.example.sharedprompts.domain.prompt.common.enums.LanguageType#KOREAN} 사용)</li>
 *   <li>{@code tags} - 태그 목록 (null 허용)</li>
 * </ul>
 * <p>
 * <strong>참고:</strong>
 * 일반적으로 {@link PromptRequestDto#toInputRequestDto()}를 통해 생성되며,
 * 이 메서드는 모든 필드에 기본값을 제공합니다. 직접 생성하는 경우 null-safety를 고려해야 합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputRequestDto {

    private String input;
    @JsonProperty("action_type")
    @JsonSerialize(using = ActionTypeSerializer.class)
    @JsonDeserialize(using = ActionTypeDeserializer.class)
    private ActionTypeInterface actionType;
    @JsonProperty("role_type")
    @JsonSerialize(using = RoleTypeSerializer.class)
    @JsonDeserialize(using = RoleTypeDeserializer.class)
    private RoleTypeInterface roleType;
    private ToneType tone;
    private ExperienceLevel experience;
    private StyleType style;
    private LanguageType language;
    @JsonProperty("prompt_category")
    private PromptCategory promptCategory;
    private List<String> tags;
}
