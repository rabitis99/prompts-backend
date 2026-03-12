package org.example.sharedprompts.domain.prompt.common.enums.action.canonical;

import java.util.HashMap;
import java.util.Map;

/**
 * Static mapping from (enum class name, constant name) to CanonicalActionId.
 * Ensures every ActionType constant has exactly one canonical capability.
 */
final class ActionToCanonicalMapping {

    private static final Map<String, CanonicalActionId> MAP = new HashMap<>();

    static {
        String P = "ProductivityActionType";
        String D = "DevelopmentActionType";
        String C = "CloudServicesActionType";
        String O = "DevOpsActionType";
        String Y = "CybersecurityActionType";
        String G = "CodingActionType";
        String R = "ProgrammingActionType";
        String A = "AiMlActionType";
        String N = "AnalysisActionType";
        String M = "MarketingActionType";
        String CC = "ContentCreationActionType";
        String CR = "CreativeActionType";
        String E = "EducationActionType";
        String RS = "ResearchActionType";
        String B = "BusinessActionType";
        String CS = "CustomerSupportActionType";
        String EM = "EmailActionType";
        String DS = "DesignActionType";
        String W = "WritingActionType";
        String ET = "EtcActionType";
        String H = "HealthFitnessActionType";
        String S = "SocialActionType";
        String CA = "CareerActionType";
        String L = "LifestyleActionType";
        String PD = "PersonalDevelopmentActionType";
        String REC = "RecommendationActionType";
        String SH = "ShoppingActionType";

        put(P, "WORKFLOW_OPTIMIZATION", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(P, "TIME_MANAGEMENT", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(P, "SCHEDULE_PLANNING", CanonicalActionId.PLANNING);
        put(P, "DAILY_PLANNING", CanonicalActionId.PLANNING);
        put(P, "WEEKLY_PLANNING", CanonicalActionId.PLANNING);
        put(P, "MONTHLY_PLANNING", CanonicalActionId.PLANNING);
        put(P, "TASK_AUTOMATION", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(P, "SHOPPING_LIST", CanonicalActionId.PLANNING);
        put(P, "MEAL_PLANNING", CanonicalActionId.PLANNING);
        put(P, "BUDGET_PLANNING", CanonicalActionId.PLANNING);
        put(P, "HOUSEHOLD_MANAGEMENT", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(P, "EFFICIENCY_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(P, "PRODUCTIVITY_PLANNING", CanonicalActionId.PLANNING);
        put(P, "PROCESS_IMPROVEMENT", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(P, "RESOURCE_OPTIMIZATION", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);

        put(D, "ARCHITECTURE_DESIGN", CanonicalActionId.STRATEGY);
        put(D, "SYSTEM_DESIGN", CanonicalActionId.STRATEGY);
        put(D, "TECH_STACK_SELECTION", CanonicalActionId.STRATEGY);
        put(D, "PERFORMANCE_OPTIMIZATION", CanonicalActionId.PERFORMANCE_OPTIMIZATION);
        put(D, "DEPLOYMENT_STRATEGY", CanonicalActionId.STRATEGY);
        put(D, "DOCUMENTATION", CanonicalActionId.TECHNICAL_WRITING);
        put(D, "CODEBASE_ANALYSIS", CanonicalActionId.CODE_ANALYSIS);
        put(D, "SECURITY_IMPLEMENTATION", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(D, "SCALABILITY_PLANNING", CanonicalActionId.PLANNING);

        put(G, "CODE_GENERATION", CanonicalActionId.CODE_GENERATION);
        put(G, "CODE_MODIFICATION", CanonicalActionId.CODE_MODIFICATION);
        put(G, "CODE_REVIEW", CanonicalActionId.EVALUATION_OR_AUDIT);
        put(G, "REFACTORING", CanonicalActionId.CODE_MODIFICATION);
        put(G, "TEST_GENERATION", CanonicalActionId.CODE_GENERATION);
        put(G, "DEBUGGING", CanonicalActionId.DEBUGGING);
        put(G, "CODE_ANALYSIS", CanonicalActionId.CODE_ANALYSIS);
        put(G, "PATTERN_APPLICATION", CanonicalActionId.CODE_GENERATION);
        put(G, "CODE_OPTIMIZATION", CanonicalActionId.CODE_MODIFICATION);
        put(G, "LEGACY_CODE_MAINTENANCE", CanonicalActionId.CODE_MODIFICATION);

        put(R, "ALGORITHM_IMPLEMENTATION", CanonicalActionId.CODE_GENERATION);
        put(R, "DATA_STRUCTURE_DESIGN", CanonicalActionId.CODE_GENERATION);
        put(R, "LANGUAGE_LEARNING", CanonicalActionId.EXPLANATION);
        put(R, "SYNTAX_OPTIMIZATION", CanonicalActionId.CODE_MODIFICATION);
        put(R, "LOGIC_DEVELOPMENT", CanonicalActionId.CODE_GENERATION);
        put(R, "API_DESIGN", CanonicalActionId.CODE_GENERATION);
        put(R, "CONCURRENT_PROGRAMMING", CanonicalActionId.CODE_GENERATION);

        put(CC, "CONTENT_CREATION", CanonicalActionId.LONG_FORM_WRITING);
        put(CC, "CONTENT_REVISION", CanonicalActionId.TEXT_REVISION);
        put(CC, "CONTENT_PLANNING", CanonicalActionId.PLANNING);
        put(CC, "BLOG_WRITING", CanonicalActionId.LONG_FORM_WRITING);
        put(CC, "SOCIAL_MEDIA_POST", CanonicalActionId.SHORT_COPY);
        put(CC, "INSTAGRAM_CAPTION", CanonicalActionId.SHORT_COPY);
        put(CC, "FACEBOOK_POST", CanonicalActionId.SHORT_COPY);
        put(CC, "X_POST", CanonicalActionId.SHORT_COPY);
        put(CC, "COMMENT_WRITING", CanonicalActionId.SHORT_COPY);
        put(CC, "VIDEO_SCRIPT", CanonicalActionId.SCRIPT_OR_MEDIA_WRITING);
        put(CC, "CONTENT_OPTIMIZATION", CanonicalActionId.TEXT_REVISION);
        put(CC, "MULTIMEDIA_PRODUCTION", CanonicalActionId.SCRIPT_OR_MEDIA_WRITING);
        put(CC, "PODCAST_SCRIPT", CanonicalActionId.SCRIPT_OR_MEDIA_WRITING);
        put(CC, "NEWSLETTER_WRITING", CanonicalActionId.LONG_FORM_WRITING);

        put(EM, "EMAIL_WRITING", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "BUSINESS_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "PERSONAL_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "THANK_YOU_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "APOLOGY_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "INQUIRY_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "INVITATION_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "FOLLOW_UP_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "REJECTION_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);
        put(EM, "CONFIRMATION_EMAIL", CanonicalActionId.MESSAGE_COMPOSITION);

        put(W, "ARTICLE_WRITING", CanonicalActionId.LONG_FORM_WRITING);
        put(W, "ESSAY_WRITING", CanonicalActionId.LONG_FORM_WRITING);
        put(W, "TECHNICAL_WRITING", CanonicalActionId.TECHNICAL_WRITING);
        put(W, "CREATIVE_WRITING_GEN", CanonicalActionId.CREATIVE_WRITING);
        put(W, "EDITING", CanonicalActionId.TEXT_REVISION);
        put(W, "PROOFREADING", CanonicalActionId.TEXT_REVISION);
        put(W, "TRANSLATION", CanonicalActionId.TRANSLATION);
        put(W, "DOC_UPDATE", CanonicalActionId.TEXT_REVISION);
        put(W, "COPYWRITING", CanonicalActionId.LONG_FORM_WRITING);
        put(W, "GRANT_WRITING", CanonicalActionId.LONG_FORM_WRITING);
        put(W, "LETTER_WRITING", CanonicalActionId.LETTER_WRITING);
        put(W, "PERSONAL_LETTER", CanonicalActionId.LETTER_WRITING);
        put(W, "BUSINESS_LETTER", CanonicalActionId.LETTER_WRITING);
        put(W, "INVITATION_CARD", CanonicalActionId.SHORT_COPY);
        put(W, "THANK_YOU_CARD", CanonicalActionId.SHORT_COPY);
        put(W, "CONGRATULATORY_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(W, "CONDOLENCE_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(W, "MESSAGE_WRITING", CanonicalActionId.MESSAGE_COMPOSITION);
        put(W, "TEXT_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(W, "WHATSAPP_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);

        put(CR, "IDEA_GENERATION", CanonicalActionId.CREATIVE_CONCEPT);
        put(CR, "CREATIVE_WRITING", CanonicalActionId.CREATIVE_WRITING);
        put(CR, "ARTISTIC_DESIGN", CanonicalActionId.DESIGN);
        put(CR, "STORYTELLING", CanonicalActionId.CREATIVE_CONCEPT);
        put(CR, "CONCEPT_DEVELOPMENT", CanonicalActionId.CREATIVE_CONCEPT);
        put(CR, "VISUAL_CREATION", CanonicalActionId.CREATIVE_CONCEPT);
        put(CR, "CHARACTER_DEVELOPMENT", CanonicalActionId.CREATIVE_CONCEPT);
        put(CR, "WORLD_BUILDING", CanonicalActionId.CREATIVE_CONCEPT);

        put(DS, "UI_DESIGN", CanonicalActionId.DESIGN);
        put(DS, "UX_DESIGN", CanonicalActionId.DESIGN);
        put(DS, "GRAPHIC_DESIGN", CanonicalActionId.DESIGN);
        put(DS, "PRODUCT_DESIGN", CanonicalActionId.DESIGN);
        put(DS, "DESIGN_DOC", CanonicalActionId.DESIGN);
        put(DS, "WIREFRAMING", CanonicalActionId.DESIGN);
        put(DS, "PROTOTYPING", CanonicalActionId.DESIGN);
        put(DS, "VISUAL_IDENTITY", CanonicalActionId.DESIGN);
        put(DS, "INTERACTION_DESIGN", CanonicalActionId.DESIGN);
        put(DS, "RESPONSIVE_DESIGN", CanonicalActionId.DESIGN);

        put(N, "DATA_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(N, "STATISTICAL_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(N, "INSIGHT_EXTRACTION", CanonicalActionId.DATA_ANALYSIS);
        put(N, "TREND_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(N, "PATTERN_RECOGNITION", CanonicalActionId.DATA_ANALYSIS);
        put(N, "PREDICTIVE_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(N, "COMPARATIVE_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(N, "ROOT_CAUSE_ANALYSIS", CanonicalActionId.DATA_ANALYSIS);
        put(N, "BUSINESS_INTELLIGENCE", CanonicalActionId.DATA_ANALYSIS);

        put(B, "PROPOSAL_WRITING", CanonicalActionId.PROJECT_OR_BUSINESS_PLAN);
        put(B, "REPORT_WRITING", CanonicalActionId.PRESENTATION_OR_REPORT);
        put(B, "BUSINESS_STRATEGY", CanonicalActionId.STRATEGY);
        put(B, "PROJECT_MANAGEMENT", CanonicalActionId.PROJECT_OR_BUSINESS_PLAN);
        put(B, "FINANCIAL_ANALYSIS", CanonicalActionId.FINANCIAL_ANALYSIS);
        put(B, "PRESENTATION_PREPARATION", CanonicalActionId.PRESENTATION_OR_REPORT);
        put(B, "CONTRACT_REVIEW", CanonicalActionId.EVALUATION_OR_AUDIT);
        put(B, "RISK_ASSESSMENT", CanonicalActionId.RISK_ASSESSMENT);
        put(B, "STAKEHOLDER_MANAGEMENT", CanonicalActionId.PROJECT_OR_BUSINESS_PLAN);
        put(B, "BUSINESS_PLAN_DEVELOPMENT", CanonicalActionId.PROJECT_OR_BUSINESS_PLAN);

        put(RS, "RESEARCH_DESIGN", CanonicalActionId.RESEARCH_METHODOLOGY);
        put(RS, "PAPER_WRITING", CanonicalActionId.LONG_FORM_WRITING);
        put(RS, "METHODOLOGY_DEVELOPMENT", CanonicalActionId.RESEARCH_METHODOLOGY);
        put(RS, "EXPERIMENT_DESIGN", CanonicalActionId.RESEARCH_METHODOLOGY);
        put(RS, "DATA_INTERPRETATION", CanonicalActionId.DATA_ANALYSIS);
        put(RS, "LITERATURE_REVIEW", CanonicalActionId.RESEARCH_METHODOLOGY);
        put(RS, "HYPOTHESIS_FORMULATION", CanonicalActionId.RESEARCH_METHODOLOGY);
        put(RS, "STATISTICAL_MODELING", CanonicalActionId.DATA_ANALYSIS);

        put(E, "CURRICULUM_DESIGN", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "MATERIAL_CREATION", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "TEACHING_METHOD", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "LEARNER_ANALYSIS", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "ASSESSMENT_DESIGN", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "INTERACTIVE_CONTENT", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "EDUCATIONAL_STRATEGY", CanonicalActionId.EDUCATION_DESIGN);
        put(E, "LESSON_PLANNING", CanonicalActionId.PLANNING);

        put(M, "MARKETING_STRATEGY", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);
        put(M, "BRANDING", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);
        put(M, "AD_CAMPAIGN", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);
        put(M, "MARKET_RESEARCH", CanonicalActionId.MARKET_OR_CUSTOMER_ANALYSIS);
        put(M, "CUSTOMER_ANALYSIS", CanonicalActionId.MARKET_OR_CUSTOMER_ANALYSIS);
        put(M, "SEO_OPTIMIZATION", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);
        put(M, "SOCIAL_MEDIA_STRATEGY", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);
        put(M, "CONTENT_MARKETING", CanonicalActionId.LONG_FORM_WRITING);
        put(M, "INFLUENCER_MARKETING", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);
        put(M, "CONVERSION_OPTIMIZATION", CanonicalActionId.MARKETING_STRATEGY_AND_EXECUTION);

        put(CA, "RESUME_WRITING", CanonicalActionId.CAREER_DOCUMENT_WRITING);
        put(CA, "COVER_LETTER", CanonicalActionId.CAREER_DOCUMENT_WRITING);
        put(CA, "INTERVIEW_PREPARATION", CanonicalActionId.INTERVIEW_PREPARATION);
        put(CA, "NETWORKING_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(CA, "APPLICATION_WRITING", CanonicalActionId.CAREER_DOCUMENT_WRITING);

        put(REC, "BOOK_RECOMMENDATION", CanonicalActionId.RECOMMEND);
        put(REC, "MOVIE_RECOMMENDATION", CanonicalActionId.RECOMMEND);
        put(REC, "RESTAURANT_RECOMMENDATION", CanonicalActionId.RECOMMEND);
        put(REC, "PRODUCT_REVIEW", CanonicalActionId.RECOMMEND);

        put(SH, "COMPARISON_SHOPPING", CanonicalActionId.SHOPPING);
        put(SH, "PRICE_NEGOTIATION", CanonicalActionId.SHOPPING);

        put(ET, "GENERAL_CONSULTATION", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(ET, "PROBLEM_SOLVING", CanonicalActionId.PROBLEM_SOLVING);
        put(ET, "INFORMATION_RESEARCH", CanonicalActionId.PROBLEM_SOLVING);
        put(ET, "RECOMMENDATION", CanonicalActionId.RECOMMEND);
        put(ET, "EXPLANATION", CanonicalActionId.EXPLANATION);
        put(ET, "ADVICE", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(ET, "GUIDANCE", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(ET, "RECIPE_CREATION", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(ET, "COOKING_TIPS", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(ET, "HEALTH_MANAGEMENT", CanonicalActionId.HEALTH_FITNESS);

        put(PD, "GOAL_SETTING", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(PD, "HABIT_FORMATION", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(PD, "SELF_IMPROVEMENT", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(PD, "STRESS_MANAGEMENT", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(PD, "MOTIVATION", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(PD, "DECISION_MAKING", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);

        put(L, "HOME_ORGANIZATION", CanonicalActionId.PRODUCTIVITY_AND_PERSONAL);
        put(L, "CHILD_CARE_TIPS", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(L, "HOBBY_EXPLORATION", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(L, "TIME_OFF_PLANNING", CanonicalActionId.PLANNING);
        put(L, "TRAVEL_PLANNING", CanonicalActionId.PLANNING);
        put(L, "EVENT_PLANNING", CanonicalActionId.PLANNING);
        put(L, "PARTY_PLANNING", CanonicalActionId.PLANNING);
        put(L, "GIFT_SELECTION", CanonicalActionId.RECOMMEND);
        put(L, "DECORATION_IDEAS", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(L, "EDUCATION_CONSULTATION", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(L, "RELATIONSHIP_ADVICE", CanonicalActionId.ADVICE_OR_GUIDANCE);
        put(L, "CONFLICT_RESOLUTION", CanonicalActionId.ADVICE_OR_GUIDANCE);

        // SocialActionType
        put(S, "COMMUNITY_ENGAGEMENT", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "EVENT_ORGANIZATION", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "SOCIAL_CAUSE_SUPPORT", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "VOLUNTEER_COORDINATION", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "FUNDRAISING", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "COMMUNITY_OUTREACH", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "SOCIAL_MEDIA_ENGAGEMENT", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "NETWORKING_EVENT", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "COMMUNITY_BUILDING", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "SOCIAL_ACTIVISM", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "ADVOCACY", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "PUBLIC_SPEAKING", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "SOCIAL_IMPACT", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "COLLABORATION", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "PARTNERSHIP_BUILDING", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "CROSS_CULTURAL_COMMUNICATION", CanonicalActionId.SOCIAL_OR_COMMUNITY);
        put(S, "COMPLAINT_WRITING", CanonicalActionId.REVIEW_OR_FEEDBACK_WRITING);
        put(S, "FEEDBACK_WRITING", CanonicalActionId.REVIEW_OR_FEEDBACK_WRITING);
        put(S, "REVIEW_WRITING", CanonicalActionId.REVIEW_OR_FEEDBACK_WRITING);
        put(S, "TESTIMONIAL_WRITING", CanonicalActionId.REVIEW_OR_FEEDBACK_WRITING);
        put(S, "FOLLOW_UP_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(S, "APPOINTMENT_SCHEDULING", CanonicalActionId.MESSAGE_COMPOSITION);
        put(S, "REMINDER_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(S, "BIRTHDAY_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(S, "ANNIVERSARY_MESSAGE", CanonicalActionId.MESSAGE_COMPOSITION);
        put(S, "HOLIDAY_GREETING", CanonicalActionId.SHORT_COPY);
        put(S, "SEASONAL_GREETING", CanonicalActionId.SHORT_COPY);

        // CustomerSupportActionType — all CUSTOMER_SUPPORT_OPERATIONS
        put(CS, "TICKET_CREATION", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "TICKET_RESOLUTION", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "KNOWLEDGE_BASE_MANAGEMENT", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "LIVE_CHAT_SUPPORT", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "CUSTOMER_SERVICE", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "FAQ_CREATION", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "SUPPORT_DOCUMENTATION", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "CUSTOMER_ONBOARDING", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "CUSTOMER_RETENTION", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "COMPLAINT_HANDLING", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "CUSTOMER_FEEDBACK_ANALYSIS", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "SUPPORT_TRAINING", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "REMOTE_SUPPORT", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);
        put(CS, "CUSTOMER_SATISFACTION", CanonicalActionId.CUSTOMER_SUPPORT_OPERATIONS);

        // HealthFitnessActionType — all HEALTH_FITNESS
        for (String name : new String[]{"WORKOUT_PLANS", "NUTRITION_TRACKING", "MEDICAL_RECORD_MANAGEMENT", "FITNESS_GOAL_SETTING", "EXERCISE_PLANNING", "DIET_PLANNING", "WEIGHT_MANAGEMENT", "CARDIO_TRAINING", "STRENGTH_TRAINING", "FLEXIBILITY_TRAINING", "RECOVERY_PLANNING", "NUTRITION_PLANNING", "MEAL_PREP", "SUPPLEMENT_GUIDANCE", "HEALTH_SCREENING", "CHRONIC_DISEASE_MANAGEMENT", "MENTAL_HEALTH", "SLEEP_OPTIMIZATION", "STRESS_MANAGEMENT_HEALTH", "FITNESS_TRACKING", "WORKOUT_FORM_CORRECTION"}) {
            put(H, name, CanonicalActionId.HEALTH_FITNESS);
        }

        // CloudServicesActionType — all CLOUD_OPERATIONS
        for (String name : new String[]{"CLOUD_DEPLOYMENT", "CLOUD_SECURITY", "CLOUD_MONITORING", "CLOUD_MIGRATION", "AWS_ARCHITECTURE", "AZURE_SETUP", "GCP_CONFIGURATION", "CONTAINER_ORCHESTRATION", "SERVERLESS_ARCHITECTURE", "CLOUD_COST_OPTIMIZATION", "DISASTER_RECOVERY", "CLOUD_BACKUP", "LOAD_BALANCING", "AUTO_SCALING", "CLOUD_NETWORKING"}) {
            put(C, name, CanonicalActionId.CLOUD_OPERATIONS);
        }

        // DevOpsActionType
        put(O, "CI_CD_PIPELINE", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "AUTOMATED_TESTING", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "INTEGRATION_TESTING", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "INFRASTRUCTURE_AS_CODE", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "CONTINUOUS_INTEGRATION", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "CONTINUOUS_DEPLOYMENT", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "CONTINUOUS_DELIVERY", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "CONTAINERIZATION", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "DOCKER_SETUP", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "KUBERNETES_ORCHESTRATION", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "CONFIGURATION_MANAGEMENT", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "VERSION_CONTROL", CanonicalActionId.INFRASTRUCTURE_AS_CODE);
        put(O, "DEPLOYMENT_STRATEGY_DEVOPS", CanonicalActionId.STRATEGY);
        put(O, "MONITORING_ALERTING", CanonicalActionId.PERFORMANCE_OPTIMIZATION);
        put(O, "LOG_MANAGEMENT", CanonicalActionId.PERFORMANCE_OPTIMIZATION);
        put(O, "PERFORMANCE_MONITORING", CanonicalActionId.PERFORMANCE_OPTIMIZATION);
        put(O, "INFRASTRUCTURE_MONITORING", CanonicalActionId.PERFORMANCE_OPTIMIZATION);

        // CybersecurityActionType
        put(Y, "VULNERABILITY_SCANNING", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "PENETRATION_TESTING", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "THREAT_ANALYSIS", CanonicalActionId.THREAT_OR_INCIDENT);
        put(Y, "DATA_ENCRYPTION", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "SECURITY_AUDIT", CanonicalActionId.EVALUATION_OR_AUDIT);
        put(Y, "INCIDENT_RESPONSE", CanonicalActionId.THREAT_OR_INCIDENT);
        put(Y, "SECURITY_POLICY", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "ACCESS_CONTROL", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "IDENTITY_MANAGEMENT", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "NETWORK_SECURITY", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "APPLICATION_SECURITY", CanonicalActionId.SECURITY_IMPLEMENTATION);
        put(Y, "SECURITY_TRAINING", CanonicalActionId.EDUCATION_DESIGN);
        put(Y, "RISK_ASSESSMENT_SECURITY", CanonicalActionId.RISK_ASSESSMENT);
        put(Y, "COMPLIANCE_MANAGEMENT", CanonicalActionId.EVALUATION_OR_AUDIT);
        put(Y, "SECURITY_MONITORING", CanonicalActionId.THREAT_OR_INCIDENT);
        put(Y, "MALWARE_ANALYSIS", CanonicalActionId.THREAT_OR_INCIDENT);
        put(Y, "FIREWALL_CONFIGURATION", CanonicalActionId.SECURITY_IMPLEMENTATION);

        // AiMlActionType — all ML_MODEL_LIFECYCLE
        for (String name : new String[]{"MODEL_TRAINING", "PREDICTION", "DATA_PREPROCESSING", "MODEL_EVALUATION", "HYPERPARAMETER_TUNING", "FEATURE_ENGINEERING", "MODEL_SELECTION", "DEEP_LEARNING", "NEURAL_NETWORK_DESIGN", "TRANSFER_LEARNING", "REINFORCEMENT_LEARNING", "NATURAL_LANGUAGE_PROCESSING", "COMPUTER_VISION", "MODEL_DEPLOYMENT", "MODEL_MONITORING", "A_B_TESTING", "DATA_LABELING", "MODEL_OPTIMIZATION", "EXPLAINABLE_AI"}) {
            put(A, name, CanonicalActionId.ML_MODEL_LIFECYCLE);
        }
    }

    private static void put(String enumName, String constantName, CanonicalActionId canonical) {
        MAP.put(enumName + "." + constantName, canonical);
    }

    static CanonicalActionId canonicalFor(String enumName, String constantName) {
        return MAP.get(enumName + "." + constantName);
    }

    private ActionToCanonicalMapping() {}
}
