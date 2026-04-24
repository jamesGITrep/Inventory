package com.winestore.inventory_system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a distinct price-batch for a product in the Product_Batches table.
 * A new batch is created whenever the purchase_price or selling_price changes.
 * If prices remain the same, the existing batch's stock is simply incremented.
 */
@Entity
@Table(name = "Product_Batches")
public class ProductBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Integer batchId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "current_stock_in_batch", nullable = false)
    private Integer currentStockInBatch = 0;

    @Column(name = "batch_created_at", nullable = false, updatable = false)
    private LocalDateTime batchCreatedAt;

    @PrePersist
    protected void onCreate() {
        if (batchCreatedAt == null) {
            batchCreatedAt = LocalDateTime.now();
        }
    }

    // --- Getters & Setters ---

    public Integer getBatchId() { return batchId; }
    public void setBatchId(Integer batchId) { this.batchId = batchId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Integer getCurrentStockInBatch() { return currentStockInBatch; }
    public void setCurrentStockInBatch(Integer currentStockInBatch) { this.currentStockInBatch = currentStockInBatch; }

    public LocalDateTime getBatchCreatedAt() { return batchCreatedAt; }
    public void setBatchCreatedAt(LocalDateTime batchCreatedAt) { this.batchCreatedAt = batchCreatedAt; }
}
