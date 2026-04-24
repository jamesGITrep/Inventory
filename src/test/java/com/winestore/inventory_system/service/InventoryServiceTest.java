package com.winestore.inventory_system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.winestore.inventory_system.model.InventorySnapshot;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.ProductBatch;
import com.winestore.inventory_system.model.PurchaseRecord;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.model.SnapshotRepository;
import com.winestore.inventory_system.repository.AppSettingRepository;
import com.winestore.inventory_system.repository.ProductBatchRepository;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.ProfitRepository;
import com.winestore.inventory_system.repository.PurchaseRepository;
import com.winestore.inventory_system.repository.SaleRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private ProfitRepository profitRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private AppSettingRepository appSettingRepository;

    @Mock
    private ProductBatchRepository productBatchRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void recordPurchaseUpdatesStockPricesAndSnapshotClosingStock() {
        Product product = new Product();
        product.setProductId(1);
        product.setProductName("Kingfisher");
        product.setCurrentStock(10);
        product.setTotalPurchases(2);
        product.setPurchasePrice(new BigDecimal("45.00"));
        product.setSellingPrice(new BigDecimal("55.00"));

        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setProductId(1);
        snapshot.setRecordDate(LocalDate.of(2026, 4, 24));
        snapshot.setOpeningStock(10);
        snapshot.setQuantityPurchased(0);
        snapshot.setQuantitySold(0);
        snapshot.setClosingStock(10);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(purchaseRepository.save(any(PurchaseRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findAll()).thenReturn(List.of(snapshot));
        when(productBatchRepository.findLatestBatchByProductId(1)).thenReturn(Optional.empty());
        when(productBatchRepository.save(any(ProductBatch.class))).thenAnswer(invocation -> {
            ProductBatch batch = invocation.getArgument(0);
            if (batch.getBatchId() == null) {
                batch.setBatchId(99);
            }
            return batch;
        });

        inventoryService.recordPurchase(
                1,
                5,
                new BigDecimal("50.00"),
                new BigDecimal("60.00"),
                LocalDate.of(2026, 4, 24));

        assertEquals(15, product.getCurrentStock());
        assertEquals(7, product.getTotalPurchases());
        assertEquals(new BigDecimal("50.00"), product.getPurchasePrice());
        assertEquals(new BigDecimal("60.00"), product.getSellingPrice());
        assertEquals(5, snapshot.getQuantityPurchased());
        assertEquals(15, snapshot.getClosingStock());
        verify(snapshotRepository).save(snapshot);
    }

    @Test
    void recordSaleUpdatesStockAndSnapshotClosingStock() {
        Product product = new Product();
        product.setProductId(1);
        product.setProductName("Tuborg");
        product.setCurrentStock(15);
        product.setTotalSales(1);

        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setProductId(1);
        snapshot.setRecordDate(LocalDate.of(2026, 4, 24));
        snapshot.setOpeningStock(20);
        snapshot.setQuantityPurchased(5);
        snapshot.setQuantitySold(1);
        snapshot.setClosingStock(24);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(saleRepository.save(any(SaleRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(snapshotRepository.findAll()).thenReturn(List.of(snapshot));
        ProductBatch batch = new ProductBatch();
        batch.setBatchId(101);
        batch.setProductId(1);
        batch.setCurrentStockInBatch(15);
        when(productBatchRepository.findOldestActiveBatchByProductId(1)).thenReturn(Optional.of(batch));
        when(productBatchRepository.save(any(ProductBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.recordSaleAndUpdateStock(
                1,
                4,
                new BigDecimal("100.00"),
                LocalDate.of(2026, 4, 24));

        assertEquals(11, product.getCurrentStock());
        assertEquals(5, product.getTotalSales());
        assertEquals(5, snapshot.getQuantitySold());
        assertEquals(20, snapshot.getClosingStock());
        verify(snapshotRepository).save(snapshot);
    }

    @Test
    void getProductsForDateCalculatesClosingStockFromPurchasesAndSales() {
        LocalDate targetDate = LocalDate.of(2026, 4, 24);

        Product product = new Product();
        product.setProductId(1);
        product.setProductName("Morpheus");
        product.setCreatedDate(targetDate.minusDays(1));
        product.setCurrentStock(12);
        product.setLowStockThreshold(2);
        product.setPurchasePrice(new BigDecimal("815.00"));
        product.setSellingPrice(new BigDecimal("850.00"));

        PurchaseRecord purchaseOnDate = new PurchaseRecord();
        purchaseOnDate.setProductId(1);
        purchaseOnDate.setQuantityBought(5);
        purchaseOnDate.setPurchaseDate(targetDate);

        PurchaseRecord purchaseAfterDate = new PurchaseRecord();
        purchaseAfterDate.setProductId(1);
        purchaseAfterDate.setQuantityBought(3);
        purchaseAfterDate.setPurchaseDate(targetDate.plusDays(1));

        SaleRecord saleOnDate = new SaleRecord();
        saleOnDate.setProductId(1);
        saleOnDate.setQuantitySold(2);
        saleOnDate.setSaleDate(targetDate);

        SaleRecord saleAfterDate = new SaleRecord();
        saleAfterDate.setProductId(1);
        saleAfterDate.setQuantitySold(4);
        saleAfterDate.setSaleDate(targetDate.plusDays(1));

        when(productRepository.findByIsActiveTrue()).thenReturn(List.of(product));
        when(purchaseRepository.findAll()).thenReturn(List.of(purchaseOnDate, purchaseAfterDate));
        when(saleRepository.findAll()).thenReturn(List.of(saleOnDate, saleAfterDate));

        Product historical = inventoryService.getProductsForDate(targetDate).getFirst();

        assertEquals(10, historical.getOpeningStock());
        assertEquals(5, historical.getTotalPurchases());
        assertEquals(2, historical.getTotalSales());
        assertEquals(13, historical.getCurrentStock());
    }

    @Test
    void recordSaleRejectsNegativeClosingStock() {
        Product product = new Product();
        product.setProductId(1);
        product.setProductName("Jack Daniels");
        product.setCurrentStock(2);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                inventoryService.recordSaleAndUpdateStock(
                        1,
                        3,
                        new BigDecimal("1200.00"),
                        LocalDate.of(2026, 4, 24)));

        assertEquals("Insufficient stock for Jack Daniels", exception.getMessage());
    }
}
