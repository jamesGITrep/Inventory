package com.winestore.inventory_system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
import com.winestore.inventory_system.model.PurchaseRecord;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.model.SnapshotRepository;
import com.winestore.inventory_system.model.ViewLastPurchased;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.ProfitRepository;
import com.winestore.inventory_system.repository.PurchaseRepository;
import com.winestore.inventory_system.repository.SaleRepository;

@Service
public class InventoryService {

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
        recordPurchase(productId, quantity, buyPrice, LocalDate.now());
    }

    @Transactional
    public void recordPurchase(Integer productId, Integer quantity, BigDecimal buyPrice, LocalDate purchaseDate) {
        Product product = findProductById(productId);
        int purchasedQuantity = safeInt(quantity);

        product.setCurrentStock(safeInt(product.getCurrentStock()) + purchasedQuantity);
        product.setTotalPurchases(safeInt(product.getTotalPurchases()) + purchasedQuantity);
        product.setPurchasePrice(buyPrice);
        productRepository.save(product);

        PurchaseRecord record = new PurchaseRecord();
        record.setProductId(productId);
        record.setQuantityBought(purchasedQuantity);
        record.setPurchasePrice(buyPrice);
        record.setPurchaseDate(purchaseDate != null ? purchaseDate : LocalDate.now());
        purchaseRepository.save(record);
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
        List<Product> products = getAllProducts();

        Map<Integer, List<SaleRecord>> salesByProduct = saleRepository.findAll().stream()
                .collect(Collectors.groupingBy(SaleRecord::getProductId));
        Map<Integer, List<PurchaseRecord>> purchasesByProduct = purchaseRepository.findAll().stream()
                .collect(Collectors.groupingBy(PurchaseRecord::getProductId));

        return products.stream()
                .map(product -> buildHistoricalProduct(
                        product,
                        targetDate,
                        salesByProduct.getOrDefault(product.getProductId(), List.of()),
                        purchasesByProduct.getOrDefault(product.getProductId(), List.of())))
                .toList();
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
        copy.setOpeningStock(Math.max(openingStock, 0));
        copy.setTotalPurchases(Math.max(purchasesOnDate, 0));
        copy.setTotalSales(Math.max(salesOnDate, 0));
        copy.setCurrentStock(Math.max(closingStock, 0));
        return copy;
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

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ENGLISH);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
