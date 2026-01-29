package org.example.sharedprompts.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.AuthUser;
import org.example.sharedprompts.domain.auth.CurrentUser;
import org.example.sharedprompts.domain.auth.service.AuthService;
import org.example.sharedprompts.dto.auth.request.ConfirmRequestDto;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.LogoutRequestDto;
import org.example.sharedprompts.dto.auth.request.RefreshRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.dto.common.CustomResponse;
import org.example.sharedprompts.dto.common.CustomResponseHelper;
import org.example.sharedprompts.global.util.JwtTokenExtractor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<CustomResponse<AuthResponseDto>> signup(
            @Valid @RequestBody SignUpRequestDto dto
    ) {
        return CustomResponseHelper.created(authService.signUp(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<CustomResponse<TokenResponseDto>> login(
            @Valid @RequestBody LoginRequestDto dto,
            HttpServletRequest request
    ){
        return CustomResponseHelper.ok(authService.login(dto, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<CustomResponse<TokenResponseDto>> confirm(
            @Valid @RequestBody ConfirmRequestDto dto,
            HttpServletRequest request
    ) {
        return CustomResponseHelper.ok(authService.confirm(dto, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<CustomResponse<TokenResponseDto>> refresh(
            @Valid @RequestBody RefreshRequestDto dto,
            HttpServletRequest request
    ){
        return CustomResponseHelper.ok(authService.refresh(dto, request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CurrentUser AuthUser authUser,
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody LogoutRequestDto dto
    ){
        String accessToken = JwtTokenExtractor.extractAccessToken(authorizationHeader);
        authService.logout(authUser.getId(), dto, accessToken);
        return CustomResponseHelper.noContent();
    }
}
