package com.winestore.inventory_system.model;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotRepository extends JpaRepository<InventorySnapshot, Integer> {}