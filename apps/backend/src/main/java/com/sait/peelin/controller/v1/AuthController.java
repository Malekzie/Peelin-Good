package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.auth.*;
import com.sait.peelin.service.AuthService;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.PasswordResetService;
import com.sait.peelin.service.TokenDenylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, log in, and obtain JWT tokens for API access")
public class AuthController {

    private final AuthService authService;
    private final TokenDenylistService tokenDenylistService;
    private final PasswordResetService passwordResetService;
    private final JwtService jwtService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

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

    @Operation(summary = "Register", description = "Create a new customer account. Returns a JWT token immediately upon success.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created, token returned"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already in use", content = @Content)
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(registerRequest);

        setTokenCookie(response, authResponse.getToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @Operation(summary = "Log out", description = "Invalidates the current JWT token. The token is added to a denylist and will be rejected on subsequent requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenDenylistService.deny(authHeader.substring(7));
        }

        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "OAuth2 callback (not yet implemented)", description = "Placeholder for future Google/Microsoft OAuth2 login flow.")
    @ApiResponse(responseCode = "501", description = "Not implemented", content = @Content)
    @PostMapping("/oauth2/callback")
    public ResponseEntity<String> oauth2Callback() {
        // TODO: implement OAuth2 login (Google/Microsoft)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("OAuth2 login is scaffolded but not yet implemented");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/oauth2/success")
    public void oauth2Success(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String token = (String) request.getSession().getAttribute("oauth2_pending_token");
        if (token == null) {
            response.sendRedirect(frontendUrl + "/login?error=oauth_failed");
            return;
        }
        request.getSession().removeAttribute("oauth2_pending_token");
        setTokenCookie(response, token);
        AuthResponse auth = authService.getUserInfoFromToken(token);
        String q = "username=" + URLEncoder.encode(auth.getUsername(), StandardCharsets.UTF_8)
                + "&role=" + URLEncoder.encode(auth.getRole(), StandardCharsets.UTF_8)
                + "&userId=" + URLEncoder.encode(String.valueOf(auth.getUserId()), StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/auth/callback?" + q);
    }
}
