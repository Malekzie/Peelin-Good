package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.auth.AuthResponse;
import com.sait.peelin.dto.v1.auth.LoginRequest;
import com.sait.peelin.dto.v1.auth.RegisterRequest;
import com.sait.peelin.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register, log in, and obtain JWT tokens for API access")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Log in", description = "Authenticate with email and password. Returns a JWT token to use in the Authorization header.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest);
    }

    @Operation(summary = "Register", description = "Create a new customer account. Returns a JWT token immediately upon success.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created, token returned"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already in use", content = @Content)
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest);
    }

    @Operation(summary = "OAuth2 callback (not yet implemented)", description = "Placeholder for future Google/Microsoft OAuth2 login flow.")
    @ApiResponse(responseCode = "501", description = "Not implemented", content = @Content)
    @PostMapping("/oauth2/callback")
    public ResponseEntity<String> oauth2Callback() {
        // TODO: implement OAuth2 login (Google/Microsoft)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("OAuth2 login is scaffolded but not yet implemented");
    }
}
