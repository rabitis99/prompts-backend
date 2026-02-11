package org.example.sharedprompts.module.domain.production.model.contract.command;

public interface ProductionCommand {
    ProductionCommandType getCommandType();
    String getCommandId();

    /**
     * 출력 파일 포맷 (md, html, json, xlsx, pdf 등)
     * 각 Command가 오버라이드하여 사용자 지정 또는 기본 포맷 반환
     */
    default String getOutputFormat() {
        return "txt";
    }

    default String toMetadataJson() {
        return null;
    }
}

