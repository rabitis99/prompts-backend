package org.example.sharedprompts.module.domain.production.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sharedprompts.domain.payment.domain.enums.ModuleType;
import org.example.sharedprompts.domain.payment.service.user.tier.UserTierService;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactory;
import org.example.sharedprompts.module.domain.production.application.factory.ProductionCommandFactoryRegistry;
import org.example.sharedprompts.module.domain.production.model.contract.command.ProductionCommand;
import org.example.sharedprompts.module.domain.production.model.job.Job;
import org.example.sharedprompts.module.domain.production.service.job.queue.JobQueueService;
import org.example.sharedprompts.module.domain.production.validation.ValidatorRegistry;
import org.example.sharedprompts.module.dto.request.production.ProductionRequest;
import org.example.sharedprompts.module.dto.response.production.JobResponseDto;
import org.example.sharedprompts.module.dto.response.production.JobResponseDtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 Production Application Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionApplicationService {
    
    private final ProductionCommandFactoryRegistry factoryRegistry;
    private final ValidatorRegistry validatorRegistry;
    private final JobQueueService jobQueueService;
    private final UserTierService userTierService;
    
    /**
     * Production 요청을 처리하고 Job을 생성한다.
     */
    @Transactional
    public JobResponseDto produce(Long promptId, Long userId, ProductionRequest request) {
        log.info("Production requested - promptId: {}, userId: {}, requestType: {}", 
                promptId, userId, request.getClass().getSimpleName());
        
        // Factory를 통해 Command 생성 (명시적 타입 매핑)
        ProductionCommandFactory factory = factoryRegistry.getFactory(request.getClass());
        ProductionCommand command = factory.createCommand(request);
        
        log.debug("Command created - commandType: {}, commandId: {}", 
                command.getCommandType(), command.getCommandId());
        
        // Command 검증
        validatorRegistry.validate(command);
        
        // 통합 한도에서 모듈별 차감량만큼 차감 (LITERARY=2, 그 외=1 등). 한도 부족 시 MODULE_DAILY_LIMIT_EXCEEDED로 Job 생성 안 함
        ModuleType moduleType = ModuleType.from(command.getCommandType().name());
        if (moduleType == null || moduleType == ModuleType.UNKNOWN) {
            throw new ApiException(ErrorCode.UNSUPPORTED_MODULE_TYPE);
        }
        userTierService.consumeModuleUsage(userId, moduleType);
        
        // userInput 추출 (인터페이스 메서드로 타입 안전하게 추출)
        String userInput = request.userInput();
        
        // Job 큐에 추가
        String jobId = jobQueueService.enqueueJob(
                promptId,
                userId,
                command,
                userInput
        );
        
        log.info("Production job enqueued - jobId: {}, userId: {}, commandType: {}", 
                jobId, userId, command.getCommandType());
        
        // Job 조회하여 응답 생성
        Job job = jobQueueService.getJob(jobId);
        return JobResponseDtoMapper.toDto(job);
    }
}

