package com.inventory.system.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.inventory.system.entity.InventorySnapshot;

@Repository
public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, Integer> {
    // Find a snapshot for a specific product on a specific date
    Optional<InventorySnapshot> findByProduct_ProductIdAndRecordDate(Integer productId, LocalDate date);
}