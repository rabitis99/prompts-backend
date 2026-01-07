package org.example.sharedprompts.domain.prompt.service.guideline;

import org.example.sharedprompts.dto.prompt.request.InputRequestDto;
import org.springframework.stereotype.Service;

@Service
public class JapaneseGuidelineBuilder extends PromptGuidelineBuilder {

    @Override
    protected String definePrinciples(InputRequestDto request) {
        return """
        # 行動原則（厳守）

        1. 利用者レベル:
           - %s

        2. 専門的役割:
           - %s

        3. 品質基準:
           - 推測や不確実な情報は禁止
           - 即座に利用可能な情報のみ提供

        4. 文脈遵守:
           - 要求範囲を逸脱しない
        """.formatted(
                request.getExperience().getGuidelineJa(),
                request.getPromptCategory().getGuidelineJa()
        );
    }

    @Override
    protected String defineWorkingStyle(InputRequestDto request) {
        return """
        # 作業スタイル（必須）

        - トーン:
          %s

        - 表現スタイル:
          %s

        - 構成:
          見出しや箇条書きを用いて
          論理的に構成すること
        """.formatted(
                request.getTone().getGuidelineJa(),
                request.getStyle().getGuidelineJa()
        );
    }

    @Override
    protected String defineResponseGuidelines(InputRequestDto request) {
        return """
        # 応答ガイドライン（違反不可）

        - 明確性:
          曖昧な表現は禁止

        - 実用性:
          理論より実務優先

        - 制限:
          挨拶文や前置きは禁止
        """;
    }
}
