package com.winestore.inventory_system.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Inventory_Snapshots")
public class InventorySnapshot {
    public Integer getSnapshotId() {
        return snapshotId;
    }
    public void setSnapshotId(Integer snapshotId) {
        this.snapshotId = snapshotId;
    }
    public Integer getProductId() {
        return productId;
    }
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
    public LocalDate getRecordDate() {
        return recordDate;
    }
    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }
    public Integer getOpeningStock() {
        return openingStock;
    }
    public void setOpeningStock(Integer openingStock) {
        this.openingStock = openingStock;
    }
    public Integer getQuantityPurchased() {
        return quantityPurchased;
    }
    public void setQuantityPurchased(Integer quantityPurchased) {
        this.quantityPurchased = quantityPurchased;
    }
    public Integer getQuantitySold() {
        return quantitySold;
    }
    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }
    public Integer getClosingStock() {
        return closingStock;
    }
    public void setClosingStock(Integer closingStock) {
        this.closingStock = closingStock;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer snapshotId;

    private Integer productId;
    private LocalDate recordDate;
    private Integer openingStock;
    private Integer quantityPurchased;
    private Integer quantitySold;
    private Integer closingStock;
}