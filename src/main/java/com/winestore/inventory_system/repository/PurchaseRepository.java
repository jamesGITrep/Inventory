package com.winestore.inventory_system.repository;

import com.winestore.inventory_system.model.PurchaseRecord;
import com.winestore.inventory_system.dto.PurchaseHistoryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PurchaseRepository extends JpaRepository<PurchaseRecord, Integer> {
    List<PurchaseRecord> findByPurchaseDate(LocalDate purchaseDate);

    @Query("SELECT new com.winestore.inventory_system.dto.PurchaseHistoryDTO(" +
           "pr.purchaseId, p.productId, p.productName, p.category, p.manufacturer, p.sizeMl, " +
           "pr.quantityBought, pr.purchasePrice, pr.purchaseDate) " +
           "FROM PurchaseRecord pr JOIN Product p ON pr.productId = p.productId " +
           "WHERE pr.purchaseDate BETWEEN :start AND :end " +
           "AND (:category IS NULL OR :category = '' OR p.category = :category)")
    List<PurchaseHistoryDTO> findPurchaseHistory(@Param("start") LocalDate start, 
                                                @Param("end") LocalDate end, 
                                                @Param("category") String category);
}