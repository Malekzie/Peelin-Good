package com.sait.peelin.dto.v1;

import com.sait.peelin.model.OrderMethod;
import com.sait.peelin.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequest {
    /** When set, admin/employee places the order for this customer (instead of self-checkout). */
    private UUID customerId;

    /** Optional manual discount (e.g. staff override); otherwise reward-tier discount applies. */
    private BigDecimal manualDiscount;

    /** Required for unauthenticated guest checkout. */
    @Valid
    private GuestCustomerRequest guest;

    @NotNull
    private Integer bakeryId;
    @NotNull
    private OrderMethod orderMethod;
    private Integer addressId;
    private String comment;
    private OffsetDateTime scheduledAt;
    @NotNull
    private PaymentMethod paymentMethod;
    @NotEmpty
    @Valid
    private List<CheckoutLineRequest> items;

    @Data
    public static class CheckoutLineRequest {
        @NotNull
        private Integer productId;
        @NotNull
        @Positive
        private Integer quantity;
        private Integer batchId;
    }
}
