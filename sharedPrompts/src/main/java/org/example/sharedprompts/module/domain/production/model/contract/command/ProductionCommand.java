package org.example.sharedprompts.module.domain.production.model.contract.command;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ProductionCommand {
    @JsonIgnore
    ProductionCommandType getCommandType();

    @JsonIgnore
    String getCommandId();

    /**
     * 출력 파일 포맷 (md, html, json, xlsx, pdf 등)
     * 각 Command가 오버라이드하여 사용자 지정 또는 기본 포맷 반환
     */
    @JsonIgnore
    default String getOutputFormat() {
        return "txt";
    }

    @JsonIgnore
    default String toMetadataJson() {
        return null;
    }
}

