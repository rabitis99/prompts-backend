package org.example.sharedprompts.domain.prompt.enums.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

import java.io.IOException;

public class ActionTypeSerializer extends JsonSerializer<ActionTypeInterface> {
    @Override
    public void serialize(ActionTypeInterface value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(((Enum<?>) value).name());
        }
    }
}

