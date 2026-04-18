package com.winestore.inventory_system.controller;

import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.service.InventoryService;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class MainController {

    @Autowired
    private InventoryService inventoryService;

    // --- LEFT SIDE: Inventory Table (Eesha) ---
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Integer> colSize;
    @FXML private TableColumn<Product, BigDecimal> colSellingPrice;
    @FXML private TextField searchField;

    // --- RIGHT SIDE: Batch Sale / Cart (James) ---
    @FXML private TableView<SaleRecord> cartTable;
    @FXML private TableColumn<SaleRecord, String> colCartProduct;
    @FXML private TableColumn<SaleRecord, Integer> colCartQty;
    @FXML private TableColumn<SaleRecord, BigDecimal> colCartPrice;
    @FXML private Label lblBillTotal;

    // Data Holders
    private ObservableList<Product> productData = FXCollections.observableArrayList();
    private ObservableList<SaleRecord> currentBill = FXCollections.observableArrayList();
    private PauseTransition searchDelay;

    @FXML
    public void initialize() {
        // 1. Setup Inventory Table Columns
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        colSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));

        // 2. Setup Cart Table Columns
        colCartProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        colCartPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));

        // 3. Bind Data to Tables
        inventoryTable.setItems(productData);
        cartTable.setItems(currentBill);

        // 4. Initial Data Load
        refreshTable();

        // 5. Debounced Search Logic
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDelay != null) {
                searchDelay.stop();
            }
            searchDelay = new PauseTransition(Duration.millis(300));
            searchDelay.setOnFinished(event -> filterInventory(newValue));
            searchDelay.play();
        });

        // 6. Low Stock Highlighting (Eesha & Lisa)
        inventoryTable.setRowFactory(tv -> new javafx.scene.control.TableRow<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.getCurrentStock() <= item.getLowStockThreshold()) {
                    setStyle("-fx-background-color: #ffcccc;"); // Light Red highlight
                } else {
                    setStyle("");
                }
            }
        });
    }

    // --- ACTIONS & LOGIC ---

    @FXML
    public void refreshTable() {
        List<Product> products = inventoryService.getAllProducts();
        productData.setAll(products);
    }

    private void filterInventory(String query) {
        System.out.println("Hardened Search Triggered for: " + query);
        List<Product> filteredResults = inventoryService.searchProducts(query);
        productData.setAll(filteredResults);
    }

    @FXML
    private void handleAddToBill() {
        Product selectedProduct = inventoryTable.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            System.out.println("Adding " + selectedProduct.getProductName() + " to bill.");

            SaleRecord tempSale = new SaleRecord();
            tempSale.setProductId(selectedProduct.getProductId());
            tempSale.setProductName(selectedProduct.getProductName());
            tempSale.setQuantitySold(1);
            tempSale.setSalePriceAtTime(selectedProduct.getSellingPrice());
            currentBill.add(tempSale);
            updateBillTotal();
        }
    }

    @FXML
    private void handleRemoveFromBill() {
        SaleRecord selected = cartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            currentBill.remove(selected);
            updateBillTotal();
        }
    }

    @FXML
    private void handleConfirmBatchSale() {
        if (!currentBill.isEmpty()) {
            System.out.println("Processing Batch Sale for " + currentBill.size() + " items.");
            inventoryService.processBatchSale(currentBill);
            
            currentBill.clear();
            refreshTable();
            updateBillTotal();
        }
    }

    private void updateBillTotal() {
        BigDecimal total = currentBill.stream()
            .map(s -> s.getUnitPrice().multiply(BigDecimal.valueOf(s.getQuantitySold())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        lblBillTotal.setText(String.format("₹%.2f", total));
    }

    @FXML
    private void handleDelete() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            System.out.println("Deleting item: " + selected.getProductName());
            // Future Sensitive Operations logic
        }
    }

                @FXML
        private void handleLowStockAlertClick() {
            System.out.println("Filtering for Low Stock Items...");
            
            // Fetch only items where stock is less than or equal to threshold
            List<Product> lowStockItems = productData.stream()
                .filter(p -> p.getCurrentStock() <= p.getLowStockThreshold())
                .toList();
            
            productData.setAll(lowStockItems);
            inventoryTable.setItems(productData);
        }

}