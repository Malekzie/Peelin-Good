package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ProductSpecialTodayDto;
import com.sait.peelin.service.ProductSpecialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/product-specials")
@RequiredArgsConstructor
@Tag(name = "Product specials", description = "Daily featured product promotions")
public class ProductSpecialController {

    private final ProductSpecialService productSpecialService;

    @Operation(
            summary = "Get today's special",
            description = "Returns the featured product for the given calendar date. Pass the client's local date via `date` (ISO format, e.g. 2024-06-01); defaults to the server's current date. `productId` is null when no special is configured for that day."
    )
    @ApiResponse(responseCode = "200", description = "Today's special returned (productId may be null if none configured)")
    @GetMapping("/today")
    public ProductSpecialTodayDto today(
            @Parameter(description = "Calendar date in ISO format (yyyy-MM-dd). Defaults to today's server date.", example = "2024-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate d = date != null ? date : LocalDate.now();
        return productSpecialService.findFirstForDate(d);
    }
}
