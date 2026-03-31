package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.AddressCreateRequest;
import com.sait.peelin.dto.v1.AddressSummaryDto;
import com.sait.peelin.service.AddressAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/addresses")
@RequiredArgsConstructor
@Tag(name = "Admin addresses")
public class AdminAddressController {

    private final AddressAdminService addressAdminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AddressSummaryDto> list() {
        return addressAdminService.listAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> create(@Valid @RequestBody AddressCreateRequest req) {
        return Map.of("id", addressAdminService.create(req));
    }
}
