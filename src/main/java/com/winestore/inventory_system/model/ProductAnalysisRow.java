package com.winestore.inventory_system.model;

public class ProductAnalysisRow {
    private Integer productId;
    private String productName;
    private String category;
    private Integer sizeMl;
    private Integer totalVolume;
    private Double totalRevenue;
    private Double totalProfit;
    private Double avgMargin;

    public ProductAnalysisRow(Integer productId, String productName, String category, Integer sizeMl, Integer totalVolume, Double totalRevenue, Double totalProfit) {
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.sizeMl = sizeMl;
        this.totalVolume = totalVolume;
        this.totalRevenue = totalRevenue;
        this.totalProfit = totalProfit;
        this.avgMargin = (totalRevenue > 0) ? (totalProfit / totalRevenue) * 100 : 0.0;
    }

    // Getters and Setters
    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getSizeMl() { return sizeMl; }
    public void setSizeMl(Integer sizeMl) { this.sizeMl = sizeMl; }

    public Integer getTotalVolume() { return totalVolume; }
    public void setTotalVolume(Integer totalVolume) { this.totalVolume = totalVolume; }

    public Double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(Double totalRevenue) { this.totalRevenue = totalRevenue; }

    public Double getTotalProfit() { return totalProfit; }
    public void setTotalProfit(Double totalProfit) { this.totalProfit = totalProfit; }

    public Double getAvgMargin() { return avgMargin; }
    public void setAvgMargin(Double avgMargin) { this.avgMargin = avgMargin; }
}
