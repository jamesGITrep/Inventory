package com.winestore.inventory_system.model;

import java.math.BigDecimal;

/**
 * Transient model representing one line-item in the POS cart (not persisted).
 */
public class CartItem {

    private Product product;
    private int quantity;
    private BigDecimal unitPrice;

    public CartItem(Product product, int quantity) {
        this.product  = product;
        this.quantity = quantity;
        this.unitPrice = product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public Product getProduct()    { return product; }
    public int     getQuantity()   { return quantity; }
    public void    setQuantity(int q) { this.quantity = q; }

    public String     getProductName() { return product.getProductName(); }
    public BigDecimal getUnitPrice()   { return unitPrice; }
    public BigDecimal getLineTotal()   { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    @Override
    public String toString() {
        return product.getProductName() + " × " + quantity;
    }
}
