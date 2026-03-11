package com.inventory.system.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.system.entity.InventorySnapshot;
import com.inventory.system.entity.Product;
import com.inventory.system.entity.PurchaseRecord;
import com.inventory.system.entity.SalesRecord;
import com.inventory.system.repository.InventorySnapshotRepository;
import com.inventory.system.repository.ProductRepository;
import com.inventory.system.repository.PurchaseRecordRepository;
import com.inventory.system.repository.SalesRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepo;
    private final SalesRecordRepository salesRepo;
    private final PurchaseRecordRepository purchaseRepo;
    private final InventorySnapshotRepository snapshotRepo;

    @Transactional
    public void recordSale(Integer productId, Integer quantity) {
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getCurrentStock() < quantity) {
            throw new RuntimeException("Insufficient stock!");
        }

        // Update Stock
        product.setCurrentStock(product.getCurrentStock() - quantity);
        productRepo.save(product);

        // Record Sale
        SalesRecord sale = new SalesRecord();
        sale.setProduct(product);
        sale.setQuantitySold(quantity);
        sale.setSalePriceAtTime(product.getSellingPrice());
        salesRepo.save(sale);
    }

    @Transactional
    public void restockProduct(Integer productId, Integer quantity) {
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setCurrentStock(product.getCurrentStock() + quantity);
        productRepo.save(product);

        PurchaseRecord purchase = new PurchaseRecord();
        purchase.setProduct(product);
        purchase.setQuantityAdded(quantity);
        purchase.setPurchasePriceAtTime(product.getPurchasePrice());
        purchaseRepo.save(purchase);
    }


@Transactional
public void createDailySnapshot(Integer productId) {
    Product product = productRepo.findById(productId)
        .orElseThrow(() -> new RuntimeException("Product not found"));

    InventorySnapshot snapshot = new InventorySnapshot();
    snapshot.setProduct(product);
    snapshot.setRecordDate(LocalDate.now());
    snapshot.setClosingStock(product.getCurrentStock());
    // In a full version, you'd calculate opening stock and daily changes here
    snapshotRepo.save(snapshot);
}




    public List<Product> getLowStockProducts() {
        // This uses the custom threshold logic from your ERD
        return productRepo.findAll().stream()
            .filter(p -> p.getCurrentStock() <= p.getLowStockThreshold())
            .toList();
    }



}