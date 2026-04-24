package com.winestore.inventory_system.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.winestore.inventory_system.model.SaleRecord;

public interface SaleRepository extends JpaRepository<SaleRecord, Integer> {

    List<SaleRecord> findByProductIdAndSaleDate(Integer productId, LocalDate saleDate);

    List<SaleRecord> findBySaleDate(LocalDate saleDate);

    List<SaleRecord> findBySaleDateBetween(LocalDate startDate, LocalDate endDate);
}

