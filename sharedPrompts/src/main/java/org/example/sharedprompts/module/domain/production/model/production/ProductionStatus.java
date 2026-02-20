package org.example.sharedprompts.module.domain.production.model.production;

/**
 * Production 처리 상태
 */
public enum ProductionStatus {
    PROCESSING,  // 처리 중
    SUCCEEDED,   // 성공
    FAILED       // 실패
}