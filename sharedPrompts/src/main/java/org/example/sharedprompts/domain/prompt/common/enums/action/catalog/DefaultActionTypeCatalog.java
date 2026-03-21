package org.example.sharedprompts.domain.prompt.common.enums.action.catalog;

import org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.BusinessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CareerActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CustomerSupportActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation.ContentCreationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation.EmailActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content_creation.RecommendationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.creative.CreativeActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.design.DesignActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.AiMlActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CloudServicesActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CodingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.CybersecurityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.DevOpsActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.DevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.development.ProgrammingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.education.EducationActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.legal.LegalActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.EtcActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.HealthFitnessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.LifestyleActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.SocialActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing.MarketingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.PersonalDevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ShoppingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.research.ResearchActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;

import java.util.List;

/** 기본 ActionType 카탈로그. 정의 집합만 제공. Compatibility 해석 순서는 OrderedActionTypeResolutionSource에서 별도 구성. */
public final class DefaultActionTypeCatalog implements ActionTypeCatalog {

    /** 정의된 enum 목록. Registry 구성 등에 사용. Compatibility 해석 순서는 OrderedActionTypeResolutionSource 사용. */
    private static final List<Class<? extends Enum<?>>> ENUM_CLASSES = List.of(
            ProductivityActionType.class,
            DevelopmentActionType.class,
            CloudServicesActionType.class,
            DevOpsActionType.class,
            CybersecurityActionType.class,
            CodingActionType.class,
            ProgrammingActionType.class,
            AiMlActionType.class,
            AnalysisActionType.class,
            MarketingActionType.class,
            ContentCreationActionType.class,
            CreativeActionType.class,
            EducationActionType.class,
            LegalActionType.class,
            ResearchActionType.class,
            BusinessActionType.class,
            CustomerSupportActionType.class,
            EmailActionType.class,
            DesignActionType.class,
            WritingActionType.class,
            EtcActionType.class,
            HealthFitnessActionType.class,
            SocialActionType.class,
            CareerActionType.class,
            LifestyleActionType.class,
            PersonalDevelopmentActionType.class,
            RecommendationActionType.class,
            ShoppingActionType.class
    );

    @Override
    public List<Class<? extends Enum<?>>> getActionTypeEnumClasses() {
        return ENUM_CLASSES;
    }
}
