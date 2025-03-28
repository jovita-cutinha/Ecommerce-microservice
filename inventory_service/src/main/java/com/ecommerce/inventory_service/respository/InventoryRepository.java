package com.ecommerce.inventory_service.respository;

import com.ecommerce.inventory_service.model.Inventory;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    void deleteByProductId(String productId);

    Inventory findByProductId(String productId);
}
