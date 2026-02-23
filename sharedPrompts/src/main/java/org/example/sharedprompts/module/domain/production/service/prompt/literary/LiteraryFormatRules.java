package org.example.sharedprompts.module.domain.production.service.prompt.literary;

import org.example.sharedprompts.module.domain.production.model.literary.LiteraryType;
import org.springframework.stereotype.Component;

@Component
public class LiteraryFormatRules {

    private static final String OUTPUT_ONLY_RULE = """
        [필수] 결과물만 출력하세요. 서론, 설명, 주석, 해석, "다음과 같이 작성했습니다" 등의 문구는 절대 포함하지 마세요.
        """;

    public String getFormatRules(LiteraryType literaryType) {
        String typeRules = switch (literaryType) {
            case POEM -> getPoemRules();
            case SHORT_STORY -> getShortStoryRules();
            case NOVEL -> getNovelRules();
            case SCRIPT -> getScriptRules();
        };
        return typeRules + "\n" + OUTPUT_ONLY_RULE;
    }

    private String getPoemRules() {
        return """
            [POEM 형식]
            - 시는 반드시 행(줄) 단위로 구성한다.
            - 각 행은 한 줄에 한 단위만 출력한다.
            - 불필요한 산문, 설명, 제목 설명은 포함하지 않는다.
            - 시 제목만 첫 줄에 두고, 이어서 본문만 출력한다.
            """;
    }

    private String getShortStoryRules() {
        return """
            [SHORT_STORY 형식]
            - 단편소설은 도입 → 전개 → 클라이맥스 → 결말 구조를 반드시 따른다.
            - 완결된 하나의 이야기로 끝낸다.
            - 중간에 끊기거나 "이어서 쓰려면" 등의 문구를 넣지 않는다.
            """;
    }

    private String getNovelRules() {
        return """
            [NOVEL 형식]
            - 장(Chapter) 단위로 구분한다. 각 장에는 "제N장" 또는 "Chapter N" 형태의 제목을 둔다.
            - 한 번에 요청한 분량만 생성하고, 끝맺음을 명확히 한다.
            - 장이 여러 개일 경우 이어지는 맥락을 유지한다.
            """;
    }

    private String getScriptRules() {
        return """
            [SCRIPT 형식]
            - 소설처럼 서술하는 문체를 사용하지 않는다.
            - 반드시 "등장인물 이름: 대사" 형식으로만 대사를 쓴다.
            - 장면 구분은 "Scene N" 또는 "[장면 N]" 등으로 명시한다.
            - 지문은 최소한으로, (괄호) 안에만 간단히 표기한다.
            """;
    }
}
