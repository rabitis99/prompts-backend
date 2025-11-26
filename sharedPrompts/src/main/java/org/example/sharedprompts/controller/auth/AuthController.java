package org.example.sharedprompts.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sharedprompts.domain.auth.service.AuthService;
import org.example.sharedprompts.dto.auth.request.LoginRequestDto;
import org.example.sharedprompts.dto.auth.request.SignUpRequestDto;
import org.example.sharedprompts.dto.auth.response.AuthResponseDto;
import org.example.sharedprompts.dto.auth.response.TokenResponseDto;
import org.example.sharedprompts.global.response.CustomResponse;
import org.example.sharedprompts.global.response.CustomResponseHelper;
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
            @Valid @RequestBody LoginRequestDto dto
    ){
        return CustomResponseHelper.ok(authService.login(dto));
    }

    @GetMapping("/callback")
    public ResponseEntity<CustomResponse<TokenResponseDto>> callback(
            @RequestParam("code") String code,
            @RequestParam(value = "state", required = false) String state
    ){
        return CustomResponseHelper.ok(authService.callback(code, state));
    }
}
