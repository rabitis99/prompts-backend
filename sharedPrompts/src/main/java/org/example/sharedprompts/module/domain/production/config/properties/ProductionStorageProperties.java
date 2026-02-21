package org.example.sharedprompts.module.domain.production.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.sharedprompts.module.domain.production.service.storage.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Production Storage 설정 바인딩.
 * <p>
 * StorageType은 현재 S3만 지원합니다. (LOCAL 등 미지원 값은 바인딩 단계에서 실패하여 조기 탐지됩니다)
 */
@Getter
@Setter
@ToString
@Validated
@ConfigurationProperties(prefix = "production.storage")
public class ProductionStorageProperties {

    /**
     * 저장소 타입 (현재: S3만 지원)
     */
    private StorageType type = StorageType.S3;
}


