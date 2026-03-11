package com.inventory.system.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Data // Lombok generates getters, setters, and toString
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer productId;

    @Column(unique = true, nullable = false)
    private String productName;

    private Integer currentStock;
    private Integer lowStockThreshold;

    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;

    // Establishing the relationship with Category (FK)
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}