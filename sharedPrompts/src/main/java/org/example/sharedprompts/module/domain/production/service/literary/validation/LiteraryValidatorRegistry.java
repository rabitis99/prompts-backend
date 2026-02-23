package org.example.sharedprompts.module.domain.production.service.literary.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class LiteraryValidatorRegistry {

    private final Map<LiteraryType, LiteraryOutputValidator> validatorMap;

    public LiteraryValidatorRegistry(List<LiteraryOutputValidator> validators) {
        Map<LiteraryType, LiteraryOutputValidator> map = new EnumMap<>(LiteraryType.class);
        for (LiteraryOutputValidator v : validators) {
            if (v.getLiteraryType() == null) {
                throw new IllegalStateException(
                        "getLiteraryType() returned null for " + v.getClass().getSimpleName());
            }
            LiteraryOutputValidator existing = map.put(v.getLiteraryType(), v);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate LiteraryOutputValidator for type " + v.getLiteraryType()
                                + ": " + existing.getClass().getSimpleName() + " and " + v.getClass().getSimpleName());
            }
            log.info("Registered LiteraryOutputValidator: {} for {}", v.getClass().getSimpleName(), v.getLiteraryType());
        }
        Set<LiteraryType> missing = EnumSet.allOf(LiteraryType.class);
        missing.removeAll(map.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "No LiteraryOutputValidator registered for types: " + missing);
        }
        this.validatorMap = Collections.unmodifiableMap(map);
    }

    public LiteraryOutputValidator getValidator(LiteraryType literaryType) {
        if (literaryType == null) {
            throw new IllegalArgumentException("literaryType must not be null");
        }
        LiteraryOutputValidator v = validatorMap.get(literaryType);
        if (v == null) {
            throw new IllegalArgumentException("No LiteraryOutputValidator registered for type: " + literaryType);
        }
        return v;
    }
}
