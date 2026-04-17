package com.winestore.inventory_system.service;

import com.winestore.inventory_system.model.InventorySnapshot;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.model.SnapshotRepository;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.ProfitRepository;
import com.winestore.inventory_system.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    @Autowired // For fetching and updating product stock levels
    private ProductRepository productRepository;

    @Autowired // For recording sales transactions
    private SaleRepository saleRepository;

    @Autowired // For saving daily inventory snapshots for the Excise report
    private SnapshotRepository snapshotRepository;

    @Autowired // For fetching the last purchase price from the SQL View
    private ProfitRepository profitRepository;

    @Transactional
    public void recordSale(SaleRecord sale) {
        // 1. Find the product being sold
        Product product = productRepository.findById(sale.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 2. Logic: Deduct sold quantity from available stock [cite: 211]
        int updatedStock = product.getCurrentStock() - sale.getQuantitySold();
        
        if (updatedStock < 0) {
            throw new RuntimeException("Insufficient stock!");
        }

        // 3. Update the Product's stock level [cite: 205, 209]
        product.setCurrentStock(updatedStock);
        productRepository.save(product);

        // 4. Save the Sale Record for reporting [cite: 260]
        saleRepository.save(sale);
        
        System.out.println("Stock updated for: " + product.getProductId());
    }

        // Logic to prevent "Dirty Data" duplicates [cite: 163, 165, 168]
        public boolean isDuplicate(String name, Integer size) {
            String cleanName = name.replaceAll("\\s+", "").toLowerCase(); // [cite: 168, 170]
        return productRepository.findByIsActiveTrue().stream()
         .anyMatch(p -> p.getProductName().replaceAll("\\s+", "").equalsIgnoreCase(cleanName) 
                  && p.getSizeMl().equals(size)); // [cite: 21, 38, 114]
        }

        @Transactional
        public void recordSaleAndUpdateStock(Integer productId, Integer quantitySold, BigDecimal salePrice) {
            // 1. Fetch the product
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // 2. Inventory Logic: Deduct stock 
            int newStock = product.getCurrentStock() - quantitySold;
            
            // 3. Low Stock Alert System check 
            if (newStock < 0) {
                throw new RuntimeException("Insufficient stock for " + product.getProductName());
            }
            
            if (newStock <= product.getLowStockThreshold()) {
                System.out.println("ALERT: Low stock for " + product.getProductName());
            }

                    // 4. Update Product table
                    product.setCurrentStock(newStock);
                    productRepository.save(product);

                    // 5. Save the individual Sales_Record 
                    SaleRecord sale = new SaleRecord();
                    sale.setProductId(productId);
                    sale.setQuantitySold(quantitySold);
                    sale.setSalePriceAtTime(salePrice);
                    sale.setSaleDate(LocalDate.now());
                    saleRepository.save(sale);
                }

                // Historical Snapshots 
            @Transactional
            public void captureDailySnapshot(Integer productId) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                // This logic "freezes" the current state for the Excise report 
                InventorySnapshot snapshot = new InventorySnapshot();
                snapshot.setProductId(productId);
                snapshot.setRecordDate(LocalDate.now());
                snapshot.setClosingStock(product.getCurrentStock());
                
                // Note: Opening stock for today is technically yesterday's closing
                // For Phase 1, we will set it to current stock + today's sales
                snapshot.setOpeningStock(product.getCurrentStock()); 

                snapshotRepository.save(snapshot);
            }

            // Method to calculate profit margin for reports
        public BigDecimal calculateProfitMargin(Integer productId, BigDecimal currentSellingPrice) {
        return profitRepository.findById(productId)
        .map(view -> currentSellingPrice.subtract(view.getLastPricePaid()))
        .orElse(BigDecimal.ZERO);
}
}