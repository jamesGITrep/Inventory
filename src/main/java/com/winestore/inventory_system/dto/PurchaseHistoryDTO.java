package com.winestore.inventory_system.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PurchaseHistoryDTO {
    private Integer purchaseId;
    private Integer productId;
    private String productName;
    private String categoryName;
    private String manufacturerName;
    private Integer sizeMl;
    private Integer quantityAdded;
    private BigDecimal purchasePrice;
    private BigDecimal totalCost;
    private LocalDate purchaseDate;

    public PurchaseHistoryDTO(Integer purchaseId, Integer productId, String productName, String categoryName, 
                              String manufacturerName, Integer sizeMl, Integer quantityAdded, BigDecimal purchasePrice, 
                              LocalDate purchaseDate) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.productName = productName;
        this.categoryName = categoryName;
        this.manufacturerName = manufacturerName;
        this.sizeMl = sizeMl;
        this.quantityAdded = quantityAdded;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.totalCost = purchasePrice != null ? purchasePrice.multiply(BigDecimal.valueOf(quantityAdded)) : BigDecimal.ZERO;
    }

    // --- Getters and Setters ---
    public Integer getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Integer purchaseId) { this.purchaseId = purchaseId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getManufacturerName() { return manufacturerName; }
    public void setManufacturerName(String manufacturerName) { this.manufacturerName = manufacturerName; }

    public Integer getSizeMl() { return sizeMl; }
    public void setSizeMl(Integer sizeMl) { this.sizeMl = sizeMl; }

    public Integer getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(Integer quantityAdded) { this.quantityAdded = quantityAdded; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
}
