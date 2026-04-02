package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.ProductSpecialTodayDto;
import com.sait.peelin.service.ProductSpecialService;
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
@Tag(name = "Product specials")
public class ProductSpecialController {

    private final ProductSpecialService productSpecialService;

    /**
     * Featured product for a calendar day (device should pass local "today" as {@code date}).
     * When no {@code product_special} row exists, {@code productId} is null.
     */
    @GetMapping("/today")
    public ProductSpecialTodayDto today(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate d = date != null ? date : LocalDate.now();
        return productSpecialService.findFirstForDate(d);
    }
}
