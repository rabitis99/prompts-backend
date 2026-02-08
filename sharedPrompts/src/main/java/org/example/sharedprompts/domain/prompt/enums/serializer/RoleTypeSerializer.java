package org.example.sharedprompts.domain.prompt.enums.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;

import java.io.IOException;

public class RoleTypeSerializer extends JsonSerializer<RoleTypeInterface> {
    @Override
    public void serialize(RoleTypeInterface value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.name());
        }
    }
}

