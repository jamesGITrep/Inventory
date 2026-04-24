package com.winestore.inventory_system.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.winestore.inventory_system.model.AppSetting;
import com.winestore.inventory_system.model.InventorySnapshot;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.ProductBatch;
import com.winestore.inventory_system.model.PurchaseRecord;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.model.SnapshotRepository;
import com.winestore.inventory_system.repository.AppSettingRepository;
import com.winestore.inventory_system.repository.ProductBatchRepository;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.PurchaseRepository;
import com.winestore.inventory_system.repository.SaleRepository;

@ExtendWith(MockitoExtension.class)
class SecurityVaultServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private ProductBatchRepository productBatchRepository;

    @Mock
    private AppSettingRepository appSettingRepository;

    private SecurityVaultService securityVaultService;
    private final Map<String, AppSetting> settings = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        securityVaultService = new SecurityVaultService(
                productRepository,
                purchaseRepository,
                saleRepository,
                snapshotRepository,
                productBatchRepository,
                appSettingRepository);

        when(appSettingRepository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(settings.get(invocation.getArgument(0))));
        when(appSettingRepository.findAll()).thenAnswer(invocation -> new ArrayList<>(settings.values()));
        when(appSettingRepository.save(any(AppSetting.class))).thenAnswer(invocation -> {
            AppSetting setting = invocation.getArgument(0);
            settings.put(setting.getSettingKey(), setting);
            return setting;
        });
    }

    @Test
    void createSecureBackupWritesEncryptedVaultAndReadableSql() throws Exception {
        Product product = new Product();
        product.setProductId(7);
        product.setProductName("Kingfisher");
        product.setCategory("Beer");
        product.setCurrentStock(12);
        product.setOpeningStock(10);
        product.setTotalPurchases(4);
        product.setTotalSales(2);
        product.setPurchasePrice(new BigDecimal("50.00"));
        product.setSellingPrice(new BigDecimal("55.00"));
        product.setCreatedDate(LocalDate.of(2026, 4, 24));

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(purchaseRepository.findAll()).thenReturn(List.of());
        when(saleRepository.findAll()).thenReturn(List.of());
        when(snapshotRepository.findAll()).thenReturn(List.of());
        when(productBatchRepository.findAll()).thenReturn(List.of());

        SecurityVaultService.BackupResult result = securityVaultService.createSecureBackup(tempDir);

        assertTrue(".wine_vault.bak".equals(result.getVaultFile().getFileName().toString()));
        assertTrue("backup_decoy.sql".equals(result.getReadableSqlFile().getFileName().toString()));
        assertTrue(Files.exists(result.getVaultFile()));
        assertTrue(Files.exists(result.getReadableSqlFile()));
        assertFalse(Files.readString(result.getVaultFile(), StandardCharsets.UTF_8).contains("Kingfisher"));
        assertTrue(Files.readString(result.getReadableSqlFile(), StandardCharsets.UTF_8).contains("Kingfisher"));
        assertFalse("Never".equals(securityVaultService.getLastSecuredDisplay()));

        DosFileAttributes attributes = Files.readAttributes(result.getVaultFile(), DosFileAttributes.class);
        assertTrue(attributes.isHidden());
        assertTrue(attributes.isSystem());
        assertTrue(attributes.isReadOnly());
    }

    @Test
    void restoreFromVaultRejectsTamperedSignature() throws Exception {
        prepareMinimalRepositories();
        SecurityVaultService.BackupResult result = securityVaultService.createSecureBackup(tempDir);

        try {
            Files.setAttribute(result.getVaultFile(), "dos:readonly", false);
        } catch (Exception ignored) {
            // Fall back to the plain File API below.
        }
        result.getVaultFile().toFile().setWritable(true);
        String tampered = Files.readString(result.getVaultFile(), StandardCharsets.UTF_8)
                .replaceFirst("\"signature\"\\s*:\\s*\".", "\"signature\":\"A");
        Files.writeString(result.getVaultFile(), tampered, StandardCharsets.UTF_8);

        SecurityVaultService.RestoreResult restoreResult = securityVaultService.restoreFromVault(result.getBackupFolder());

        assertTrue(restoreResult.isTampered());
        verify(productRepository, never()).deleteAllInBatch();
    }

    @Test
    void restoreFromVaultRepopulatesRepositoriesWhenSignatureIsValid() throws Exception {
        Product product = new Product();
        product.setProductId(3);
        product.setProductName("Tuborg");
        product.setCurrentStock(9);
        product.setCreatedDate(LocalDate.of(2026, 4, 24));

        PurchaseRecord purchase = new PurchaseRecord();
        purchase.setPurchaseId(1);
        purchase.setProductId(3);
        purchase.setQuantityBought(5);
        purchase.setPurchasePrice(new BigDecimal("80.00"));
        purchase.setPurchaseDate(LocalDate.of(2026, 4, 24));

        SaleRecord sale = new SaleRecord();
        sale.setSaleId(2);
        sale.setProductId(3);
        sale.setQuantitySold(1);
        sale.setSalePriceAtTime(new BigDecimal("100.00"));
        sale.setSaleDate(LocalDate.of(2026, 4, 24));

        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setSnapshotId(4);
        snapshot.setProductId(3);
        snapshot.setRecordDate(LocalDate.of(2026, 4, 24));
        snapshot.setOpeningStock(5);
        snapshot.setQuantityPurchased(5);
        snapshot.setQuantitySold(1);
        snapshot.setClosingStock(9);

        ProductBatch batch = new ProductBatch();
        batch.setBatchId(8);
        batch.setProductId(3);
        batch.setPurchasePrice(new BigDecimal("80.00"));
        batch.setSellingPrice(new BigDecimal("100.00"));
        batch.setCurrentStockInBatch(9);

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(purchaseRepository.findAll()).thenReturn(List.of(purchase));
        when(saleRepository.findAll()).thenReturn(List.of(sale));
        when(snapshotRepository.findAll()).thenReturn(List.of(snapshot));
        when(productBatchRepository.findAll()).thenReturn(List.of(batch));

        SecurityVaultService.BackupResult result = securityVaultService.createSecureBackup(tempDir);
        clearInvocations(productRepository, purchaseRepository, saleRepository, snapshotRepository,
                productBatchRepository, appSettingRepository);

        SecurityVaultService.RestoreResult restoreResult = securityVaultService.restoreFromVault(result.getBackupFolder());

        assertTrue(restoreResult.isRestored());
        verify(snapshotRepository).deleteAllInBatch();
        verify(saleRepository).deleteAllInBatch();
        verify(purchaseRepository).deleteAllInBatch();
        verify(productBatchRepository).deleteAllInBatch();
        verify(productRepository).deleteAllInBatch();
        verify(appSettingRepository).deleteAllInBatch();
        verify(productRepository).saveAll(any());
        verify(purchaseRepository).saveAll(any());
        verify(saleRepository).saveAll(any());
        verify(snapshotRepository).saveAll(any());
        verify(productBatchRepository).saveAll(any());
        verify(appSettingRepository).saveAll(any());
    }

    private void prepareMinimalRepositories() throws IOException {
        when(productRepository.findAll()).thenReturn(List.of());
        when(purchaseRepository.findAll()).thenReturn(List.of());
        when(saleRepository.findAll()).thenReturn(List.of());
        when(snapshotRepository.findAll()).thenReturn(List.of());
        when(productBatchRepository.findAll()).thenReturn(List.of());
    }
}
