package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.auth.ChangePasswordRequest;
import com.sait.peelin.service.AuthService;
import com.sait.peelin.service.CustomerService;
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
@Tag(name = "Account")
public class AccountController {

    private final AuthService authService;
    private final CustomerService customerService;

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
    }

    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CustomerDto uploadProfilePhoto(@RequestParam("photo") MultipartFile photo) {
        return customerService.uploadMyProfilePhoto(photo);
    }
}
