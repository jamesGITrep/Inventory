package com.winestore.inventory_system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.winestore.inventory_system.model.SaleRecord;

public interface SaleRepository extends JpaRepository<SaleRecord, Integer> {}