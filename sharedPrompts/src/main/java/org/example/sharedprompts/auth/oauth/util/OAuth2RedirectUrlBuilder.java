package org.example.sharedprompts.auth.oauth.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 리다이렉트 URL 생성 유틸리티
 * 
 * <p>OAuth2 인증 성공 후 프론트엔드로 리다이렉트할 URL을 생성합니다.
 * 
 * <p>모든 메서드는 static이므로 인스턴스화를 방지합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OAuth2RedirectUrlBuilder {

    private static final String KEY_PARAM = "key";
    private static final String STATE_PARAM = "state";

    /**
     * OAuth2 콜백 리다이렉트 URL을 생성합니다.
     * 
     * <p>형식: {frontUrl}?key={encodedKey}&state={encodedState}
     * 
     * @param frontUrl 프론트엔드 URL
     * @param tempKey 임시 토큰 키
     * @param state OAuth2 State 값
     * @return 리다이렉트 URL
     */
    public static String buildCallbackUrl(String frontUrl, String tempKey, String state) {
        StringBuilder url = new StringBuilder(frontUrl);
        
        // 첫 번째 파라미터는 ?로 시작
        url.append("?");
        url.append(KEY_PARAM).append("=").append(encode(tempKey));
        url.append("&");
        url.append(STATE_PARAM).append("=").append(encode(state));
        
        return url.toString();
    }

    /**
     * URL 인코딩을 수행합니다.
     * 
     * @param value 인코딩할 값
     * @return URL 인코딩된 값
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}


