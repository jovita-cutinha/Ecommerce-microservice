package com.ecommerce.order_service.repository;

import com.ecommerce.order_service.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserKeycloakIdOrderByOrderDateDesc(String userKeycloakId, Pageable pageable);

    Page<Order> findByStatus(String status, Pageable pageable);

    List<Order> findByUserKeycloakIdAndStatusOrderByOrderDateDesc(String userKeycloakId, String status, Pageable pageable);
}
