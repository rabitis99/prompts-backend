package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.EtcActionType;
import org.example.sharedprompts.domain.prompt.infrastructure.serialization.ActionTypeRegistryHolder;

/** ActionType JSON 역직렬화기. */
public class ActionTypeDeserializer extends JsonDeserializer<ActionTypeInterface> {

    @Override
    public ActionTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            return EtcActionType.GENERAL_CONSULTATION;
        }
        return ActionTypeRegistryHolder.getResolver().resolve(value)
                .orElseThrow(() -> new IllegalArgumentException("Unknown action type: " + value));
    }
}