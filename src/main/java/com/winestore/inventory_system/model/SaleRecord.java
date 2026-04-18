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

    @Transient // This tells the database NOT to save this column, it's only for the UI
    private String productName;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer saleId;

    private Integer productId;
    private Integer quantitySold;
    private BigDecimal salePriceAtTime;
    private LocalDate saleDate;
    public Integer getProductId() {
    return this.productId;
}

        public Integer getQuantitySold() {
            return this.quantitySold;
        }

        // Ensure the report logic can find the price
        public BigDecimal getUnitPrice() {
            return this.salePriceAtTime;
        }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        // This method is called from the InventoryService when a sale is made
        // It creates a new SaleRecord and saves it to the database with the current price and date

}