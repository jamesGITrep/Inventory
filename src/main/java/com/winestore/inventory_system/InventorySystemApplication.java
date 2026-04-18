package com.winestore.inventory_system;

import com.winestore.inventory_system.model.DailyReport;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.service.InventoryService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.math.BigDecimal;

import java.util.List;

@SpringBootApplication
@EnableScheduling // This turns on the internal alarm clock for our scheduled tasks (like the daily snapshot)   

public class InventorySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventorySystemApplication.class, args);
    }

       @Bean
CommandLineRunner run(InventoryService inventoryService, ProductRepository productRepository) {
    return args -> {
        System.out.println("--- STARTING CONSOLE REPORT TEST ---");

        // 1. Setup: Ensure a test product exists (e.g., Kingfisher)
        Product wine = new Product();
        wine.setProductName("Kingfisher Premium");
        wine.setCurrentStock(50);
        wine.setSellingPrice(new BigDecimal("180.00"));
        wine.setLowStockThreshold(10);
        wine.setIsActive(true);
        productRepository.save(wine);

        // 2. Simulate a Sale (Lisa's Logic)
        // Selling 10 bottles at 180.00 each
        inventoryService.recordSaleAndUpdateStock(wine.getProductId(), 10, new BigDecimal("180.00"));
        System.out.println("Simulated sale of 10 bottles recorded.");

        // 3. Generate and Print the Report (Eesha's Logic)
        List<DailyReport> todayReports = inventoryService.generateDailyReport();
        
        System.out.println("\n========= DAILY PROFIT REPORT =========");
        for (DailyReport report : todayReports) {
            System.out.println("Product: " + report.getProductName());
            System.out.println("Qty Sold: " + report.getTotalSold());
            System.out.println("Revenue:  ₹" + report.getTotalRevenue());
            System.out.println("Profit:   ₹" + report.getTotalProfit());
        }
        System.out.println("=======================================\n");
    };
}
}