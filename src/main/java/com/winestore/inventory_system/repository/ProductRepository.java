package com.winestore.inventory_system.repository;

import com.winestore.inventory_system.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    // Only fetch products that aren't marked as typo duplicates [cite: 161]
    List<Product> findByIsActiveTrue();

    List<Product> findByProductNameContainingIgnoreCaseAndIsActiveTrue(String name);

}