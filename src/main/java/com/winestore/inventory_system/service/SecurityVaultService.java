package com.winestore.inventory_system.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@Service
public class SecurityVaultService {

    private static final String LAST_SECURED_KEY = "last_secured_backup_at";
    private static final String VAULT_FILENAME = ".wine_vault.bak";
    private static final String SQL_FILENAME = "backup_decoy.sql";
    private static final byte[] HARD_CODED_AES_KEY =
            "InvManSys-AES-256-Master-Key-420".getBytes(StandardCharsets.UTF_8);
    private static final byte[] HARD_CODED_HMAC_KEY =
            "InvManSys-HMAC-Signature-Key-99".getBytes(StandardCharsets.UTF_8);
    private static final DateTimeFormatter DISPLAY_TIMESTAMP =
            DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ENGLISH);

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final SnapshotRepository snapshotRepository;
    private final ProductBatchRepository productBatchRepository;
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecurityVaultService(ProductRepository productRepository,
                                PurchaseRepository purchaseRepository,
                                SaleRepository saleRepository,
                                SnapshotRepository snapshotRepository,
                                ProductBatchRepository productBatchRepository,
                                AppSettingRepository appSettingRepository) {
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleRepository = saleRepository;
        this.snapshotRepository = snapshotRepository;
        this.productBatchRepository = productBatchRepository;
        this.appSettingRepository = appSettingRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Transactional
    public BackupResult createSecureBackup(Path parentDirectory) throws IOException, GeneralSecurityException {
        if (parentDirectory == null) {
            throw new IOException("Please choose a backup location.");
        }

        LocalDateTime now = LocalDateTime.now();
        setSettingValue(LAST_SECURED_KEY, now.format(DISPLAY_TIMESTAMP));

        VaultKeys keys = getVaultKeys();
        DatabaseBackupPayload payload = buildPayload();

        String payloadJson = objectMapper.writeValueAsString(payload);
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);

        byte[] encrypted = encrypt(payloadJson, keys.aesKey(), iv);
        byte[] signature = sign(iv, encrypted, keys.hmacKey());

        VaultEnvelope envelope = new VaultEnvelope();
        envelope.setVersion(1);
        envelope.setCreatedAt(Instant.now().toString());
        envelope.setCipher("AES-256-CBC");
        envelope.setSignatureAlgorithm("HMAC-SHA256");
        envelope.setIv(Base64.getEncoder().encodeToString(iv));
        envelope.setPayload(Base64.getEncoder().encodeToString(encrypted));
        envelope.setSignature(Base64.getEncoder().encodeToString(signature));

        Path backupFolder = Files.createDirectories(parentDirectory.resolve("secure-backup-" + now.format(FILE_TIMESTAMP)));
        Path vaultFile = backupFolder.resolve(VAULT_FILENAME);
        Path sqlFile = backupFolder.resolve(SQL_FILENAME);

        Files.writeString(vaultFile, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(envelope),
                StandardCharsets.UTF_8);
        Files.writeString(sqlFile, generateReadableSql(payload), StandardCharsets.UTF_8);

        markAsHiddenSystem(vaultFile);
        makeReadOnly(vaultFile);
        makeReadOnly(sqlFile);
        makeReadOnly(backupFolder);

        return new BackupResult(backupFolder, vaultFile, sqlFile, now.format(DISPLAY_TIMESTAMP));
    }

    @Transactional
    public RestoreResult restoreFromVault(Path selectedPath) throws IOException, GeneralSecurityException {
        Path vaultFile = resolveVaultFile(selectedPath);
        if (vaultFile == null || !Files.exists(vaultFile)) {
            throw new IOException("Please choose a valid vault file.");
        }

        VaultEnvelope envelope = objectMapper.readValue(Files.readString(vaultFile, StandardCharsets.UTF_8), VaultEnvelope.class);
        VaultKeys keys = getVaultKeys();
        byte[] iv = Base64.getDecoder().decode(envelope.getIv());
        byte[] payload = Base64.getDecoder().decode(envelope.getPayload());
        byte[] signature = Base64.getDecoder().decode(envelope.getSignature());

        byte[] expectedSignature = sign(iv, payload, keys.hmacKey());
        if (!java.security.MessageDigest.isEqual(signature, expectedSignature)) {
            return RestoreResult.tampered("Tampering detected. Restore was blocked to avoid writing corrupted stock data.");
        }

        String json = decrypt(payload, keys.aesKey(), iv);
        DatabaseBackupPayload backupPayload = objectMapper.readValue(json, DatabaseBackupPayload.class);

        snapshotRepository.deleteAllInBatch();
        saleRepository.deleteAllInBatch();
        purchaseRepository.deleteAllInBatch();
        productBatchRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        appSettingRepository.deleteAllInBatch();

        if (!backupPayload.getAppSettings().isEmpty()) {
            appSettingRepository.saveAll(backupPayload.getAppSettings());
        }
        if (!backupPayload.getProducts().isEmpty()) {
            productRepository.saveAll(backupPayload.getProducts());
        }
        if (!backupPayload.getProductBatches().isEmpty()) {
            productBatchRepository.saveAll(backupPayload.getProductBatches());
        }
        if (!backupPayload.getPurchases().isEmpty()) {
            purchaseRepository.saveAll(backupPayload.getPurchases());
        }
        if (!backupPayload.getSales().isEmpty()) {
            saleRepository.saveAll(backupPayload.getSales());
        }
        if (!backupPayload.getSnapshots().isEmpty()) {
            snapshotRepository.saveAll(backupPayload.getSnapshots());
        }

        return RestoreResult.success("Inventory data was restored successfully.");
    }

    public Path exportInventoryCsv(LocalDate date, Path parentDirectory, List<Product> products) throws IOException {
        if (date == null) {
            date = LocalDate.now();
        }
        if (parentDirectory == null) {
            throw new IOException("Please choose an export location.");
        }

        Path csvFile = parentDirectory.resolve("Inventory_Export_" + date + ".csv");
        List<String> lines = new ArrayList<>();
        lines.add("Date,Product ID,Product Name,Category,Opening Stock,Purchases,Sales,Closing Stock,Purchase Price,Selling Price,Profit,Stock Alert");

        for (Product product : products) {
            lines.add(String.join(",",
                    date.toString(),
                    safeString(product.getProductId()),
                    csvValue(product.getProductName()),
                    csvValue(product.getCategory()),
                    safeString(product.getOpeningStock()),
                    safeString(product.getTotalPurchases()),
                    safeString(product.getTotalSales()),
                    safeString(product.getCurrentStock()),
                    safeString(product.getPurchasePrice()),
                    safeString(product.getSellingPrice()),
                    safeString(product.getProfit()),
                    csvValue(product.getStockStatus())));
        }

        Files.write(csvFile, lines, StandardCharsets.UTF_8);
        makeReadOnly(csvFile);
        return csvFile;
    }

    public String getLastSecuredDisplay() {
        return appSettingRepository.findById(LAST_SECURED_KEY)
                .map(AppSetting::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse("Never");
    }

    private DatabaseBackupPayload buildPayload() {
        DatabaseBackupPayload payload = new DatabaseBackupPayload();
        payload.setAppSettings(new ArrayList<>(appSettingRepository.findAll()));
        payload.setProducts(new ArrayList<>(productRepository.findAll()));
        payload.setProductBatches(new ArrayList<>(productBatchRepository.findAll()));
        payload.setPurchases(new ArrayList<>(purchaseRepository.findAll()));
        payload.setSales(new ArrayList<>(saleRepository.findAll()));
        payload.setSnapshots(new ArrayList<>(snapshotRepository.findAll()));
        return payload;
    }

    private VaultKeys getVaultKeys() {
        return new VaultKeys(HARD_CODED_AES_KEY, HARD_CODED_HMAC_KEY);
    }

    private void setSettingValue(String key, String value) {
        AppSetting setting = appSettingRepository.findById(key).orElseGet(() -> {
            AppSetting newSetting = new AppSetting();
            newSetting.setSettingKey(key);
            return newSetting;
        });
        setting.setSettingValue(value);
        appSettingRepository.save(setting);
    }

    private byte[] encrypt(String plainText, byte[] aesKey, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
    }

    private String decrypt(byte[] encrypted, byte[] aesKey, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private byte[] sign(byte[] iv, byte[] payload, byte[] hmacKey) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
        mac.update(iv);
        mac.update(payload);
        return mac.doFinal();
    }

    private Path resolveVaultFile(Path selectedPath) throws IOException {
        if (selectedPath == null || !Files.exists(selectedPath)) {
            return null;
        }

        if (Files.isRegularFile(selectedPath)) {
            return selectedPath;
        }

        Path hiddenVault = selectedPath.resolve(VAULT_FILENAME);
        if (Files.exists(hiddenVault)) {
            return hiddenVault;
        }

        try (var stream = Files.list(selectedPath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".bak"))
                    .findFirst()
                    .orElseThrow(() -> new IOException("No backup vault was found in the selected folder."));
        }
    }

    private void markAsHiddenSystem(Path path) {
        try {
            Files.setAttribute(path, "dos:hidden", true);
            Files.setAttribute(path, "dos:system", true);
        } catch (Exception ignored) {
            // Non-Windows or unsupported filesystems simply keep the dot-prefixed filename.
        }
    }

    private void makeReadOnly(Path path) {
        try {
            Files.setAttribute(path, "dos:readonly", true);
        } catch (Exception ignored) {
            path.toFile().setReadOnly();
        }
    }

    private String generateReadableSql(DatabaseBackupPayload payload) {
        StringBuilder sql = new StringBuilder();
        sql.append("-- Readable companion export generated by Inventory System").append(System.lineSeparator());
        sql.append("-- This file is not encrypted. Handle it carefully.").append(System.lineSeparator()).append(System.lineSeparator());

        sql.append("DELETE FROM Inventory_Snapshots;").append(System.lineSeparator());
        sql.append("DELETE FROM Sales_Records;").append(System.lineSeparator());
        sql.append("DELETE FROM Purchases;").append(System.lineSeparator());
        sql.append("DELETE FROM Product_Batches;").append(System.lineSeparator());
        sql.append("DELETE FROM Products;").append(System.lineSeparator());
        sql.append("DELETE FROM app_settings;").append(System.lineSeparator()).append(System.lineSeparator());

        appendAppSettings(sql, payload.getAppSettings());
        appendProducts(sql, payload.getProducts());
        appendProductBatches(sql, payload.getProductBatches());
        appendPurchases(sql, payload.getPurchases());
        appendSales(sql, payload.getSales());
        appendSnapshots(sql, payload.getSnapshots());

        return sql.toString();
    }

    private void appendAppSettings(StringBuilder sql, List<AppSetting> settings) {
        for (AppSetting setting : settings) {
            sql.append("INSERT INTO app_settings (setting_key, setting_value) VALUES (")
                    .append(sqlValue(setting.getSettingKey())).append(", ")
                    .append(sqlValue(setting.getSettingValue())).append(");")
                    .append(System.lineSeparator());
        }
    }

    private void appendProducts(StringBuilder sql, List<Product> products) {
        for (Product product : products) {
            sql.append("INSERT INTO Products (product_id, product_name, manufacturer, category, size_ml, current_stock, ")
                    .append("low_stock_threshold, purchase_price, selling_price, is_active, opening_stock, total_purchases, ")
                    .append("total_sales, created_date) VALUES (")
                    .append(sqlValue(product.getProductId())).append(", ")
                    .append(sqlValue(product.getProductName())).append(", ")
                    .append(sqlValue(product.getManufacturer())).append(", ")
                    .append(sqlValue(product.getCategory())).append(", ")
                    .append(sqlValue(product.getSizeMl())).append(", ")
                    .append(sqlValue(product.getCurrentStock())).append(", ")
                    .append(sqlValue(product.getLowStockThreshold())).append(", ")
                    .append(sqlValue(product.getPurchasePrice())).append(", ")
                    .append(sqlValue(product.getSellingPrice())).append(", ")
                    .append(sqlBoolean(product.getIsActive())).append(", ")
                    .append(sqlValue(product.getOpeningStock())).append(", ")
                    .append(sqlValue(product.getTotalPurchases())).append(", ")
                    .append(sqlValue(product.getTotalSales())).append(", ")
                    .append(sqlValue(product.getCreatedDate())).append(");")
                    .append(System.lineSeparator());
        }
    }

    private void appendProductBatches(StringBuilder sql, List<ProductBatch> batches) {
        for (ProductBatch batch : batches) {
            sql.append("INSERT INTO Product_Batches (batch_id, product_id, purchase_price, selling_price, current_stock_in_batch, batch_created_at) VALUES (")
                    .append(sqlValue(batch.getBatchId())).append(", ")
                    .append(sqlValue(batch.getProductId())).append(", ")
                    .append(sqlValue(batch.getPurchasePrice())).append(", ")
                    .append(sqlValue(batch.getSellingPrice())).append(", ")
                    .append(sqlValue(batch.getCurrentStockInBatch())).append(", ")
                    .append(sqlValue(batch.getBatchCreatedAt())).append(");")
                    .append(System.lineSeparator());
        }
    }

    private void appendPurchases(StringBuilder sql, List<PurchaseRecord> purchases) {
        for (PurchaseRecord purchase : purchases) {
            sql.append("INSERT INTO Purchases (purchase_id, product_id, quantity_bought, purchase_price, purchase_date) VALUES (")
                    .append(sqlValue(purchase.getPurchaseId())).append(", ")
                    .append(sqlValue(purchase.getProductId())).append(", ")
                    .append(sqlValue(purchase.getQuantityBought())).append(", ")
                    .append(sqlValue(purchase.getPurchasePrice())).append(", ")
                    .append(sqlValue(purchase.getPurchaseDate())).append(");")
                    .append(System.lineSeparator());
        }
    }

    private void appendSales(StringBuilder sql, List<SaleRecord> sales) {
        for (SaleRecord sale : sales) {
            sql.append("INSERT INTO Sales_Records (sale_id, product_id, quantity_sold, sale_price_at_time, sale_date) VALUES (")
                    .append(sqlValue(sale.getSaleId())).append(", ")
                    .append(sqlValue(sale.getProductId())).append(", ")
                    .append(sqlValue(sale.getQuantitySold())).append(", ")
                    .append(sqlValue(sale.getSalePriceAtTime())).append(", ")
                    .append(sqlValue(sale.getSaleDate())).append(");")
                    .append(System.lineSeparator());
        }
    }

    private void appendSnapshots(StringBuilder sql, List<InventorySnapshot> snapshots) {
        for (InventorySnapshot snapshot : snapshots) {
            sql.append("INSERT INTO Inventory_Snapshots (snapshot_id, product_id, record_date, opening_stock, quantity_purchased, quantity_sold, closing_stock) VALUES (")
                    .append(sqlValue(snapshot.getSnapshotId())).append(", ")
                    .append(sqlValue(snapshot.getProductId())).append(", ")
                    .append(sqlValue(snapshot.getRecordDate())).append(", ")
                    .append(sqlValue(snapshot.getOpeningStock())).append(", ")
                    .append(sqlValue(snapshot.getQuantityPurchased())).append(", ")
                    .append(sqlValue(snapshot.getQuantitySold())).append(", ")
                    .append(sqlValue(snapshot.getClosingStock())).append(");")
                    .append(System.lineSeparator());
        }
    }

    private String sqlBoolean(Boolean value) {
        if (value == null) {
            return "NULL";
        }
        return value ? "1" : "0";
    }

    private String sqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString();
        }
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        return "'" + value.toString().replace("'", "''") + "'";
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String safeString(Object value) {
        return Objects.toString(value, "");
    }

    private record VaultKeys(byte[] aesKey, byte[] hmacKey) {
    }

    public static class BackupResult {
        private final Path backupFolder;
        private final Path vaultFile;
        private final Path readableSqlFile;
        private final String securedAt;

        public BackupResult(Path backupFolder, Path vaultFile, Path readableSqlFile, String securedAt) {
            this.backupFolder = backupFolder;
            this.vaultFile = vaultFile;
            this.readableSqlFile = readableSqlFile;
            this.securedAt = securedAt;
        }

        public Path getBackupFolder() {
            return backupFolder;
        }

        public Path getVaultFile() {
            return vaultFile;
        }

        public Path getReadableSqlFile() {
            return readableSqlFile;
        }

        public String getSecuredAt() {
            return securedAt;
        }
    }

    public static class RestoreResult {
        private final boolean restored;
        private final boolean tampered;
        private final String message;

        private RestoreResult(boolean restored, boolean tampered, String message) {
            this.restored = restored;
            this.tampered = tampered;
            this.message = message;
        }

        public static RestoreResult success(String message) {
            return new RestoreResult(true, false, message);
        }

        public static RestoreResult tampered(String message) {
            return new RestoreResult(false, true, message);
        }

        public boolean isRestored() {
            return restored;
        }

        public boolean isTampered() {
            return tampered;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class VaultEnvelope {
        private int version;
        private String createdAt;
        private String cipher;
        private String signatureAlgorithm;
        private String iv;
        private String payload;
        private String signature;

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getCipher() {
            return cipher;
        }

        public void setCipher(String cipher) {
            this.cipher = cipher;
        }

        public String getSignatureAlgorithm() {
            return signatureAlgorithm;
        }

        public void setSignatureAlgorithm(String signatureAlgorithm) {
            this.signatureAlgorithm = signatureAlgorithm;
        }

        public String getIv() {
            return iv;
        }

        public void setIv(String iv) {
            this.iv = iv;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }

    public static class DatabaseBackupPayload {
        private List<AppSetting> appSettings = new ArrayList<>();
        private List<Product> products = new ArrayList<>();
        private List<ProductBatch> productBatches = new ArrayList<>();
        private List<PurchaseRecord> purchases = new ArrayList<>();
        private List<SaleRecord> sales = new ArrayList<>();
        private List<InventorySnapshot> snapshots = new ArrayList<>();

        public List<AppSetting> getAppSettings() {
            return appSettings;
        }

        public void setAppSettings(List<AppSetting> appSettings) {
            this.appSettings = appSettings;
        }

        public List<Product> getProducts() {
            return products;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public List<ProductBatch> getProductBatches() {
            return productBatches;
        }

        public void setProductBatches(List<ProductBatch> productBatches) {
            this.productBatches = productBatches;
        }

        public List<PurchaseRecord> getPurchases() {
            return purchases;
        }

        public void setPurchases(List<PurchaseRecord> purchases) {
            this.purchases = purchases;
        }

        public List<SaleRecord> getSales() {
            return sales;
        }

        public void setSales(List<SaleRecord> sales) {
            this.sales = sales;
        }

        public List<InventorySnapshot> getSnapshots() {
            return snapshots;
        }

        public void setSnapshots(List<InventorySnapshot> snapshots) {
            this.snapshots = snapshots;
        }
    }
}
