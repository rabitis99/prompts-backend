package org.example.sharedprompts.domain.prompt.common.enums.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.example.sharedprompts.domain.prompt.common.enums.action.ActionTypeInterface;
import org.example.sharedprompts.domain.prompt.common.enums.action.resolver.ActionTypeResolver;

import java.io.IOException;
import java.util.Objects;

/**
 * ActionType JSON 역직렬화. 주입된 {@link ActionTypeResolver}로만 해석.
 * Blank/null은 해석하지 않으며, null 또는 예외로 처리한다 (기본값 정책 없음).
 */
public class ActionTypeDeserializer extends JsonDeserializer<ActionTypeInterface> {

    private final ActionTypeResolver resolver;

    public ActionTypeDeserializer(ActionTypeResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    @Override
    public ActionTypeInterface deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("action_type must not be null or blank");
        }
        return resolver.resolve(value.trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown action type: " + value));
    }
}
