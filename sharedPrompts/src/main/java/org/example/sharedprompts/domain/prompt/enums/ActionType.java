package org.example.sharedprompts.domain.prompt.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.sharedprompts.domain.prompt.enums.action.ActionTypeInterface;

@Getter
@AllArgsConstructor
public enum ActionType implements ActionTypeInterface {
    ;

    private final String displayNameKo;
    private final String displayNameEn;
    private final String displayNameJa;
}

