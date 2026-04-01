package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.BakeryDto;
import com.sait.peelin.dto.v1.BakeryHourDto;
import com.sait.peelin.dto.v1.BakeryUpsertRequest;
import com.sait.peelin.service.BakeryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bakeries")
@RequiredArgsConstructor
@Tag(name = "Bakeries")
public class BakeryController {

    private final BakeryService bakeryService;

    @GetMapping
    public List<BakeryDto> list(@RequestParam(required = false) String search) {
        return bakeryService.list(search);
    }

    @GetMapping("/{id}")
    public BakeryDto get(@PathVariable Integer id) {
        return bakeryService.get(id);
    }

    @GetMapping("/{id}/hours")
    public List<BakeryHourDto> hours(@PathVariable Integer id) {
        return bakeryService.hours(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BakeryDto create(@Valid @RequestBody BakeryUpsertRequest req) {
        return bakeryService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BakeryDto update(@PathVariable Integer id, @Valid @RequestBody BakeryUpsertRequest req) {
        return bakeryService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        bakeryService.delete(id);
    }
}
