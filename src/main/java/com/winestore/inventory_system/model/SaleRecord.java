package com.winestore.inventory_system.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "Sales_Records")
public class SaleRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer saleId;

    private Integer productId;
    private Integer quantitySold;
    private BigDecimal salePriceAtTime;
    private LocalDate saleDate;

    @Transient 
    private String productName;
    @Transient
    private BigDecimal profit;
    @Transient
    private Integer sizeMl;

    // Standard Getters and Setters
    public Integer getSaleId() { return saleId; }
    public void setSaleId(Integer saleId) { this.saleId = saleId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getQuantitySold() { return quantitySold; }
    public void setQuantitySold(Integer quantitySold) { this.quantitySold = quantitySold; }

    public BigDecimal getSalePriceAtTime() { return salePriceAtTime; }
    public void setSalePriceAtTime(BigDecimal salePriceAtTime) { this.salePriceAtTime = salePriceAtTime; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    
    public BigDecimal getProfit() { return profit; }
    public void setProfit(BigDecimal profit) { this.profit = profit; }

    public Integer getSizeMl() { return sizeMl; }
    public void setSizeMl(Integer sizeMl) { this.sizeMl = sizeMl; }

    // Computed Fields for TableView
    public BigDecimal getTotalPrice() {
        if (salePriceAtTime == null || quantitySold == null) return BigDecimal.ZERO;
        return salePriceAtTime.multiply(BigDecimal.valueOf(quantitySold));
    }

    // Alias for report logic consistency
    public BigDecimal getUnitPrice() { return this.salePriceAtTime; }
}