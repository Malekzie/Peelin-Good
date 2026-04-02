package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.RewardDto;
import com.sait.peelin.service.RewardQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Rewards")
public class RewardQueryController {

    private final RewardQueryService rewardQueryService;

    @GetMapping("/customers/{customerId}/rewards")
    @PreAuthorize("isAuthenticated()")
    public List<RewardDto> forCustomer(@PathVariable UUID customerId) {
        return rewardQueryService.listForCustomer(customerId);
    }

    @GetMapping("/rewards")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RewardDto> all() {
        return rewardQueryService.listAll();
    }
}
