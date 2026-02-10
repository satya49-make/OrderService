package com.order.repository;

import com.order.entity.Product;
import com.order.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(Long productId);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByProductNameContainingIgnoreCase(String productName);
}

