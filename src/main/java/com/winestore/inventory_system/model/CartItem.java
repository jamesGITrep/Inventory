package com.winestore.inventory_system.model;

import java.math.BigDecimal;

/**
 * Transient model representing one line-item in the POS cart (not persisted).
 */
public class CartItem {

    private Product product;
    private int quantity;
    private BigDecimal unitPrice;
    private String category;
    private Integer sizeMl;
    private Integer totalVolume;

    public CartItem(Product product, int quantity) {
        this.product  = product;
        this.quantity = quantity;
        this.unitPrice = product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO;
        this.category = product.getCategory();
        this.sizeMl = product.getSizeMl();
        this.totalVolume = product.getSizeMl() * quantity;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public Product getProduct()    { return product; }
    public int     getQuantity()   { return quantity; }
    public void    setQuantity(int q) { this.quantity = q; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSizeMl() { return sizeMl; }
    public void setSizeMl(Integer sizeMl) { this.sizeMl = sizeMl; }

    public Integer getTotalVolume() { return totalVolume; }
    public void setTotalVolume(Integer totalVolume) { this.totalVolume = totalVolume; }

    public String     getProductName() { return product.getProductName(); }
    public BigDecimal getUnitPrice()   { return unitPrice; }
    public BigDecimal getLineTotal()   { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    @Override
    public String toString() {
        return product.getProductName() + " × " + quantity;
    }
}
