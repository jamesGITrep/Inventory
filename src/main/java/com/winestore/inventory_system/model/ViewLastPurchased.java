package com.winestore.inventory_system.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Immutable // Marks this entity as read-only, reflecting the SQL View's nature. this tells Spring that this is a View, not a table you can change
@Table(name = "View_Last_Purchased") // Links to Eesha's SQL View 
public class ViewLastPurchased {
    @Id
    private Integer productId;
    private String productName;
    private Integer sizeMl;
    private LocalDate lastPurchasedDate;
    private Integer lastQuantityPurchased;
    private BigDecimal lastPricePaid;

    // Standard Getters ONLY (no setters needed for an Immutable View)
    public Integer getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getLastPricePaid() { return lastPricePaid; }
}