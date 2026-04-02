package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.BatchDto;
import com.sait.peelin.service.BatchQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Batches")
public class BatchController {

    private final BatchQueryService batchQueryService;

    /** Public read: storefront "available here" at a bakery (no staff role required). */
    @GetMapping("/bakeries/{bakeryId}/batches")
    public List<BatchDto> byBakery(
            @PathVariable Integer bakeryId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return batchQueryService.byBakery(bakeryId, activeOnly);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @GetMapping("/products/{productId}/batches")
    public List<BatchDto> byProduct(@PathVariable Integer productId) {
        return batchQueryService.byProduct(productId);
    }
}
