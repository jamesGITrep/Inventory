package com.winestore.inventory_system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
// We add the Index here. 'idx_product_name' makes searching the 'product_name' column instant.
@Table(name = "Products", indexes = {
    @Index(name = "idx_product_name", columnList = "product_name")
})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    @Column(nullable = false)
    private String productName;

    private Integer manufacturerId; // Link to Manufacturer [cite: 27]
    private Integer categoryId;
    private Integer sizeMl; // Bottle/Can size [cite: 30]
    
    private Integer currentStock;
    private Integer lowStockThreshold; // For alerts [cite: 33, 142]
    
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    
    @Column(name = "is_active")
    private Boolean isActive = true; // Guard for duplicates [cite: 159]

    // Standard Getters and Setters are required for Spring Data JPA
   public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Integer getCurrentStock() { return currentStock; }
    public void setCurrentStock(Integer currentStock) { this.currentStock = currentStock; }

    public Integer getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(Integer lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }

    // Repeat this simple pattern for sizeMl, prices, and isActive...
    public Integer getSizeMl() { return sizeMl; }
    public void setSizeMl(Integer sizeMl) { this.sizeMl = sizeMl; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }


}