package org.example.sharedprompts.domain.prompt.common.enums.action.metadata;

import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionDomainRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.action.registry.ActionOutputBehaviorRegistry;
import org.example.sharedprompts.domain.prompt.common.enums.output.OutputBehaviorType;
import org.example.sharedprompts.domain.prompt.common.enums.semantic.TaskDomain;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/** classpath properties에서 출력 정책·TaskDomain 메타데이터 로드. enum 외부 정책용. */
public final class ActionTypeMetadataLoader {

    public static final String OUTPUT_BEHAVIOR_RESOURCE = "action-type-output-behavior.properties";
    public static final String DOMAIN_RESOURCE = "action-type-domain.properties";

    private ActionTypeMetadataLoader() {}

    /** stableKey → OutputBehaviorType 로드. 리소스 없으면 빈 맵. */
    public static Map<String, OutputBehaviorType> loadKeyToOutputBehavior() {
        return loadKeyToEnum(OUTPUT_BEHAVIOR_RESOURCE, OutputBehaviorType.class);
    }

    /** stableKey → TaskDomain 로드. 리소스 없으면 빈 맵. */
    public static Map<String, TaskDomain> loadKeyToDomain() {
        return loadKeyToEnum(DOMAIN_RESOURCE, TaskDomain.class);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> Map<String, E> loadKeyToEnum(String resource, Class<E> enumClass) {
        try (InputStream in = ActionTypeMetadataLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return Collections.emptyMap();
            }
            Properties props = new Properties();
            props.load(in);
            Map<String, E> result = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                if (value == null || value.isBlank()) continue;
                String trimmedKey = key.trim();
                String trimmedValue = value.trim();
                if (trimmedKey.isEmpty()) continue;
                try {
                    result.put(trimmedKey, (E) Enum.valueOf(enumClass, trimmedValue));
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(
                            "Invalid " + enumClass.getSimpleName() + " mapping for key '" + trimmedKey + "' in " + resource,
                            e
                    );
                }
            }
            return Collections.unmodifiableMap(result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load action metadata resource: " + resource, e);
        }
    }
}
