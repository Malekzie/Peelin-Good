package com.sait.peelin.repository;

import com.sait.peelin.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrder_Id(UUID orderId);

    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            WHERE oi.order.customer.id = :customerId
              AND oi.product.id = :productId
            """)
    boolean existsPurchasedByCustomer(@Param("customerId") UUID customerId, @Param("productId") Integer productId);
}
