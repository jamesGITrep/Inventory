package com.winestore.inventory_system.repository;

import com.winestore.inventory_system.model.ViewLastPurchased;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfitRepository extends JpaRepository<ViewLastPurchased, Integer> {
    // This allows to fetch the most recent purchase price for any wine
}