package com.winestore.inventory_system;

import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;

@SpringBootApplication
public class InventorySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventorySystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(ProductRepository repository) {
        return (args) -> {
            // Check if the table is empty before adding test data
            if (repository.count() == 0) {
                Product testProduct = new Product();
                testProduct.setProductName("Kingfisher Premium");
                testProduct.setSizeMl(650);
                testProduct.setCurrentStock(24);
                testProduct.setLowStockThreshold(10);
                testProduct.setPurchasePrice(new BigDecimal("150.00"));
                testProduct.setSellingPrice(new BigDecimal("180.00"));
                testProduct.setIsActive(true); // Dirty Data Guard check [cite: 3, 9]

                repository.save(testProduct);
                System.out.println(">>> TEST DATA INSERTED: Kingfisher Premium saved to database!");
            }
        };
    }
}