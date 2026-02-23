package org.example.sharedprompts.module.domain.production.service.literary.validation;

import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class LiteraryValidatorRegistry {

    private final Map<LiteraryType, LiteraryOutputValidator> validatorMap = new ConcurrentHashMap<>();

    public LiteraryValidatorRegistry(List<LiteraryOutputValidator> validators) {
        for (LiteraryOutputValidator v : validators) {
            validatorMap.put(v.getLiteraryType(), v);
            log.info("Registered LiteraryOutputValidator: {} for {}", v.getClass().getSimpleName(), v.getLiteraryType());
        }
    }

    public LiteraryOutputValidator getValidator(LiteraryType literaryType) {
        LiteraryOutputValidator v = validatorMap.get(literaryType);
        if (v == null) {
            return validatorMap.get(LiteraryType.POEM);
        }
        return v;
    }
}
