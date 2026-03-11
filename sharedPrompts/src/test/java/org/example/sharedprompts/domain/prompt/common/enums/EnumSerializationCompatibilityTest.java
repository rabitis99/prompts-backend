package org.example.sharedprompts.domain.prompt.common.enums;

import org.example.sharedprompts.domain.prompt.common.enums.serializer.EnumCompatParser;
import org.example.sharedprompts.domain.prompt.common.enums.style.StyleType;
import org.example.sharedprompts.domain.prompt.common.enums.style.ToneType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("EnumCompatParser 직렬화 호환성 테스트")
class EnumSerializationCompatibilityTest {

    @Test
    @DisplayName("legacy Enum.name() 값으로 파싱 가능해야 한다")
    void parseFromLegacyName() {
        ToneType tone = EnumCompatParser.parse("FRIENDLY", ToneType.class, EnumCompatParser.Mode.STRICT);
        assertThat(tone).isEqualTo(ToneType.FRIENDLY);

        StyleType style = EnumCompatParser.parse("NARRATIVE", StyleType.class, EnumCompatParser.Mode.STRICT);
        assertThat(style).isEqualTo(StyleType.NARRATIVE);
    }

    @Test
    @DisplayName("새로운 stable key 값으로도 파싱 가능해야 한다")
    void parseFromStableKey() {
        ToneType tone = EnumCompatParser.parse("TONE.FRIENDLY", ToneType.class, EnumCompatParser.Mode.STRICT);
        assertThat(tone).isEqualTo(ToneType.FRIENDLY);

        StyleType style = EnumCompatParser.parse("STYLE.NARRATIVE", StyleType.class, EnumCompatParser.Mode.STRICT);
        assertThat(style).isEqualTo(StyleType.NARRATIVE);
    }

    @Test
    @DisplayName("LENIENT 모드에서는 알 수 없는 값에 대해 null 반환")
    void lenientModeReturnsNullForUnknown() {
        ToneType tone = EnumCompatParser.parse("UNKNOWN_VALUE", ToneType.class, EnumCompatParser.Mode.LENIENT);
        assertThat(tone).isNull();
    }

    @Test
    @DisplayName("STRICT 모드에서는 알 수 없는 값에 대해 예외 발생")
    void strictModeThrowsForUnknown() {
        assertThatThrownBy(() ->
                EnumCompatParser.parse("UNKNOWN_VALUE", ToneType.class, EnumCompatParser.Mode.STRICT)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}

