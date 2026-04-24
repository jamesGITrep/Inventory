package com.winestore.inventory_system.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "Products", indexes = {
    @Index(name = "idx_product_name", columnList = "product_name")
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    @Column(nullable = false)
    private String productName;

    private String manufacturer; 
    private String category;
    private Integer sizeMl; 
    
    private Integer currentStock;
    private Integer lowStockThreshold; 
    
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    
    @Column(name = "is_active")
    private Boolean isActive = true;

    // --- James: Added fields for Excise Friendly Dashboard compatibility ---
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer openingStock = 0;
    
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalPurchases = 0;
    
    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalSales = 0;

    @Column(name = "created_date")
    private LocalDate createdDate = LocalDate.now();

    // Standard Getters and Setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    public Integer getSizeMl() { return sizeMl; }
    public void setSizeMl(Integer sizeMl) { this.sizeMl = sizeMl; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // --- New Getters for Dashboard Columns ---
    public Integer getOpeningStock() { return openingStock; }
    public void setOpeningStock(Integer openingStock) { this.openingStock = openingStock; }

    public Integer getTotalPurchases() { return totalPurchases; }
    public void setTotalPurchases(Integer totalPurchases) { this.totalPurchases = totalPurchases; }

    public Integer getTotalSales() { return totalSales; }
    public void setTotalSales(Integer totalSales) { this.totalSales = totalSales; }

    // Logic for the "Stock Alert" column
    public String getStockStatus() {
        if (currentStock == null || lowStockThreshold == null) return "OK";
        return (currentStock <= lowStockThreshold) ? "LOW STOCK" : "OK";
    }

    // Computed profit = (sellingPrice - purchasePrice) x totalSales
    @Transient
    public BigDecimal getProfit() {
        if (purchasePrice == null || sellingPrice == null) return null;
        if (totalSales == null || totalSales == 0) return null;
        return sellingPrice.subtract(purchasePrice)
                           .multiply(BigDecimal.valueOf(totalSales));
    }
}