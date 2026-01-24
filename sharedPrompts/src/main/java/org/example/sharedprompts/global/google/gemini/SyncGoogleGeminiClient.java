package org.example.sharedprompts.global.google.gemini;

/**
 * 도메인 계층에서 사용할 동기식 Google Gemini 클라이언트.
 * 내부적으로는 리액티브 클라이언트(GoogleGeminiService)를 사용할 수 있지만,
 * 외부에는 동기 API만 노출하여 block 사용을 이 계층으로 한정한다.
 */
public interface SyncGoogleGeminiClient {

    String chatSync(String prompt);
}


