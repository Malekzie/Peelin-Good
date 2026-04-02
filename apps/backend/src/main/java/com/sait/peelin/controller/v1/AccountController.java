package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.auth.ChangePasswordRequest;
import com.sait.peelin.service.AuthService;
import com.sait.peelin.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.sait.peelin.dto.v1.CustomerDto;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Manage the currently authenticated user's account settings")
@SecurityRequirement(name = "bearer-jwt")
public class AccountController {

    private final AuthService authService;
    private final CustomerService customerService;

    @Operation(summary = "Change password", description = "Update the authenticated user's password. Requires current password for verification.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or incorrect current password", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    @Operation(summary = "Upload profile photo", description = "Upload a profile photo for the authenticated customer. The photo is stored and set to pending review before being publicly visible.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo uploaded, customer record returned with updated photo URL"),
            @ApiResponse(responseCode = "400", description = "Invalid file type or size", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomerDto uploadProfilePhoto(@RequestParam("photo") MultipartFile photo) {
        return customerService.uploadMyProfilePhoto(photo);
    }
}
