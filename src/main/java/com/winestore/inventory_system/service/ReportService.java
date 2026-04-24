package com.winestore.inventory_system.service;

import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.ProductAnalysisRow;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.repository.ProductRepository;
import com.winestore.inventory_system.repository.SaleRepository;
import com.winestore.inventory_system.repository.PurchaseRepository;
import com.winestore.inventory_system.dto.PurchaseHistoryDTO;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PurchaseRepository purchaseRepository;

    public List<ProductAnalysisRow> getProductWiseAnalysis() {
        List<SaleRecord> allSales = saleRepository.findAll();
        Map<Integer, List<SaleRecord>> salesByProduct = allSales.stream()
                .collect(Collectors.groupingBy(SaleRecord::getProductId));

        List<ProductAnalysisRow> analysisData = new ArrayList<>();
        Map<Integer, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        salesByProduct.forEach((productId, sales) -> {
            Product p = productMap.get(productId);
            if (p != null) {
                int volume = sales.stream()
                        .mapToInt(s -> s.getQuantitySold() != null ? s.getQuantitySold() : 0)
                        .sum();
                
                BigDecimal revenue = sales.stream()
                        .map(s -> {
                            BigDecimal price = s.getSalePriceAtTime() != null ? s.getSalePriceAtTime() : BigDecimal.ZERO;
                            BigDecimal qty = BigDecimal.valueOf(s.getQuantitySold() != null ? s.getQuantitySold() : 0);
                            return price.multiply(qty);
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                BigDecimal cost = BigDecimal.ZERO;
                if (p.getPurchasePrice() != null) {
                    BigDecimal pPrice = p.getPurchasePrice();
                    cost = sales.stream()
                            .map(s -> {
                                BigDecimal qty = BigDecimal.valueOf(s.getQuantitySold() != null ? s.getQuantitySold() : 0);
                                return pPrice.multiply(qty);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                }
                
                BigDecimal profit = revenue.subtract(cost);

                analysisData.add(new ProductAnalysisRow(
                        productId,
                        p.getProductName(),
                        p.getCategory(),
                        p.getSizeMl(),
                        volume,
                        revenue.doubleValue(),
                        profit.doubleValue()
                ));
            }
        });

        // Sort by volume descending
        return analysisData.stream()
                .sorted((a, b) -> b.getTotalVolume().compareTo(a.getTotalVolume()))
                .collect(Collectors.toList());
    }

    public Map<String, Double> getCategoryProfitMap() {
        List<ProductAnalysisRow> data = getProductWiseAnalysis();
        return data.stream()
                .collect(Collectors.groupingBy(
                        row -> {
                            String category = row.getCategory();
                            return (category == null || category.isBlank()) ? "Uncategorized" : category;
                        },
                        Collectors.summingDouble(ProductAnalysisRow::getTotalProfit)
                ));
    }

    public List<PurchaseHistoryDTO> getPurchaseHistory(LocalDate start, LocalDate end, String category) {
        return purchaseRepository.findPurchaseHistory(start, end, category);
    }
}
