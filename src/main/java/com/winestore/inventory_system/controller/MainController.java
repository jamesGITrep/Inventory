package com.winestore.inventory_system.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.service.InventoryService;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

@Component
public class MainController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    // --- Inventory Table Columns ---
    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Integer> colSize; 
    @FXML private TableColumn<Product, Integer> colOpeningStock;
    @FXML private TableColumn<Product, Integer> colPurchases;
    @FXML private TableColumn<Product, Integer> colSales;
    @FXML private TableColumn<Product, Integer> colStock; // Closing Stock
    @FXML private TableColumn<Product, BigDecimal> colPurchasePrice;
    @FXML private TableColumn<Product, BigDecimal> colSellingPrice;
    @FXML private TableColumn<Product, String> colStatus;

    // --- Dashboard UI Elements ---
    @FXML private TextField searchField;
    @FXML private Label lblTotalProfit;
    @FXML private Label lblLowStockCount;

    // --- Add Item Modal Fields ---
    @FXML private TextField txtProductName;
    @FXML private TextField txtSize;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtPurchasePrice;
    @FXML private TextField txtSellingPrice;

    // --- Record Purchase Fields ---
    @FXML private ComboBox<String> comboProductName;
    @FXML private Label lblCurrentStock;
    @FXML private Label lblLastPrice;

    private ObservableList<Product> productData = FXCollections.observableArrayList();
    private PauseTransition searchDelay;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl")); 
        colOpeningStock.setCellValueFactory(new PropertyValueFactory<>("openingStock"));
        colPurchases.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        colSales.setCellValueFactory(new PropertyValueFactory<>("totalSales"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("stockStatus"));

        inventoryTable.setItems(productData);
        refreshTable();
        setupAutoSuggestion();

        if (productData.isEmpty()) {
            javafx.application.Platform.runLater(this::showImportPrompt);
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (searchDelay != null) searchDelay.stop();
            searchDelay = new PauseTransition(Duration.millis(300));
            searchDelay.setOnFinished(event -> filterInventory(newValue));
            searchDelay.play();
        });

        inventoryTable.setRowFactory(tv -> new javafx.scene.control.TableRow<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item.getCurrentStock() <= item.getLowStockThreshold()) {
                    setStyle("-fx-background-color: #ffcccc;"); 
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void openModal(String fxmlPath, String title) {
        try {
            java.net.URL location = getClass().getResource(fxmlPath);
            if (location == null) {
                System.err.println("CRITICAL: FXML Not Found at " + fxmlPath);
                new Alert(Alert.AlertType.ERROR, "File Missing: " + fxmlPath).show();
                return;
            }

            FXMLLoader loader = new FXMLLoader(location);
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(inventoryTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshTable();
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Could not load: " + fxmlPath).show();
        }
    }

    @FXML private void handleAddItem() { openModal("/view/AddItemModal.fxml", "Add New Item"); }

    @FXML private void handleRecordPurchase() { 
        openModal("/view/RecordPurchaseModal.fxml", "Record Purchase"); 
    }

    @FXML private void handleBatchSalesEntry() { openModal("/view/BatchSalesView.fxml", "End-of-Day Sales"); }

    @FXML
    private void handleExportCSV() {
        String userHome = System.getProperty("user.home");
        // Using double backslashes for reliable Windows pathing
        File file = new File(userHome + "\\Desktop\\Complete_Inventory_Master.csv");
        
        try (FileWriter writer = new FileWriter(file)) {
            // Write CSV Header
            writer.write("ITEM ID,WINE NAME,CATEGORY,SIZE (ml),OPENING STOCK,PURCHASES,SALES,CLOSING STOCK,PURCHASE PRICE,SELLING PRICE,STOCK ALERT\n");
            
            // Write Product Data
            for (Product p : productData) {
                String sizeDisplay = (p.getSizeMl() != null) ? p.getSizeMl() + "ml" : "N/A";
                writer.write(String.format("%d,%s,%s,%s,%d,%d,%d,%d,%.2f,%.2f,%s\n", 
                    p.getProductId(), 
                    p.getProductName(), 
                    "Wine", 
                    sizeDisplay, 
                    p.getOpeningStock(), 
                    p.getTotalPurchases(), 
                    p.getTotalSales(), 
                    p.getCurrentStock(), 
                    p.getPurchasePrice() != null ? p.getPurchasePrice() : 0.00, 
                    p.getSellingPrice() != null ? p.getSellingPrice() : 0.00, 
                    p.getStockStatus()));
            }
            
            writer.flush();
            System.out.println(">>> Export Successful: " + file.getAbsolutePath());

            // --- RESTORED CONFIRMATION MESSAGE ---
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Status");
            alert.setHeaderText(null);
            alert.setContentText("Inventory Snapshot has been exported to your Desktop successfully!");
            alert.showAndWait();

        } catch (IOException e) { 
            System.err.println("Export Failed: " + e.getMessage());
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Export Error");
            errorAlert.setContentText("Failed to export CSV: " + e.getMessage());
            errorAlert.showAndWait();
        }
    }

    @FXML
    private void handleImportCSV() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup CSV");
        File selectedFile = fileChooser.showOpenDialog(inventoryTable.getScene().getWindow());
        if (selectedFile != null) {
            try (BufferedReader br = new BufferedReader(new FileReader(selectedFile))) {
                String line; br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    Product p = new Product();
                    p.setProductName(data[1].trim());
                    String sizeVal = data[3].replace("ml", "").replace("N/A", "0").trim();
                    p.setSizeMl(Integer.parseInt(sizeVal));
                    p.setCurrentStock(Integer.parseInt(data[7].trim()));
                    p.setSellingPrice(new BigDecimal(data[9].trim()));
                    p.setLowStockThreshold(10);
                    p.setIsActive(true);
                    inventoryService.saveProduct(p);
                }
                refreshTable();
            } catch (Exception e) { System.err.println("Import Error"); }
        }
    }

    private void showImportPrompt() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("System Setup");
        alert.setHeaderText("No Data Found");
        alert.setContentText("Import backup CSV?");
        ButtonType importBtn = new ButtonType("Import");
        ButtonType startFresh = new ButtonType("Fresh", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(importBtn, startFresh);
        alert.showAndWait().ifPresent(type -> { if (type == importBtn) handleImportCSV(); });
    }

    private void setupAutoSuggestion() {
        try {
            List<String> suggestions = productData.stream().map(p -> p.getProductName().trim()).distinct().collect(Collectors.toList());
            if (!suggestions.isEmpty()) TextFields.bindAutoCompletion(searchField, suggestions);
        } catch (Exception e) { }
    }

    @FXML 
    public void refreshTable() { 
        List<Product> products = inventoryService.getAllProducts();
        productData.setAll(products); 
        lblLowStockCount.setText(String.valueOf(products.stream().filter(p -> p.getCurrentStock() <= p.getLowStockThreshold()).count()));
        setupAutoSuggestion(); 
    }

    private void filterInventory(String query) { productData.setAll(inventoryService.searchProducts(query)); }
    
    @FXML
    private void handleSaveNewItem() {
        try {
            Product p = new Product();
            p.setProductName(txtProductName.getText());
            p.setSizeMl(Integer.parseInt(txtSize.getText()));
            p.setCurrentStock(Integer.parseInt(txtQuantity.getText()));
            p.setPurchasePrice(new BigDecimal(txtPurchasePrice.getText()));
            p.setSellingPrice(new BigDecimal(txtSellingPrice.getText()));
            p.setLowStockThreshold(10);
            p.setIsActive(true);

            inventoryService.saveProduct(p);
            handleCancelModal();
            refreshTable();
        } catch (Exception e) { System.err.println("Save Error: " + e.getMessage()); }
    }

    @FXML
    private void handleCancelModal() {
        Stage stage = (Stage) inventoryTable.getScene().getWindow();
        // Fallback for modal windows
        if (txtProductName != null && txtProductName.getScene() != null) stage = (Stage) txtProductName.getScene().getWindow();
        else if (comboProductName != null && comboProductName.getScene() != null) stage = (Stage) comboProductName.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleLowStockAlertClick() {
        List<Product> lowStockItems = inventoryService.getAllProducts().stream()
                .filter(p -> p.getCurrentStock() <= p.getLowStockThreshold())
                .toList();
        productData.setAll(lowStockItems);
    }

    @FXML
    private void handleTotalProductsClick() {
        refreshTable();
        if (searchField != null) searchField.clear();
    }

    @FXML private void handleAddToBill() { System.out.println("Adding to bill..."); }
    @FXML private void handleConfirmBatchSale() { System.out.println("Confirming sale..."); }
    @FXML private void handleRemoveFromBill() { System.out.println("Clearing cart..."); }

    @FXML
    private void handleSavePurchase() {
        try {
            String productName = comboProductName.getEditor().getText();
            String qtyStr = txtQuantity.getText();
            String priceStr = txtPurchasePrice.getText();

            if (productName == null || productName.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Please enter a product name").show();
                return;
            }

            Product existingProduct = inventoryService.getAllProducts().stream()
                    .filter(p -> p.getProductName().equalsIgnoreCase(productName.trim()))
                    .findFirst().orElse(null);

            if (existingProduct != null) {
                existingProduct.setCurrentStock(existingProduct.getCurrentStock() + Integer.parseInt(qtyStr));
                existingProduct.setPurchasePrice(new BigDecimal(priceStr));
                inventoryService.saveProduct(existingProduct);
            } else {
                Product newP = new Product();
                newP.setProductName(productName.trim());
                newP.setCurrentStock(Integer.parseInt(qtyStr));
                newP.setPurchasePrice(new BigDecimal(priceStr));
                newP.setSellingPrice(new BigDecimal(priceStr).multiply(new BigDecimal("1.2"))); // Default 20% markup
                newP.setLowStockThreshold(10);
                newP.setIsActive(true);
                inventoryService.saveProduct(newP);
            }
            handleCancelModal();
            refreshTable();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error: " + e.getMessage()).show();
        }
    }
}