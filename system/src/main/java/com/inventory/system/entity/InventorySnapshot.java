package com.inventory.system.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "inventory_snapshots")
@Data
public class InventorySnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer snapshotId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private LocalDate recordDate; // The date this snapshot represents
    private Integer openingStock; // Stock at the start of the day
    private Integer quantityPurchased; // Total restocked that day
    private Integer quantitySold; // Total sold that day
    private Integer closingStock; // Final stock at end of day
}