package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.auth.AuthResponse;
import com.sait.peelin.dto.v1.auth.LoginRequest;
import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt.expiration:864000000}")
    private long jwtExpiration;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    private void setTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofMillis(jwtExpiration))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(loginRequest);

        setTokenCookie(response, authResponse.getToken());

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(registerRequest);

        setTokenCookie(response, authResponse.getToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/callback")
    public ResponseEntity<String> oauth2Callback() {
        // TODO: implement OAuth2 login (Google/Microsoft)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("OAuth2 login is scaffolded but not yet implemented");
    }
}
