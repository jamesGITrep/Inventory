package com.winestore.inventory_system;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.winestore.inventory_system.model.DailyReport;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.service.InventoryService;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@SpringBootApplication
@EnableScheduling
public class InventorySystemApplication extends Application {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Triggers the JavaFX lifecycle [cite: 6]
        Application.launch(InventorySystemApplication.class, args);
    }

    @Override
    public void init() throws Exception {
        // Starts the Spring Boot backend and database connection [cite: 2]
        springContext = SpringApplication.run(InventorySystemApplication.class);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // James: Using a more robust path loader for the UI file [cite: 4]
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            
            // Allows Spring to inject Lisa and Eesha's logic into your UI [cite: 6]
            loader.setControllerFactory(springContext::getBean);
            
            Parent root = loader.load();
            primaryStage.setTitle("Wine Store Inventory System - Master (Excise Friendly)");
            primaryStage.setScene(new Scene(root));
            
            // Keeps the window open on top
            primaryStage.show();
            
        } catch (Exception e) {
            // Surgical Debugging: Prints exactly what is crashing the UI
            System.err.println("CRITICAL UI ERROR: Could not load MainView.fxml");
            System.err.println("REASON: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    @Override
    public void stop() {
        // Cleanly shuts down the MySQL connection [cite: 2]
        if (springContext != null) {
            springContext.close();
        }
        System.exit(0);
    }

    @Bean
    CommandLineRunner run(InventoryService inventoryService, ProductRepository productRepository) {
        return args -> {
            System.out.println("--- STARTING CONSOLE REPORT TEST ---");

            // 1. Setup: Ensure a test product exists (Dirty Data Guard [cite: 9])
            if (productRepository.count() == 0) {
                Product wine = new Product();
                wine.setProductName("Kingfisher Premium");
                wine.setCurrentStock(50);
                wine.setSellingPrice(new BigDecimal("180.00"));
                wine.setLowStockThreshold(10);
                wine.setIsActive(true);
                productRepository.save(wine);
                
                // 2. Simulate a Sale (Lisa's Inventory Logic )
                inventoryService.recordSaleAndUpdateStock(wine.getProductId(), 10, new BigDecimal("180.00"));
                System.out.println("Simulated sale of 10 bottles recorded.");
            }

            // 3. Generate and Print the Report (Eesha's Logic [cite: 1, 2])
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