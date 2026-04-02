package com.sait.peelin.controller.v1;

import com.sait.peelin.dto.v1.*;
import com.sait.peelin.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Orders", description = "Place and manage orders. Status updates require ADMIN or EMPLOYEE role.")
@SecurityRequirement(name = "bearer-jwt")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "List my orders", description = "Returns all orders belonging to the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping
    public List<OrderDto> list() {
        return orderService.listForCurrentUser();
    }

    @Operation(summary = "Get order", description = "Returns a single order by ID. Customers may only retrieve their own orders.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "403", description = "Order belongs to another customer", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    @GetMapping("/{id}")
    public OrderDto get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @Operation(summary = "Checkout", description = "Place a new order. Cart items, delivery method, and bakery are included in the request body.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or insufficient stock", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto checkout(@Valid @RequestBody CheckoutRequest req) {
        return orderService.checkout(req);
    }

    @Operation(summary = "Update order status", description = "Change the status of an order (e.g. PREPARING → READY). Requires ADMIN or EMPLOYEE role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PatchMapping("/{id}/status")
    public OrderDto patchStatus(@PathVariable UUID id, @Valid @RequestBody OrderStatusPatchRequest req) {
        return orderService.updateStatus(id, req);
    }

    @Operation(summary = "Mark order delivered", description = "Mark an order as delivered, optionally recording delivery notes. Requires ADMIN or EMPLOYEE role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order marked as delivered"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content),
            @ApiResponse(responseCode = "404", description = "Order not found", content = @Content)
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @PatchMapping("/{id}/delivered")
    public OrderDto markDelivered(@PathVariable UUID id, @RequestBody(required = false) OrderDeliveredPatchRequest req) {
        return orderService.markDelivered(id, req != null ? req : new OrderDeliveredPatchRequest());
    }
}
