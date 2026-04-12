package com.sait.peelin.dto.v1;

import com.sait.peelin.model.OrderMethod;
import com.sait.peelin.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CheckoutRequest {
    /** When set, admin/employee places the order for this customer (instead of self-checkout). */
    private UUID customerId;

    /** Optional manual discount (e.g. staff override); otherwise reward-tier discount applies. */
    private BigDecimal manualDiscount;

    /**
     * Client-local calendar date for today’s product special (ISO yyyy-MM-dd). Defaults to America/Edmonton
     * when omitted so server pricing aligns with bakery operations.
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate pricingLocalDate;

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

    /** One-time delivery address for this order (overrides addressId and saved customer address). */
    @Valid
    private InlineAddressRequest deliveryAddress;

    @Data
    public static class InlineAddressRequest {
        @NotNull
        @Size(max = 120)
        private String line1;
        @Size(max = 120)
        private String line2;
        @NotNull
        @Size(max = 120)
        private String city;
        @NotNull
        @Size(max = 80)
        private String province;
        @NotNull
        @Size(max = 7)
        private String postalCode;
    }

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
