package org.example.sharedprompts.module.dto.request.production;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.example.sharedprompts.module.domain.production.api.command.ProductionCommandType;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "command_type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BlogCommandDto.class, name = "BLOG"),
        @JsonSubTypes.Type(value = EmailCommandDto.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = TextCommandDto.class, name = "TEXT"),
        @JsonSubTypes.Type(value = ImageCommandDto.class, name = "IMAGE")
})
public interface CommandDto {
    ProductionCommandType getCommandType();
}

