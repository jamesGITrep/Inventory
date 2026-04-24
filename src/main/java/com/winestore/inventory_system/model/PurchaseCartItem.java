package com.winestore.inventory_system.model;

import java.math.BigDecimal;

/**
 * Transient model representing one line-item in a batch purchase (not persisted).
 */
public class PurchaseCartItem {

    private Product product;
    private int quantity;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;

    public PurchaseCartItem(Product product, int quantity, BigDecimal purchasePrice, BigDecimal sellingPrice) {
        this.product = product;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
        this.sellingPrice = sellingPrice != null ? sellingPrice : BigDecimal.ZERO;
    }

    // --- Getters ---
    public Product getProduct() { return product; }
    public String getProductName() { return product != null ? product.getProductName() : ""; }
    public Integer getSizeMl() { return product != null ? product.getSizeMl() : 0; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    
    public BigDecimal getLineTotal() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
