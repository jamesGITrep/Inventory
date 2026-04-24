package com.winestore.inventory_system.repository;

import com.winestore.inventory_system.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Integer> {

    /**
     * Finds the single most recently created batch for the given product.
     * "Most recent" is determined by the highest batch_id (auto-increment).
     */
    @Query("SELECT pb FROM ProductBatch pb WHERE pb.productId = :productId " +
           "ORDER BY pb.batchId DESC")
    Optional<ProductBatch> findLatestBatchByProductId(@Param("productId") Integer productId);

    /**
     * FIFO Finder: returns the OLDEST batch for a product that still has stock.
     * Ordered by batchCreatedAt ASC so we deplete the earliest-received stock first.
     */
    @Query("SELECT pb FROM ProductBatch pb " +
           "WHERE pb.productId = :productId AND pb.currentStockInBatch > 0 " +
           "ORDER BY pb.batchCreatedAt ASC")
    Optional<ProductBatch> findOldestActiveBatchByProductId(@Param("productId") Integer productId);

    /**
     * Returns ALL batches for a product ordered oldest-first (for the history popup).
     */
    @Query("SELECT pb FROM ProductBatch pb WHERE pb.productId = :productId ORDER BY pb.batchId ASC")
    List<ProductBatch> findAllByProductIdOrderByBatchIdAsc(@Param("productId") Integer productId);
}
