package com.invoiceflow.auth;

import com.invoiceflow.common.config.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        return respondWithTokens(result, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return respondWithTokens(result, HttpStatus.OK);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = extractRefreshCookie(request)
                .orElseThrow(() -> new InvalidCredentialsException("Missing refresh token"));

        AuthService.AuthResult result = authService.refresh(rawRefreshToken);
        return respondWithTokens(result, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal != null) {
            authService.logout(principal.id());
        }
        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    private ResponseEntity<AuthResponse> respondWithTokens(AuthService.AuthResult result, HttpStatus status) {
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, result.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(java.time.Duration.ofDays(jwtProperties.refreshTokenTtlDays()))
                .build();

        AuthResponse body = new AuthResponse(
                result.accessToken(),
                result.user().getId(),
                result.user().getEmail(),
                result.user().getName(),
                result.organization().getId(),
                result.role().name()
        );

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    private java.util.Optional<String> extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return java.util.Optional.empty();
        }
        return java.util.Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_COOKIE_NAME.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst();
    }
}
