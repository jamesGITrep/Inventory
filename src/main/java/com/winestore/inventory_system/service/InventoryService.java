package com.winestore.inventory_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.winestore.inventory_system.model.DailyReport;
import com.winestore.inventory_system.model.InventorySnapshot;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.ProductBatch;
import com.winestore.inventory_system.model.PurchaseRecord;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.model.SnapshotRepository;
import com.winestore.inventory_system.model.AppSetting;
import com.winestore.inventory_system.model.ViewLastPurchased;
import com.winestore.inventory_system.repository.AppSettingRepository;
import com.winestore.inventory_system.repository.ProductBatchRepository;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.ProfitRepository;
import com.winestore.inventory_system.repository.PurchaseRepository;
import com.winestore.inventory_system.repository.SaleRepository;
import com.winestore.inventory_system.dto.ProductSummaryDTO;

@Service
public class InventoryService {

    private static final String MANAGER_PASSWORD_KEY = "manager_password";
    private static final String LEGACY_ADMIN_PASSWORD_KEY = "admin_password";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private ProfitRepository profitRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private ProductBatchRepository productBatchRepository;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Transactional
    public void recordSale(SaleRecord sale) {
        recordSaleAndUpdateStock(
                sale.getProductId(),
                sale.getQuantitySold(),
                sale.getSalePriceAtTime(),
                sale.getSaleDate() != null ? sale.getSaleDate() : LocalDate.now());
    }

    public boolean isDuplicate(String name, Integer size) {
        String cleanName = normalize(name);
        Integer normalizedSize = size == null ? 0 : size;
        return productRepository.findByIsActiveTrue().stream()
                .anyMatch(product -> normalize(product.getProductName()).equals(cleanName)
                        && safeInt(product.getSizeMl()) == normalizedSize);
    }

    @Transactional
    public void recordSaleAndUpdateStock(Integer productId, Integer quantitySold, BigDecimal salePrice) {
        recordSaleAndUpdateStock(productId, quantitySold, salePrice, LocalDate.now());
    }

    @Transactional
    public void recordSaleAndUpdateStock(Integer productId,
                                         Integer quantitySold,
                                         BigDecimal salePrice,
                                         LocalDate saleDate) {
        Product product = findProductById(productId);
        int soldQuantity = safeInt(quantitySold);
        int currentStock = safeInt(product.getCurrentStock());
        int newStock = currentStock - soldQuantity;

        if (newStock < 0) {
            throw new RuntimeException("Insufficient stock for " + product.getProductName());
        }

        product.setCurrentStock(newStock);
        product.setTotalSales(safeInt(product.getTotalSales()) + soldQuantity);
        productRepository.save(product);

        SaleRecord sale = new SaleRecord();
        sale.setProductId(productId);
        sale.setQuantitySold(soldQuantity);
        sale.setSalePriceAtTime(salePrice);
        sale.setSaleDate(saleDate != null ? saleDate : LocalDate.now());
        saleRepository.save(sale);

        // Deplete the stock from the oldest batches via FIFO
        processFIFOSale(productId, soldQuantity);

        updateSnapshotForDate(productId, sale.getSaleDate(), 0, soldQuantity);
    }

    @Transactional
    public void captureDailySnapshot(Integer productId) {
        Product product = findProductById(productId);

        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setProductId(productId);
        snapshot.setRecordDate(LocalDate.now());
        snapshot.setOpeningStock(safeInt(product.getCurrentStock()));
        snapshot.setQuantityPurchased(0);
        snapshot.setQuantitySold(0);
        snapshot.setClosingStock(safeInt(product.getCurrentStock()));
        snapshotRepository.save(snapshot);
    }

    public BigDecimal calculateProfitMargin(Integer productId, BigDecimal currentSellingPrice) {
        return profitRepository.findById(productId)
                .map(ViewLastPurchased::getLastPricePaid)
                .map(currentSellingPrice::subtract)
                .orElse(BigDecimal.ZERO);
    }

    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrue();
    }

    public List<String> getAllProductNames() {
        return getAllProducts().stream()
                .map(Product::getProductName)
                .filter(name -> name != null && !name.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public Product findActiveProductByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String normalizedName = normalize(name);
        return getAllProducts().stream()
                .filter(product -> normalize(product.getProductName()).equals(normalizedName))
                .findFirst()
                .orElse(null);
    }

    public Product findProductById(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.findByProductNameContainingIgnoreCaseAndIsActiveTrue(query.trim());
    }

    public List<DailyReport> generateDailyReport() {
        List<DailyReport> reports = new ArrayList<>();
        List<Product> products = getAllProducts();

        for (Product product : products) {
            List<SaleRecord> sales = saleRepository.findByProductIdAndSaleDate(product.getProductId(), LocalDate.now());
            if (!sales.isEmpty()) {
                int totalQuantity = sales.stream().mapToInt(sale -> safeInt(sale.getQuantitySold())).sum();
                BigDecimal totalRevenue = sales.stream()
                        .map(SaleRecord::getTotalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal margin = calculateProfitMargin(product.getProductId(), product.getSellingPrice());
                BigDecimal totalProfit = margin.multiply(BigDecimal.valueOf(totalQuantity));
                reports.add(new DailyReport(product.getProductName(), totalQuantity, totalRevenue, totalProfit));
            }
        }

        return reports;
    }

    public void captureAllSnapshots() {
        getAllProducts().forEach(product -> captureDailySnapshot(product.getProductId()));
    }

    @Transactional
    public void processBatchSale(List<SaleRecord> batchSales) {
        for (SaleRecord sale : batchSales) {
            recordSaleAndUpdateStock(
                    sale.getProductId(),
                    sale.getQuantitySold(),
                    sale.getSalePriceAtTime(),
                    sale.getSaleDate());
        }
    }

    @Transactional
    public void recordPurchase(Integer productId, Integer quantity, BigDecimal buyPrice) {
        recordPurchase(productId, quantity, buyPrice, null, LocalDate.now());
    }

    @Transactional
    public void recordPurchase(Integer productId, Integer quantity, BigDecimal buyPrice, LocalDate purchaseDate) {
        recordPurchase(productId, quantity, buyPrice, null, purchaseDate);
    }

    /**
     * Batch-aware purchase recording.
     *
     * <p>For an existing product:</p>
     * <ul>
     *   <li>Fetches the latest {@code Product_Batches} row for the product.</li>
     *   <li>If the incoming {@code buyPrice} and {@code sellingPrice} match the latest batch,
     *       the batch's {@code current_stock_in_batch} is simply incremented.</li>
     *   <li>If either price differs (price hike / drop), a new batch row is inserted with
     *       the new prices and the current timestamp.</li>
     * </ul>
     * The resulting {@code PurchaseRecord} is linked to whichever {@code batch_id} was used.
     */
    @Transactional
    public void recordPurchase(Integer productId,
                               Integer quantity,
                               BigDecimal buyPrice,
                               BigDecimal sellingPrice,
                               LocalDate purchaseDate) {
        Product product = findProductById(productId);
        int purchasedQuantity = safeInt(quantity);

        // --- Batch-aware logic ---
        BigDecimal effectiveSellingPrice = sellingPrice != null ? sellingPrice : product.getSellingPrice();

        Integer affectedBatchId = resolveBatch(productId, buyPrice, effectiveSellingPrice, purchasedQuantity);

        // --- Update the Products table ---
        product.setCurrentStock(safeInt(product.getCurrentStock()) + purchasedQuantity);
        product.setTotalPurchases(safeInt(product.getTotalPurchases()) + purchasedQuantity);
        product.setPurchasePrice(buyPrice);
        if (effectiveSellingPrice != null) {
            product.setSellingPrice(effectiveSellingPrice);
        }
        productRepository.save(product);

        // --- Persist the Purchase_Records row, linked to the batch ---
        PurchaseRecord record = new PurchaseRecord();
        record.setProductId(productId);
        record.setQuantityBought(purchasedQuantity);
        record.setPurchasePrice(buyPrice);
        record.setPurchaseDate(purchaseDate != null ? purchaseDate : LocalDate.now());
        record.setBatchId(affectedBatchId);
        purchaseRepository.save(record);

        updateSnapshotForDate(productId, record.getPurchaseDate(), purchasedQuantity, 0);
    }

    /**
     * Finds or creates a {@link ProductBatch} for the given product and price combination.
     *
     * @return the {@code batch_id} of the batch that was incremented or newly created
     */
    private Integer resolveBatch(Integer productId,
                                 BigDecimal buyPrice,
                                 BigDecimal sellingPrice,
                                 int quantity) {
        return productBatchRepository.findLatestBatchByProductId(productId)
                .map(latestBatch -> {
                    boolean sameBuyPrice    = pricesMatch(latestBatch.getPurchasePrice(), buyPrice);
                    boolean sameSellPrice   = pricesMatch(latestBatch.getSellingPrice(),  sellingPrice);

                    if (sameBuyPrice && sameSellPrice) {
                        // Prices match — just increment the existing batch stock
                        latestBatch.setCurrentStockInBatch(
                                latestBatch.getCurrentStockInBatch() + quantity);
                        productBatchRepository.save(latestBatch);
                        return latestBatch.getBatchId();
                    } else {
                        // Price hike / drop — create a new batch
                        return createNewBatch(productId, buyPrice, sellingPrice, quantity);
                    }
                })
                .orElseGet(() -> createNewBatch(productId, buyPrice, sellingPrice, quantity));
    }

    /** Inserts a new {@link ProductBatch} row and returns its generated {@code batch_id}. */
    private Integer createNewBatch(Integer productId,
                                   BigDecimal buyPrice,
                                   BigDecimal sellingPrice,
                                   int initialStock) {
        ProductBatch batch = new ProductBatch();
        batch.setProductId(productId);
        batch.setPurchasePrice(buyPrice);
        batch.setSellingPrice(sellingPrice != null ? sellingPrice : BigDecimal.ZERO);
        batch.setCurrentStockInBatch(initialStock);
        // batchCreatedAt is set automatically by @PrePersist
        return productBatchRepository.save(batch).getBatchId();
    }

    /**
     * Null-safe BigDecimal comparison (ignores scale, e.g. 100 == 100.00).
     */
    private boolean pricesMatch(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.compareTo(b) == 0;
    }

    // =========================================================================
    // BATCH CONVENIENCE METHODS
    // =========================================================================

    /**
     * Convenience alias used by the UI — delegates directly to the full
     * batch-aware {@link #recordPurchase} implementation.
     *
     * @param productId  the product being restocked
     * @param buyPrice   cost price per unit in this delivery
     * @param sellPrice  intended retail price for this delivery
     * @param qty        number of units received
     */
    @Transactional
    public void addStockToProduct(Integer productId,
                                  BigDecimal buyPrice,
                                  BigDecimal sellPrice,
                                  Integer qty) {
        recordPurchase(productId, qty, buyPrice, sellPrice, LocalDate.now());
    }

    /**
     * FIFO sale processor: walks batches oldest-first and drains stock until
     * the full sold quantity is accounted for.
     *
     * <p>Each batch touched is saved immediately so the depletion is atomic
     * within the surrounding {@code @Transactional} boundary.</p>
     *
     * @param productId       product being sold
     * @param quantityToSell  total units to deduct from FIFO batches
     * @throws RuntimeException if there is insufficient batch stock in aggregate
     */
    @Transactional
    public void processFIFOSale(Integer productId, int quantityToSell) {
        int remaining = quantityToSell;

        while (remaining > 0) {
            final int remainingSnapshot = remaining;
            ProductBatch oldest = productBatchRepository
                    .findOldestActiveBatchByProductId(productId)
                    .orElseThrow(() -> new RuntimeException(
                            "Insufficient batch stock for product " + productId +
                            ". Could not fulfil " + remainingSnapshot + " more unit(s)."));

            int available = oldest.getCurrentStockInBatch();

            if (available >= remaining) {
                // This batch covers the rest of the sale
                oldest.setCurrentStockInBatch(available - remaining);
                remaining = 0;
            } else {
                // Drain this batch completely and continue to the next
                oldest.setCurrentStockInBatch(0);
                remaining -= available;
            }

            productBatchRepository.save(oldest);
        }
    }

    /**
     * Returns all batches for a product ordered oldest-first, used by
     * {@code BatchDetailsController} to populate the history popup.
     */
    public List<ProductBatch> getBatchesForProduct(Integer productId) {
        return productBatchRepository.findAllByProductIdOrderByBatchIdAsc(productId);
    }

    /**
     * Builds a {@link ProductSummaryDTO} for a single product by aggregating
     * its batch data in-memory (avoids N additional DB queries on the dashboard).
     */
    public ProductSummaryDTO buildProductSummary(Product product) {
        List<ProductBatch> batches =
                productBatchRepository.findAllByProductIdOrderByBatchIdAsc(product.getProductId());

        List<ProductBatch> active = batches.stream()
                .filter(b -> b.getCurrentStockInBatch() > 0)
                .collect(Collectors.toList());

        int totalStock = active.stream().mapToInt(ProductBatch::getCurrentStockInBatch).sum();

        BigDecimal minBuy = active.stream()
                .map(ProductBatch::getPurchasePrice)
                .filter(p -> p != null)
                .min(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal maxBuy = active.stream()
                .map(ProductBatch::getPurchasePrice)
                .filter(p -> p != null)
                .max(Comparator.naturalOrder())
                .orElse(null);

        // Latest batch = last in the list (highest batch_id)
        BigDecimal currentSell = batches.isEmpty() ? product.getSellingPrice()
                : batches.get(batches.size() - 1).getSellingPrice();

        return new ProductSummaryDTO(
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getManufacturer(),
                product.getSizeMl(),
                totalStock,
                active.size(),
                minBuy,
                maxBuy,
                currentSell);
    }

    public void saveProduct(Product product) {
        if (product.getOpeningStock() == null) {
            product.setOpeningStock(safeInt(product.getCurrentStock()));
        }
        if (product.getCurrentStock() == null) {
            product.setCurrentStock(safeInt(product.getOpeningStock()));
        }
        if (product.getLowStockThreshold() == null) {
            product.setLowStockThreshold(10);
        }
        if (product.getTotalPurchases() == null) {
            product.setTotalPurchases(0);
        }
        if (product.getTotalSales() == null) {
            product.setTotalSales(0);
        }
        if (product.getIsActive() == null) {
            product.setIsActive(true);
        }
        productRepository.save(product);
    }

    public int getTotalStock() {
        return getAllProducts().stream()
                .mapToInt(product -> safeInt(product.getCurrentStock()))
                .sum();
    }

    public BigDecimal getTotalProfit() {
        return getAllProducts().stream()
                .map(Product::getProfit)
                .filter(profit -> profit != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<String> getDistinctCategories() {
        return getAllProducts().stream()
                .map(Product::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<Product> searchAndFilterProducts(String query, String category) {
        return getAllProducts().stream()
                .filter(product -> query == null || query.isBlank()
                        || normalize(product.getProductName()).contains(normalize(query)))
                .filter(product -> category == null || category.isBlank()
                        || "All Categories".equals(category)
                        || category.equals(product.getCategory()))
                .toList();
    }

    public List<Product> getProductsForDate(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        // Handle future dates
        if (targetDate.isAfter(LocalDate.now())) {
            return List.of();
        }

        List<SaleRecord> allSales = saleRepository.findAll();
        List<PurchaseRecord> allPurchases = purchaseRepository.findAll();

        // Find the earliest date in the system (first activity)
        LocalDate earliestActivity = findEarliestActivityDate(allSales, allPurchases);

        // If targetDate is before the first recorded activity, show nothing
        if (earliestActivity != null && targetDate.isBefore(earliestActivity)) {
            return List.of();
        }

        List<Product> products = getAllProducts();

        Map<Integer, List<SaleRecord>> salesByProduct = allSales.stream()
                .collect(Collectors.groupingBy(SaleRecord::getProductId));
        Map<Integer, List<PurchaseRecord>> purchasesByProduct = allPurchases.stream()
                .collect(Collectors.groupingBy(PurchaseRecord::getProductId));

        return products.stream()
                .filter(product -> {
                    LocalDate createdDate = product.getCreatedDate();
                    if (createdDate != null) {
                        return !createdDate.isAfter(targetDate);
                    }
                    
                    // Fallback: check if there's any activity for this product on or before the target date
                    List<SaleRecord> productSales = salesByProduct.getOrDefault(product.getProductId(), List.of());
                    List<PurchaseRecord> productPurchases = purchasesByProduct.getOrDefault(product.getProductId(), List.of());
                    
                    boolean hadSale = productSales.stream()
                            .anyMatch(s -> s.getSaleDate() != null && !s.getSaleDate().isAfter(targetDate));
                    boolean hadPurchase = productPurchases.stream()
                            .anyMatch(p -> p.getPurchaseDate() != null && !p.getPurchaseDate().isAfter(targetDate));
                            
                    return hadSale || hadPurchase;
                })
                .map(product -> buildHistoricalProduct(
                        product,
                        targetDate,
                        salesByProduct.getOrDefault(product.getProductId(), List.of()),
                        purchasesByProduct.getOrDefault(product.getProductId(), List.of())))
                .toList();
    }

    private LocalDate findEarliestActivityDate(List<SaleRecord> sales, List<PurchaseRecord> purchases) {
        LocalDate earliestSale = sales.stream()
                .map(SaleRecord::getSaleDate)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate earliestPurchase = purchases.stream()
                .map(PurchaseRecord::getPurchaseDate)
                .filter(d -> d != null)
                .min(LocalDate::compareTo)
                .orElse(null);

        if (earliestSale == null) return earliestPurchase;
        if (earliestPurchase == null) return earliestSale;
        return earliestSale.isBefore(earliestPurchase) ? earliestSale : earliestPurchase;
    }

    private Product buildHistoricalProduct(Product product,
                                           LocalDate targetDate,
                                           List<SaleRecord> sales,
                                           List<PurchaseRecord> purchases) {
        int salesOnDate = sales.stream()
                .filter(sale -> targetDate.equals(sale.getSaleDate()))
                .mapToInt(sale -> safeInt(sale.getQuantitySold()))
                .sum();
        int salesAfterDate = sales.stream()
                .filter(sale -> sale.getSaleDate() != null && sale.getSaleDate().isAfter(targetDate))
                .mapToInt(sale -> safeInt(sale.getQuantitySold()))
                .sum();

        int purchasesOnDate = purchases.stream()
                .filter(purchase -> targetDate.equals(purchase.getPurchaseDate()))
                .mapToInt(purchase -> safeInt(purchase.getQuantityBought()))
                .sum();
        int purchasesAfterDate = purchases.stream()
                .filter(purchase -> purchase.getPurchaseDate() != null && purchase.getPurchaseDate().isAfter(targetDate))
                .mapToInt(purchase -> safeInt(purchase.getQuantityBought()))
                .sum();

        int closingStock = safeInt(product.getCurrentStock()) - purchasesAfterDate + salesAfterDate;
        int openingStock = closingStock - purchasesOnDate + salesOnDate;

        Product copy = new Product();
        copy.setProductId(product.getProductId());
        copy.setProductName(product.getProductName());
        copy.setCategory(product.getCategory());
        copy.setManufacturer(product.getManufacturer());
        copy.setSizeMl(product.getSizeMl());
        copy.setPurchasePrice(product.getPurchasePrice());
        copy.setSellingPrice(product.getSellingPrice());
        copy.setLowStockThreshold(product.getLowStockThreshold());
        copy.setIsActive(product.getIsActive());
        copy.setCreatedDate(product.getCreatedDate());
        copy.setOpeningStock(Math.max(openingStock, 0));
        copy.setTotalPurchases(Math.max(purchasesOnDate, 0));
        copy.setTotalSales(Math.max(salesOnDate, 0));
        copy.setCurrentStock(Math.max(closingStock, 0));
        return copy;
    }

    private void updateSnapshotForDate(Integer productId,
                                       LocalDate recordDate,
                                       int purchasedQuantity,
                                       int soldQuantity) {
        if (recordDate == null) {
            return;
        }

        snapshotRepository.findAll().stream()
                .filter(snapshot -> snapshot.getProductId().equals(productId) && recordDate.equals(snapshot.getRecordDate()))
                .findFirst()
                .ifPresent(snapshot -> {
                    int updatedPurchased = safeInt(snapshot.getQuantityPurchased()) + purchasedQuantity;
                    int updatedSold = safeInt(snapshot.getQuantitySold()) + soldQuantity;
                    snapshot.setQuantityPurchased(updatedPurchased);
                    snapshot.setQuantitySold(updatedSold);
                    snapshot.setClosingStock(safeInt(snapshot.getOpeningStock()) + updatedPurchased - updatedSold);
                    snapshotRepository.save(snapshot);
                });
    }

    public List<SaleRecord> getSalesBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return saleRepository.findAll();
        }
        if (startDate.equals(endDate)) {
            return saleRepository.findBySaleDate(startDate);
        }
        return saleRepository.findBySaleDateBetween(startDate, endDate);
    }

    public BigDecimal calculateProfitForSales(List<SaleRecord> sales) {
        Map<Integer, Product> productMap = getAllProducts().stream()
                .collect(Collectors.toMap(Product::getProductId, Function.identity()));

        BigDecimal totalProfit = BigDecimal.ZERO;
        for (SaleRecord sale : sales) {
            Product product = productMap.get(sale.getProductId());
            if (product != null && product.getPurchasePrice() != null && sale.getSalePriceAtTime() != null) {
                BigDecimal profitPerUnit = sale.getSalePriceAtTime().subtract(product.getPurchasePrice());
                totalProfit = totalProfit.add(profitPerUnit.multiply(BigDecimal.valueOf(safeInt(sale.getQuantitySold()))));
            }
        }
        return totalProfit;
    }

    public int getSalesCountOn(LocalDate date) {
        if (date == null) {
            return 0;
        }
        return saleRepository.findBySaleDate(date).size();
    }

    public long getPurchaseCountForProduct(Integer productId) {
        return purchaseRepository.findAll().stream()
                .filter(record -> record.getProductId().equals(productId))
                .count();
    }

    public long getSaleCountForProduct(Integer productId) {
        return saleRepository.findAll().stream()
                .filter(record -> record.getProductId().equals(productId))
                .count();
    }

    public boolean isManagerPasswordConfigured() {
        String storedPassword = getStoredManagerPassword();
        return storedPassword != null && !storedPassword.isBlank();
    }

    public boolean validateManagerPassword(String candidatePassword) {
        if (candidatePassword == null || candidatePassword.isBlank()) {
            return false;
        }
        String storedPassword = getStoredManagerPassword();
        return storedPassword != null && !storedPassword.isBlank() && storedPassword.equals(candidatePassword);
    }

    @Transactional
    public void updateManagerPassword(String newPassword) {
        String trimmedPassword = newPassword == null ? "" : newPassword.trim();

        AppSetting passwordSetting = getOrCreateSetting(MANAGER_PASSWORD_KEY);
        passwordSetting.setSettingValue(trimmedPassword);
        appSettingRepository.save(passwordSetting);

        AppSetting legacyPasswordSetting = getOrCreateSetting(LEGACY_ADMIN_PASSWORD_KEY);
        legacyPasswordSetting.setSettingValue(trimmedPassword);
        appSettingRepository.save(legacyPasswordSetting);
    }

    @Transactional
    protected AppSetting getManagerPasswordSetting() {
        return getOrCreateSetting(MANAGER_PASSWORD_KEY);
    }

    private String getStoredManagerPassword() {
        AppSetting primarySetting = getOrCreateSetting(MANAGER_PASSWORD_KEY);
        String primaryValue = primarySetting.getSettingValue();
        if (primaryValue != null && !primaryValue.isBlank()) {
            return primaryValue;
        }

        return appSettingRepository.findById(LEGACY_ADMIN_PASSWORD_KEY)
                .map(AppSetting::getSettingValue)
                .orElse("");
    }

    private AppSetting getOrCreateSetting(String settingKey) {
        return appSettingRepository.findById(settingKey)
                .orElseGet(() -> {
                    AppSetting setting = new AppSetting();
                    setting.setSettingKey(settingKey);
                    setting.setSettingValue("");
                    return appSettingRepository.save(setting);
                });
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ENGLISH);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
