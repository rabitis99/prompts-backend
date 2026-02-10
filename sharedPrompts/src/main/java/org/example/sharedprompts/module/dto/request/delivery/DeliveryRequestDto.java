package org.example.sharedprompts.module.dto.request.delivery;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.sharedprompts.module.domain.delivery.api.model.DeliveryContext;
import org.example.sharedprompts.module.domain.delivery.api.type.DeliveryType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequestDto {
    
    @NotNull(message = "Delivery 타입을 입력해주세요.")
    private DeliveryType deliveryType;
    
    @NotNull(message = "Delivery Context를 입력해주세요.")
    @Valid
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deliveryType", visible = true)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = BlogDeliveryContextDto.class, name = "BLOG"),
        @JsonSubTypes.Type(value = EmailDeliveryContextDto.class, name = "EMAIL"),
        @JsonSubTypes.Type(value = GitHubDeliveryContextDto.class, name = "GITHUB"),
        @JsonSubTypes.Type(value = NotionDeliveryContextDto.class, name = "NOTION")
    })
    private DeliveryContextDto context;
    
    public DeliveryContext toDeliveryContext(Long userId) {
        DeliveryContext deliveryContext = new DeliveryContext(deliveryType, userId);
        
        if (context instanceof BlogDeliveryContextDto blogCtx) {
            // additionalAttributes를 먼저 설정하여 고정 속성이 우선되도록 함
            if (blogCtx.getAdditionalAttributes() != null) {
                blogCtx.getAdditionalAttributes().forEach(deliveryContext::setAttribute);
            }
            deliveryContext.setAttribute("platform", blogCtx.getPlatform());
            deliveryContext.setAttribute("title", blogCtx.getTitle());
        } else if (context instanceof EmailDeliveryContextDto emailCtx) {
            // additionalAttributes를 먼저 설정하여 고정 속성이 우선되도록 함
            if (emailCtx.getAdditionalAttributes() != null) {
                emailCtx.getAdditionalAttributes().forEach(deliveryContext::setAttribute);
            }
            deliveryContext.setAttribute("to", emailCtx.getTo());
            deliveryContext.setAttribute("subject", emailCtx.getSubject());
        } else if (context instanceof GitHubDeliveryContextDto githubCtx) {
            // additionalAttributes를 먼저 설정하여 고정 속성이 우선되도록 함
            if (githubCtx.getAdditionalAttributes() != null) {
                githubCtx.getAdditionalAttributes().forEach(deliveryContext::setAttribute);
            }
            deliveryContext.setAttribute("repository", githubCtx.getRepository());
            deliveryContext.setAttribute("branch", githubCtx.getBranch());
            deliveryContext.setAttribute("path", githubCtx.getPath());
        } else if (context instanceof NotionDeliveryContextDto notionCtx) {
            // additionalAttributes를 먼저 설정하여 고정 속성이 우선되도록 함
            if (notionCtx.getAdditionalAttributes() != null) {
                notionCtx.getAdditionalAttributes().forEach(deliveryContext::setAttribute);
            }
            deliveryContext.setAttribute("pageId", notionCtx.getPageId());
            deliveryContext.setAttribute("title", notionCtx.getTitle());
        } else {
            // 새로운 DeliveryContextDto 구현체가 추가된 경우를 대비
            // additionalAttributes만 설정 (고정 속성은 없음)
            if (context.getAdditionalAttributes() != null) {
                context.getAdditionalAttributes().forEach(deliveryContext::setAttribute);
            }
        }
        
        return deliveryContext;
    }
}

