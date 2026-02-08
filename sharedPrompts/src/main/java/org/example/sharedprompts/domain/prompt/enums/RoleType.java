package org.example.sharedprompts.domain.prompt.enums;

import org.example.sharedprompts.domain.prompt.enums.role.RoleTypeInterface;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RoleType implements RoleTypeInterface {
    ;

    private final String roleNameKo;
    private final String descriptionKo;
    private final String roleNameEn;
    private final String descriptionEn;
    private final String roleNameJa;
    private final String descriptionJa;
}
