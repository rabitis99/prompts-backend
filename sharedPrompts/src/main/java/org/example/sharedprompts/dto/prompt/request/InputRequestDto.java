package org.example.sharedprompts.dto.prompt.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.domain.prompt.enums.*;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;
import org.example.sharedprompts.domain.prompt.enums.serializer.ActionTypeDeserializer;
import org.example.sharedprompts.domain.prompt.enums.serializer.ActionTypeSerializer;
import org.example.sharedprompts.domain.prompt.enums.serializer.RoleTypeDeserializer;
import org.example.sharedprompts.domain.prompt.enums.serializer.RoleTypeSerializer;

import java.util.List;

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
