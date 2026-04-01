package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.CustomerDto;
import com.sait.peelin.dto.v1.CustomerPatchRequest;
import com.sait.peelin.service.CustomerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers/me")
@RequiredArgsConstructor
@Tag(name = "Customer profile")
public class CustomerSelfController {

    private final CustomerService customerService;

    @GetMapping
    public CustomerDto me() {
        return customerService.me();
    }

    @PatchMapping
    public CustomerDto patch(@Valid @RequestBody CustomerPatchRequest req) {
        return customerService.patchMe(req);
    }
}
