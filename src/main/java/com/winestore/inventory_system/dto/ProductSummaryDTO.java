package com.winestore.inventory_system.dto;

import java.math.BigDecimal;

/**
 * Data Transfer Object that combines core Product fields with batch-derived
 * aggregate data (total stock across all batches, price range) so the
 * main dashboard never needs to fire per-product stock-sum queries.
 */
public class ProductSummaryDTO {

    private final Integer productId;
    private final String productName;
    private final String category;
    private final String manufacturer;
    private final Integer sizeMl;

    /** Sum of current_stock_in_batch across ALL batches for this product. */
    private final int totalStock;

    /**
     * Number of distinct price batches that currently have stock.
     * > 1 means the shelf has items at different cost prices (FIFO-relevant).
     */
    private final int activeBatchCount;

    /**
     * The cheapest purchase price among active batches (oldest cost floor).
     * Null when no batches exist.
     */
    private final BigDecimal lowestBuyPrice;

    /**
     * The most expensive purchase price among active batches (newest cost ceiling).
     * Null when no batches exist.
     */
    private final BigDecimal highestBuyPrice;

    /** The selling price on the latest batch (current shelf price). */
    private final BigDecimal currentSellingPrice;

    public ProductSummaryDTO(Integer productId,
                             String productName,
                             String category,
                             String manufacturer,
                             Integer sizeMl,
                             int totalStock,
                             int activeBatchCount,
                             BigDecimal lowestBuyPrice,
                             BigDecimal highestBuyPrice,
                             BigDecimal currentSellingPrice) {
        this.productId          = productId;
        this.productName        = productName;
        this.category           = category;
        this.manufacturer       = manufacturer;
        this.sizeMl             = sizeMl;
        this.totalStock         = totalStock;
        this.activeBatchCount   = activeBatchCount;
        this.lowestBuyPrice     = lowestBuyPrice;
        this.highestBuyPrice    = highestBuyPrice;
        this.currentSellingPrice = currentSellingPrice;
    }

    // --- Getters ---

    public Integer getProductId()           { return productId; }
    public String  getProductName()         { return productName; }
    public String  getCategory()            { return category; }
    public String  getManufacturer()        { return manufacturer; }
    public Integer getSizeMl()              { return sizeMl; }
    public int     getTotalStock()          { return totalStock; }
    public int     getActiveBatchCount()    { return activeBatchCount; }
    public BigDecimal getLowestBuyPrice()   { return lowestBuyPrice; }
    public BigDecimal getHighestBuyPrice()  { return highestBuyPrice; }
    public BigDecimal getCurrentSellingPrice() { return currentSellingPrice; }

    /**
     * Returns a user-friendly price-range string, e.g. "Rs.80.00 – Rs.95.00"
     * or just "Rs.80.00" when there is only one cost tier.
     */
    public String getBuyPriceRangeDisplay() {
        if (lowestBuyPrice == null) return "-";
        if (highestBuyPrice == null || lowestBuyPrice.compareTo(highestBuyPrice) == 0) {
            return "Rs." + lowestBuyPrice.toPlainString();
        }
        return "Rs." + lowestBuyPrice.toPlainString() + " – Rs." + highestBuyPrice.toPlainString();
    }

    /**
     * True when multiple cost tiers are present, signalling FIFO matters for profit calc.
     */
    public boolean hasMixedCosts() {
        return activeBatchCount > 1;
    }
}
