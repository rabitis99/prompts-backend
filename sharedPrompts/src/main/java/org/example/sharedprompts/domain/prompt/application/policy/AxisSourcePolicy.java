package org.example.sharedprompts.domain.prompt.application.policy;

import org.example.sharedprompts.domain.prompt.common.AxisSourceConstants;
import org.example.sharedprompts.domain.prompt.domain.semantic.ConfirmedSemanticAxes;
import org.springframework.stereotype.Component;

import java.util.Map;

/** axis_sources 맵 구성 정책 */
@Component
public class AxisSourcePolicy {

    public Map<String, String> forExtraction() {
        return Map.of(
                "intent", AxisSourceConstants.IMPLIED_BY_MODE,
                "objective", AxisSourceConstants.IMPLIED_BY_MODE,
                "output_needs", AxisSourceConstants.IMPLIED_BY_MODE
        );
    }

    public Map<String, String> fromResolutionMetadata(
            boolean isExtraction,
            boolean userProvidedIntent,
            boolean userProvidedRole,
            boolean userProvidedAction,
            boolean fallbackIntentUsed
    ) {
        if (isExtraction) {
            return forExtraction();
        }
        return Map.of(
                "intent", userProvidedIntent ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.FALLBACK,
                "role", userProvidedRole ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.RECOMMENDED,
                "action", userProvidedAction ? AxisSourceConstants.USER_PROVIDED : AxisSourceConstants.RECOMMENDED,
                "objective", AxisSourceConstants.RECOMMENDED,
                "output_needs", AxisSourceConstants.RECOMMENDED
        );
    }

    public Map<String, String> forConfirmedAxes(ConfirmedSemanticAxes axes) {
        Map<String, String> map = new java.util.HashMap<>();
        map.put("intent", AxisSourceConstants.USER_PROVIDED);
        if (axes.role().isPresent()) {
            map.put("role", AxisSourceConstants.USER_PROVIDED);
        }
        if (axes.actionType().isPresent()) {
            map.put("action", AxisSourceConstants.USER_PROVIDED);
        }
        map.put("objective", AxisSourceConstants.RECOMMENDED);
        map.put("output_needs", AxisSourceConstants.RECOMMENDED);
        return Map.copyOf(map);
    }
}
