package org.example.sharedprompts.domain.prompt.common.enums.action;

import org.example.sharedprompts.domain.prompt.common.enums.action.category.analysis.AnalysisActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.BusinessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CareerActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.business.CustomerSupportActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content.ContentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content.EmailActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.content.RecommendationActionType;
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
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.EtcActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.HealthFitnessActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.LifestyleActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.etc.SocialActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.marketing.MarketingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.PersonalDevelopmentActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ProductivityActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.productivity.ShoppingActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.research.ResearchActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.study.StudyActionType;
import org.example.sharedprompts.domain.prompt.common.enums.action.category.writing.WritingActionType;

import java.util.List;

/**
 * Central catalog of supported ActionType enum classes.
 * Single source of truth for deserializers and tests; adding a new ActionType
 * requires only (1) the enum class with its behavior and (2) registering it here.
 */
public final class ActionTypeCatalog {

    private ActionTypeCatalog() {
    }

    /**
     * All ActionType enum classes used for JSON deserialization and coverage tests.
     * Order may affect resolution when multiple enums could match; keep consistent with
     * existing stable keys to preserve backward compatibility.
     */
    public static final List<Class<? extends Enum<?>>> ACTION_ENUMS = List.of(
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
            ContentActionType.class,
            CreativeActionType.class,
            StudyActionType.class,
            EducationActionType.class,
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
}
