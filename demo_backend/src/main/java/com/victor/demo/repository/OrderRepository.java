package com.victor.demo.repository;

import com.victor.demo.model.Order;
import com.victor.demo.model.OrderStatus;
import com.victor.demo.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // ── Existing queries ───────────────────────────────────────────────────────
    List<Order> findByPersonId(UUID personId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByPersonIdAndStatus(UUID personId, OrderStatus status);
    List<Order> findTop10ByOrderByOrderDateDesc();

    // ── Payment ────────────────────────────────────────────────────────────────
    long countByPaymentStatus(PaymentStatus paymentStatus);

    // ── Analytics (no date range — kept for backward compat) ──────────────────

    @Query("SELECT SUM(oi.quantity * oi.product.price) FROM Order o JOIN o.items oi WHERE o.paymentStatus = 'PAID'")
    Double sumRevenueForPaidOrders();

    @Query("SELECT COUNT(DISTINCT o.person.id) FROM Order o")
    long countDistinctCustomers();

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT o.destination, o.status, COUNT(o) FROM Order o WHERE o.destination IS NOT NULL GROUP BY o.destination, o.status ORDER BY o.destination")
    List<Object[]> countOrdersByDestinationAndStatus();

    @Query("SELECT oi.product.name, SUM(oi.quantity), SUM(oi.quantity * oi.product.price) FROM Order o JOIN o.items oi GROUP BY oi.product.name ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> topProducts();

    // ── Analytics WITH date range ─────────────────────────────────────────────

    @Query("SELECT SUM(oi.quantity * oi.product.price) FROM Order o JOIN o.items oi WHERE o.paymentStatus = 'PAID' AND o.orderDate >= :from AND o.orderDate < :to")
    Double sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderDate >= :from AND o.orderDate < :to")
    long countByOrderDateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = 'PAID' AND o.orderDate >= :from AND o.orderDate < :to")
    long countPaidBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT o.person.id) FROM Order o WHERE o.orderDate >= :from AND o.orderDate < :to")
    long countDistinctCustomersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT CAST(o.order_date AS date) AS day,
               SUM(oi.quantity * p.price)
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN product    p  ON p.id = oi.product_id
        WHERE o.payment_status = 'PAID'
          AND o.order_date >= :from AND o.order_date < :to
        GROUP BY day
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> revenueByDayBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT TO_CHAR(DATE_TRUNC('week', o.order_date), 'IYYY-"W"IW') AS week,
               SUM(oi.quantity * p.price)
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN product    p  ON p.id = oi.product_id
        WHERE o.payment_status = 'PAID'
          AND o.order_date >= :from AND o.order_date < :to
        GROUP BY DATE_TRUNC('week', o.order_date)
        ORDER BY DATE_TRUNC('week', o.order_date)
        """, nativeQuery = true)
    List<Object[]> revenueByWeekBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT TO_CHAR(DATE_TRUNC('month', o.order_date), 'YYYY-MM') AS month,
               SUM(oi.quantity * p.price)
        FROM orders o
        JOIN order_item oi ON oi.order_id = o.id
        JOIN product    p  ON p.id = oi.product_id
        WHERE o.payment_status = 'PAID'
          AND o.order_date >= :from AND o.order_date < :to
        GROUP BY DATE_TRUNC('month', o.order_date)
        ORDER BY DATE_TRUNC('month', o.order_date)
        """, nativeQuery = true)
    List<Object[]> revenueByMonthBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
