package com.winestore.inventory_system.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "Sales_Records")
public class SaleRecord {
    public Integer getSaleId() {
        return saleId;
    }
    public void setSaleId(Integer saleId) {
        this.saleId = saleId;
    }
    public void setProductId(Integer productId) {
        this.productId = productId;
    }
    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }
    public BigDecimal getSalePriceAtTime() {
        return salePriceAtTime;
    }
    public void setSalePriceAtTime(BigDecimal salePriceAtTime) {
        this.salePriceAtTime = salePriceAtTime;
    }
    public LocalDate getSaleDate() {
        return saleDate;
    }
    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer saleId;

    private Integer productId;
    private Integer quantitySold;
    private BigDecimal salePriceAtTime;
    private LocalDate saleDate;
    public Integer getProductId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProductId'");
    }
    public int getQuantitySold() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getQuantitySold'");
    }
}