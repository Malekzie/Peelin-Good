package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.RewardTierDto;
import com.sait.peelin.dto.v1.RewardTierUpsertRequest;
import com.sait.peelin.service.RewardTierService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reward-tiers")
@RequiredArgsConstructor
@Tag(name = "Reward tiers")
public class RewardTierController {

    private final RewardTierService rewardTierService;

    @GetMapping
    public List<RewardTierDto> list() {
        return rewardTierService.list();
    }

    @GetMapping("/{id}")
    public RewardTierDto get(@PathVariable Integer id) {
        return rewardTierService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RewardTierDto create(@Valid @RequestBody RewardTierUpsertRequest req) {
        return rewardTierService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RewardTierDto update(@PathVariable Integer id, @Valid @RequestBody RewardTierUpsertRequest req) {
        return rewardTierService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        rewardTierService.delete(id);
    }
}
