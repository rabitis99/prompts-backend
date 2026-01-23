package org.example.sharedprompts.auth.oauth.mapper.model;

import lombok.Builder;
import lombok.Value;
import org.example.sharedprompts.domain.user.enums.Provider;
import org.example.sharedprompts.global.exception.ApiException;
import org.example.sharedprompts.global.exception.ErrorCode;

/**
 * OAuth2 사용자 속성
 * 
 * Provider별로 추출한 사용자 정보를 담는 값 객체
 * Builder 패턴을 사용하여 필수 필드 검증을 수행합니다.
 */
@Value
@Builder
public class OAuth2UserAttributes {
    String providerId;
    String email;
    String nickname;
    String imageUrl;
    Provider provider;

    /**
     * Builder를 통한 검증된 객체 생성
     */
    public static OAuth2UserAttributesBuilder builder() {
        return new OAuth2UserAttributesBuilder();
    }

    /**
     * Builder 내부 클래스 - 검증 로직 포함
     */
    public static class OAuth2UserAttributesBuilder {
        private String providerId;
        private String email;
        private String nickname;
        private String imageUrl;
        private Provider provider;

        public OAuth2UserAttributesBuilder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public OAuth2UserAttributesBuilder email(String email) {
            this.email = email;
            return this;
        }

        public OAuth2UserAttributesBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public OAuth2UserAttributesBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public OAuth2UserAttributesBuilder provider(Provider provider) {
            this.provider = provider;
            return this;
        }

        /**
         * 필수 필드 검증 후 객체 생성
         */
        public OAuth2UserAttributes build() {
            validate();
            return new OAuth2UserAttributes(providerId, email, nickname, imageUrl, provider);
        }

        /**
         * 필수 필드 검증
         * - providerId: 필수 (모든 Provider)
         * - provider: 필수
         */
        private void validate() {
            if (provider == null) {
                throw new ApiException(
                        ErrorCode.OAUTH2_PROVIDER_REQUIRED,
                        "OAuth2 Provider는 필수입니다."
                );
            }

            if (providerId == null || providerId.isBlank()) {
                throw new ApiException(
                        ErrorCode.OAUTH2_PROVIDER_ID_MISSING,
                        String.format("OAuth2 Provider ID는 필수입니다. Provider: %s", provider)
                );
            }
        }
    }
}
