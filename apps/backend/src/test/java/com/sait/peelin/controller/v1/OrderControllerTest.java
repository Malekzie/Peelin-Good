package com.sait.peelin.controller.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sait.peelin.dto.v1.CheckoutRequest;
import com.sait.peelin.dto.v1.CheckoutSessionResponse;
import com.sait.peelin.service.JwtService;
import com.sait.peelin.service.OrderService;
import com.sait.peelin.service.TokenDenylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = OrderController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ClientWebSecurityAutoConfiguration.class
    },
    excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.sait\\.peelin\\.security\\..*")
)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private TokenDenylistService tokenDenylistService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void checkout_ShouldReturnCreated() throws Exception {
        CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
        line.setProductId(101);
        line.setQuantity(1);

        CheckoutRequest req = new CheckoutRequest();
        req.setBakeryId(1);
        req.setOrderMethod(com.sait.peelin.model.OrderMethod.pickup);
        req.setPaymentMethod(com.sait.peelin.model.PaymentMethod.credit_card);
        req.setItems(java.util.List.of(line));

        CheckoutSessionResponse responseDto = new CheckoutSessionResponse(
                UUID.randomUUID(), "ORD-123", "pi_test_secret_123", "pi_test_id_123",
                new java.math.BigDecimal("10.00"), java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, new java.math.BigDecimal("0.50"),
                new java.math.BigDecimal("10.50"));
        when(orderService.checkout(any(CheckoutRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
