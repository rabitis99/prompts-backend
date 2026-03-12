package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeCatalog;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.EtcActionType;

import java.io.IOException;

public class ActionTypeDeserializer extends JsonDeserializer<ActionTypeInterface> {

    @Override
    public ActionTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            // InputRequestDto에는 @NotNull 제약이 없으므로, null/blank 값에 대해 기본값 반환
            // PromptGuidelineBuilder에서 NPE 방지
            return EtcActionType.GENERAL_CONSULTATION;
        }

        return EnumResolver.resolve(value, ActionTypeCatalog.ACTION_ENUMS);
    }
}

