package com.sait.peelin.service;

import com.sait.peelin.dto.v1.CheckoutRequest;
import com.sait.peelin.dto.v1.GuestCustomerRequest;
import com.sait.peelin.dto.v1.CheckoutSessionResponse;
import com.sait.peelin.model.*;
import com.sait.peelin.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.stripe.model.PaymentIntent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RewardRepository rewardRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private BakeryRepository bakeryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private BatchRepository batchRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TaxRateRepository taxRateRepository;
    @Mock private CustomerService customerService;
    @Mock private CurrentUserService currentUserService;
    @Mock private StripeService stripeService;
    @Mock private StripePaymentFulfillmentService stripePaymentFulfillmentService;
    @Mock private RewardAccrualService rewardAccrualService;
    @Mock private RewardTierService rewardTierService;
    @Mock private RecommendationService recommendationService;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Customer customer;
    private Bakery bakery;
    private Product product;
    private RewardTier rewardTier;
    private TaxRate taxRate;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserRole(UserRole.customer);

        rewardTier = new RewardTier();
        rewardTier.setRewardTierDiscountRate(BigDecimal.valueOf(10));

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setUser(user);
        customer.setRewardTier(rewardTier);
        customer.setCustomerRewardBalance(100);
        Address address = new Address();
        address.setId(1);
        address.setAddressProvince("Ontario");
        customer.setAddress(address);

        bakery = new Bakery();
        bakery.setId(1);

        product = new Product();
        product.setId(101);
        product.setProductName("Test Bread");
        product.setProductBasePrice(BigDecimal.valueOf(5.0));

        taxRate = new TaxRate();
        taxRate.setProvinceName("Ontario");
        taxRate.setTaxPercent(BigDecimal.valueOf(13));

        lenient().when(stripeService.isConfigured()).thenReturn(true);
        lenient().when(rewardTierService.tierForBalance(100)).thenReturn(Optional.of(rewardTier));
    }

    @Test
    void checkout_SuccessfulCustomerOrder() throws Exception {
        // Arrange
        doReturn(user).when(currentUserService).currentUserOrNull();
        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(customer));
        when(bakeryRepository.findById(1)).thenReturn(Optional.of(bakery));
        when(productRepository.findById(101)).thenReturn(Optional.of(product));
        when(taxRateRepository.findByProvinceNameIgnoreCase("Ontario")).thenReturn(Optional.of(taxRate));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        try {
            when(stripeService.createPaymentIntent(any(UUID.class), any(BigDecimal.class))).thenReturn(mockIntent);
            when(mockIntent.getId()).thenReturn("pi_test_id");
            when(mockIntent.getClientSecret()).thenReturn("pi_test_secret");
        } catch (Exception ignored) {}

        CheckoutRequest req = new CheckoutRequest();
        req.setBakeryId(1);
        req.setOrderMethod(OrderMethod.pickup);
        req.setPaymentMethod(PaymentMethod.credit_card);

        CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
        line.setProductId(101);
        line.setQuantity(2);
        req.setItems(List.of(line));

        // Act
        CheckoutSessionResponse result = orderService.checkout(req);

        // Assert
        assertNotNull(result);
        assertNotNull(result.orderId());
        assertNotNull(result.orderNumber());
        verify(orderItemRepository, atLeastOnce()).save(any(OrderItem.class));

        // subtotal 10.0, 10% tier discount = 1.0, pre-tax = 9.0, tax 13% = 1.17, grand = 10.17
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(0, BigDecimal.valueOf(9.00).compareTo(savedOrder.getOrderTotal()));
        assertEquals(0, BigDecimal.valueOf(1.00).compareTo(savedOrder.getOrderDiscount()));
        assertEquals(0, BigDecimal.valueOf(13.0).compareTo(savedOrder.getOrderTaxRate()));
        assertEquals(0, BigDecimal.valueOf(1.17).compareTo(savedOrder.getOrderTaxAmount()));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(10.17).compareTo(paymentCaptor.getValue().getPaymentAmount()));

        verify(stripeService).createPaymentIntent(any(UUID.class),
                argThat(amt -> amt.compareTo(BigDecimal.valueOf(10.17)) == 0));
    }

    @Test
    void checkout_FailsIfDeliveryMissingAddress() {
        // Arrange
        doReturn(user).when(currentUserService).currentUserOrNull();
        customer.setAddress(null);
        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(customer));
        when(bakeryRepository.findById(1)).thenReturn(Optional.of(bakery));

        CheckoutRequest req = new CheckoutRequest();
        req.setBakeryId(1);
        req.setOrderMethod(OrderMethod.delivery); // Delivery!
        req.setAddressId(null); // Missing!

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> orderService.checkout(req));
    }

    @Test
    void checkout_StaffPlacingOrderForCustomer() throws Exception {
        // Arrange
        User admin = new User();
        admin.setUserId(UUID.randomUUID());
        admin.setUserRole(UserRole.admin);

        doReturn(admin).when(currentUserService).currentUserOrNull();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(bakeryRepository.findById(1)).thenReturn(Optional.of(bakery));
        when(productRepository.findById(101)).thenReturn(Optional.of(product));
        when(taxRateRepository.findByProvinceNameIgnoreCase("Ontario")).thenReturn(Optional.of(taxRate));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        try {
            when(stripeService.createPaymentIntent(any(UUID.class), any(BigDecimal.class))).thenReturn(mockIntent);
            when(mockIntent.getId()).thenReturn("pi_test_id");
            when(mockIntent.getClientSecret()).thenReturn("pi_test_secret");
        } catch (Exception ignored) {}

        CheckoutRequest req = new CheckoutRequest();
        req.setBakeryId(1);
        req.setCustomerId(customer.getId()); // Providing customer ID
        req.setOrderMethod(OrderMethod.pickup);
        req.setPaymentMethod(PaymentMethod.cash);

        CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
        line.setProductId(101);
        line.setQuantity(1);
        req.setItems(List.of(line));

        // Act
        CheckoutSessionResponse result = orderService.checkout(req);

        // Assert
        assertNotNull(result);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void checkout_GuestReuseDoesNotApplyTierDiscount() throws Exception {
        GuestCustomerRequest guest = new GuestCustomerRequest();
        guest.setFirstName("Jamie");
        guest.setLastName("Guest");
        guest.setEmail("jamie@example.com");
        guest.setPhone("4035551212");
        guest.setAddressLine1("123 Main St");
        guest.setCity("Calgary");
        guest.setProvince("Ontario");
        guest.setPostalCode("T2T2T2");

        Customer guestCustomer = new Customer();
        guestCustomer.setId(UUID.randomUUID());
        guestCustomer.setRewardTier(rewardTier);
        guestCustomer.setCustomerRewardBalance(0);
        Address guestAddress = new Address();
        guestAddress.setAddressProvince("Ontario");
        guestCustomer.setAddress(guestAddress);

        doReturn(null).when(currentUserService).currentUserOrNull();
        when(customerService.resolveOrCreateGuestCustomer(guest)).thenReturn(guestCustomer);
        when(bakeryRepository.findById(1)).thenReturn(Optional.of(bakery));
        when(productRepository.findById(101)).thenReturn(Optional.of(product));
        when(taxRateRepository.findByProvinceNameIgnoreCase("Ontario")).thenReturn(Optional.of(taxRate));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        PaymentIntent mockIntent = mock(PaymentIntent.class);
        try {
            when(stripeService.createPaymentIntent(any(UUID.class), any(BigDecimal.class))).thenReturn(mockIntent);
            when(mockIntent.getId()).thenReturn("pi_test_id");
            when(mockIntent.getClientSecret()).thenReturn("pi_test_secret");
        } catch (Exception ignored) {}

        CheckoutRequest req = new CheckoutRequest();
        req.setGuest(guest);
        req.setBakeryId(1);
        req.setOrderMethod(OrderMethod.pickup);
        req.setPaymentMethod(PaymentMethod.credit_card);

        CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
        line.setProductId(101);
        line.setQuantity(2);
        req.setItems(List.of(line));

        CheckoutSessionResponse result = orderService.checkout(req);

        assertNotNull(result);
        assertNotNull(result.orderId());
        assertNotNull(result.orderNumber());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(0, BigDecimal.valueOf(10.00).compareTo(savedOrder.getOrderTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(savedOrder.getOrderDiscount()));
        assertEquals(0, BigDecimal.valueOf(1.30).compareTo(savedOrder.getOrderTaxAmount()));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(0, BigDecimal.valueOf(11.30).compareTo(paymentCaptor.getValue().getPaymentAmount()));

        verify(stripeService).createPaymentIntent(any(UUID.class),
                argThat(amt -> amt.compareTo(BigDecimal.valueOf(11.30)) == 0));
        verify(customerService).resolveOrCreateGuestCustomer(guest);
    }

    @Test
    void checkout_SkipsStripeWhenNotConfigured() throws Exception {
        when(stripeService.isConfigured()).thenReturn(false);

        doReturn(user).when(currentUserService).currentUserOrNull();
        when(customerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(customer));
        when(bakeryRepository.findById(1)).thenReturn(Optional.of(bakery));
        when(productRepository.findById(101)).thenReturn(Optional.of(product));
        when(taxRateRepository.findByProvinceNameIgnoreCase("Ontario")).thenReturn(Optional.of(taxRate));
        UUID orderUuid = UUID.randomUUID();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(orderUuid);
            return o;
        });

        CheckoutRequest req = new CheckoutRequest();
        req.setBakeryId(1);
        req.setOrderMethod(OrderMethod.pickup);
        req.setPaymentMethod(PaymentMethod.credit_card);
        CheckoutRequest.CheckoutLineRequest line = new CheckoutRequest.CheckoutLineRequest();
        line.setProductId(101);
        line.setQuantity(1);
        req.setItems(List.of(line));

        CheckoutSessionResponse result = orderService.checkout(req);

        assertNotNull(result);
        assertTrue(result.clientSecret().startsWith("dev_pi_" + orderUuid));
        verify(stripeService, never()).createPaymentIntent(any(), any());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals("dev_pi_" + orderUuid, paymentCaptor.getValue().getStripeSessionId());
    }
}
