package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.BatchDto;
import com.sait.peelin.service.BatchQueryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Batches")
public class BatchController {

    private final BatchQueryService batchQueryService;

    @GetMapping("/bakeries/{bakeryId}/batches")
    public List<BatchDto> byBakery(
            @PathVariable Integer bakeryId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return batchQueryService.byBakery(bakeryId, activeOnly);
    }

    @GetMapping("/products/{productId}/batches")
    public List<BatchDto> byProduct(@PathVariable Integer productId) {
        return batchQueryService.byProduct(productId);
    }
}
