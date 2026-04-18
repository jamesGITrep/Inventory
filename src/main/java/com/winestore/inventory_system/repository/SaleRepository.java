package com.winestore.inventory_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.SaleRecord;

public interface SaleRepository extends JpaRepository<SaleRecord, Integer> {

// Spring JPA needs the method name to match the 'productId' field
    List<SaleRecord> findByProductIdAndSaleDate(Integer productId, LocalDate saleDate);    
}

