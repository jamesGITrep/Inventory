package com.winestore.inventory_system.repository;

import com.winestore.inventory_system.model.PurchaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseRecord, Integer> {
    // This allows you to save "Stock Inward" entries
}