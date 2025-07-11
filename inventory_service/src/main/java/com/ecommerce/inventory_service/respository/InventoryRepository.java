package com.ecommerce.inventory_service.respository;

import com.ecommerce.inventory_service.model.Inventory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    void deleteByProductId(String productId);

    Inventory findByProductId(String productId);

    List<Inventory> findBySellerIdAndAvailableQuantityLessThanEqual(UUID sellerId, int threshold, Pageable pageable);
}
