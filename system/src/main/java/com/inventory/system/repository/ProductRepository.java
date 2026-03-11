package com.inventory.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventory.system.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    // For Requirement: "Search products by few initial letters"
    List<Product> findByProductNameStartingWithIgnoreCase(String prefix);

    // For Requirement: "Low stock alerts"
    List<Product> findByCurrentStockLessThanEqual(Integer threshold);
}