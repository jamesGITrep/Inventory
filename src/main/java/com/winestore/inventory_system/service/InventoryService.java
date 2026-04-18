package com.winestore.inventory_system.service;

import com.winestore.inventory_system.model.DailyReport;
import com.winestore.inventory_system.model.InventorySnapshot;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.PurchaseRecord;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.model.SnapshotRepository;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.ProfitRepository;
import com.winestore.inventory_system.repository.PurchaseRepository;
import com.winestore.inventory_system.repository.SaleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private PurchaseRepository purchaseRepository; // For recording purchases (stock inward)

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

        public List<Product> getAllProducts() {
           return productRepository.findAll();
        }

        // Inside InventoryService.java
    public List<Product> searchProducts(String query) {
    if (query == null || query.trim().isEmpty()) {
        return productRepository.findAll(); 
    }
        // This calls the "Engine"  built in the Repository
        return productRepository.findByProductNameContainingIgnoreCaseAndIsActiveTrue(query.trim());
    }

    public List<DailyReport> generateDailyReport() {
    List<DailyReport> reports = new ArrayList<>();
    List<Product> products = productRepository.findAll();

    for (Product p : products) {
        // 1. Get all sales for this product TODAY
        // Pass the ID instead of the object
        List<SaleRecord> sales = saleRepository.findByProductIdAndSaleDate(p.getProductId(), LocalDate.now());
        
        if (!sales.isEmpty()) {
            int totalQty = sales.stream().mapToInt(SaleRecord::getQuantitySold).sum();
            BigDecimal totalRev = sales.stream()
                .map(s -> s.getUnitPrice().multiply(BigDecimal.valueOf(s.getQuantitySold())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 2. Calculate Profit (Revenue - Last Purchase Price)
            BigDecimal margin = calculateProfitMargin(p.getProductId(), p.getSellingPrice());
            BigDecimal totalProfit = margin.multiply(BigDecimal.valueOf(totalQty));

            reports.add(new DailyReport(p.getProductName(), totalQty, totalRev, totalProfit));
        }
    }
    return reports;
}

    public void captureAllSnapshots() {
    List<Product> products = productRepository.findAll();
    for (Product p : products) {
        captureDailySnapshot(p.getProductId());
    }
}


                @Transactional // This ensures if one sale fails, the whole "Batch" is rolled back
        public void processBatchSale(List<SaleRecord> batchSales) {
            for (SaleRecord sale : batchSales) {
                // Reuse your solid logic from yesterday
                recordSaleAndUpdateStock(
                    sale.getProductId(), 
                    sale.getQuantitySold(), 
                    sale.getUnitPrice()
                );
            }
            System.out.println(">>> BATCH SALE: Successfully processed " + batchSales.size() + " items.");
        }
            public void recordPurchase(Integer productId, Integer qty, BigDecimal buyPrice) {
    Product p = productRepository.findById(productId)
        .orElseThrow(() -> new RuntimeException("Product not found"));

    // 1. Increase the stock
    p.setCurrentStock(p.getCurrentStock() + qty);
    productRepository.save(p); 

    // 2. Record the purchase for Profit Analysis
    PurchaseRecord record = new PurchaseRecord();
    record.setProductId(productId);
    record.setQuantityBought(qty);
    record.setPurchasePrice(buyPrice);
    record.setPurchaseDate(LocalDate.now());
    // purchaseRepository.save(record); // Assuming you created PurchaseRepository

   purchaseRepository.save(record); // This will allow us to track the cost price for profit margin calculations
    System.out.println(">>> STOCK INWARD: Added " + qty + " units to " + p.getProductName());

}


    

}