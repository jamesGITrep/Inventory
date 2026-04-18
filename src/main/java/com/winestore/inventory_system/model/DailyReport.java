package com.winestore.inventory_system.model;

import java.math.BigDecimal;

// This is a simple POJO (Plain Old Java Object) that will hold the data for our daily sales report.
public class DailyReport {
    private String productName;
    private Integer totalSold;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;

    // Constructor to easily create DailyReport objects
    public DailyReport(String productName, Integer totalSold, BigDecimal totalRevenue, BigDecimal totalProfit) {
        this.productName = productName;
        this.totalSold = totalSold;
        this.totalRevenue = totalRevenue;
        this.totalProfit = totalProfit;
    }

    // Standard Getters
    public String getProductName() { return productName; }
    public Integer getTotalSold() { return totalSold; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getTotalProfit() { return totalProfit; }
}