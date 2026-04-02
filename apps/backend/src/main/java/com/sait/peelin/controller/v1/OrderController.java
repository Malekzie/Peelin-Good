package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.*;
import com.sait.peelin.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> list() {
        return orderService.listForCurrentUser();
    }

    @GetMapping("/{id}")
    public OrderDto get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@Valid @RequestBody CheckoutRequest req) {
        return orderService.checkout(req);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PatchMapping("/{id}/status")
    public OrderDto patchStatus(@PathVariable UUID id, @Valid @RequestBody OrderStatusPatchRequest req) {
        return orderService.updateStatus(id, req);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PatchMapping("/{id}/delivered")
    public OrderDto markDelivered(@PathVariable UUID id, @RequestBody(required = false) OrderDeliveredPatchRequest req) {
        return orderService.markDelivered(id, req != null ? req : new OrderDeliveredPatchRequest());
    }

    @PatchMapping("/{id}/accept-delivery")
    public OrderDto acceptDelivery(@PathVariable UUID id) {
        return orderService.acceptDelivery(id);
    }
}
