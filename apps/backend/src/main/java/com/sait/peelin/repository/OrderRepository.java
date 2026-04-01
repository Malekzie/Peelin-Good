package com.sait.peelin.repository;

import com.sait.peelin.model.Order;
import com.sait.peelin.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomer_User_UserIdOrderByOrderPlacedDatetimeDesc(UUID userId);

    List<Order> findByBakery_IdOrderByOrderPlacedDatetimeDesc(Integer bakeryId);

    @Query("SELECT o FROM Order o WHERE o.orderPlacedDatetime >= :start AND o.orderPlacedDatetime < :end")
    List<Order> findByPlacedBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    long countByOrderStatus(OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.orderTotal - o.orderDiscount), 0) FROM Order o WHERE o.orderStatus IN (:s1, :s2)")
    BigDecimal sumRevenueForStatuses(@Param("s1") OrderStatus s1, @Param("s2") OrderStatus s2);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus IN (:s1, :s2)")
    long countByOrderStatusIn(@Param("s1") OrderStatus s1, @Param("s2") OrderStatus s2);
}
