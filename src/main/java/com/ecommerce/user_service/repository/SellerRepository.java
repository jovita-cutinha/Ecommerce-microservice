package com.ecommerce.user_service.repository;

import com.ecommerce.user_service.model.Seller;
import com.ecommerce.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByKeycloakId(String keycloakId);
}
