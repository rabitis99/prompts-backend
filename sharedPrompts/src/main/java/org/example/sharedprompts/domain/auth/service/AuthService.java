package org.example.sharedprompts.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;

public interface AuthService {
    /**
 * Creates a new user account using the provided signup details and returns authentication response data.
 *
 * @param dto signup details such as credentials and profile information
 * @return an AuthResponseDto containing authentication-related response data (for example, tokens and user information)
 */
AuthResponseDto signUp(SignUpRequestDto dto);
    /**
 * Authenticates a user using supplied credentials and request context.
 *
 * @param dto     the login credentials and related authentication data
 * @param request the HTTP request providing contextual metadata (for example headers or client IP)
 * @return        a TokenResponseDto containing authentication tokens and related response data
 */
TokenResponseDto login(LoginRequestDto dto, HttpServletRequest request);
    /**
 * Handles an authentication callback and exchanges the provided callback data for authentication tokens.
 *
 * @param key         provider-specific callback identifier (e.g., authorization code or provider key)
 * @param state       state parameter to validate the authentication flow
 * @param deviceToken optional device token to associate the resulting tokens with a device
 * @param request     HTTP servlet request providing request context (headers, remote address, etc.)
 * @return            a TokenResponseDto containing access/refresh tokens and related authentication metadata
 */
TokenResponseDto callback(String key, String state, String deviceToken, HttpServletRequest request);
    /**
 * Refreshes authentication tokens using the provided refresh token details.
 *
 * @param dto     contains the refresh token and any additional data required to obtain new tokens
 * @param request HTTP request context (e.g., headers, client IP) used for validation or auditing
 * @return        a TokenResponseDto containing new access (and optionally refresh) tokens and related metadata
 */
TokenResponseDto refresh(RefreshRequestDto dto, HttpServletRequest request);
    /**
 * Logs out the specified user and revokes their authentication state.
 *
 * @param userId      the identifier of the user to log out
 * @param dto         logout request details (e.g., device or session info)
 * @param accessToken the access token to be invalidated or verified during logout
 */
void logout(Long userId, LogoutRequestDto dto, String accessToken);
}