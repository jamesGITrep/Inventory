package com.winestore.inventory_system.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.winestore.inventory_system.model.CartItem;
import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.PurchaseCartItem;
import com.winestore.inventory_system.model.SaleRecord;
import com.winestore.inventory_system.service.InventoryService;
import com.winestore.inventory_system.service.SecurityVaultService;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Component
public class MainController {

    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.ENGLISH);
    private static final DateTimeFormatter CHIP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
    private static final int PAGE_SIZE = 10;

    private static final String ACTIVE_CARD =
            "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 18 20; "
                    + "-fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.2), 15, 0, 0, 5); "
                    + "-fx-border-color: #bfdbfe; -fx-border-width: 2; -fx-border-radius: 12; -fx-cursor: hand;";
    private static final String INACTIVE_CARD =
            "-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 18 20; "
                    + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 8, 0, 0, 3); -fx-cursor: hand;";

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SecurityVaultService securityVaultService;

    @Autowired
    private ConfigurableApplicationContext springContext;

    @FXML private StackPane contentArea;
    @FXML private VBox dashboardView;

    @FXML private Button btnNavInventory;
    @FXML private Button btnNavAnalytics;
    @FXML private Button btnNavHistory;
    @FXML private Button btnNavPurchaseHistory;
    @FXML private Button btnSettings;

    @FXML private VBox cardTotalProducts;
    @FXML private VBox cardTotalStock;
    @FXML private VBox cardLowStock;
    @FXML private VBox cardRecentSales;

    @FXML private Label lblTotalProducts;
    @FXML private Label lblTotalStock;
    @FXML private Label lblLowStockCount;
    @FXML private Label lblLowStockTitle;
    @FXML private Label lblRecentSalesEntries;
    @FXML private Label lblCurrentDate;
    @FXML private Label lblRowCount;
    @FXML private Label lblPageInfo;

    @FXML private Button btnDateToday;
    @FXML private Button btnDateYesterday;
    @FXML private Button btnDate3;
    @FXML private Button btnDate4;
    @FXML private Button btnDate5;
    @FXML private Button btnDate6;
    @FXML private Button btnNextDate;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;

    @FXML private TableView<Product> inventoryTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, Integer> colOpeningStock;
    @FXML private TableColumn<Product, Integer> colPurchases;
    @FXML private TableColumn<Product, Integer> colSales;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, Integer> colSize;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, BigDecimal> colPurchasePrice;
    @FXML private TableColumn<Product, BigDecimal> colSellingPrice;
    @FXML private TableColumn<Product, BigDecimal> colProfit;
    @FXML private TableColumn<Product, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;

    @FXML private TableView<Product> salesItemsTable;
    @FXML private TableColumn<Product, String> colSaleProductName;
    @FXML private TableColumn<Product, String> colSaleCategory;
    @FXML private TableColumn<Product, Integer> colSaleSize;
    @FXML private TableColumn<Product, Integer> colSaleCurrentStock;
    @FXML private TableColumn<Product, BigDecimal> colSalePrice;
    @FXML private TextField posSearchField;
    @FXML private TextField txtSaleQuantity;
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> colCartProduct;
    @FXML private TableColumn<CartItem, Integer> colCartSize;
    @FXML private TableColumn<CartItem, Integer> colCartQty;
    @FXML private TableColumn<CartItem, BigDecimal> colCartPrice;
    @FXML private Label lblBillTotal;
    @FXML private Label lblEntryDate;

    @FXML private TextField txtProductName;
    @FXML private TextField txtSize;
    @FXML private TextField txtSellingPrice;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtPurchasePrice;
    @FXML private TextField txtCategory;
    @FXML private TextField txtManufacturer;
    @FXML private TextField txtLowStockThreshold;
    @FXML private TextField txtProfitMarginDisplay;
    @FXML private ComboBox<String> comboProductName;

    // --- Batch Purchase Fields ---
    @FXML private TableView<Product> purchaseItemsTable;
    @FXML private TableColumn<Product, String> colPurchaseItemName;
    @FXML private TableColumn<Product, Integer> colPurchaseItemSize;
    @FXML private TableColumn<Product, Integer> colPurchaseItemStock;
    @FXML private TableColumn<Product, Integer> colPurchaseItemPurchases;
    @FXML private TableColumn<Product, BigDecimal> colPurchaseItemPrice;
    @FXML private TableColumn<Product, BigDecimal> colPurchaseItemSellPrice;
    
    @FXML private TableView<PurchaseCartItem> purchaseCartTable;
    @FXML private TableColumn<PurchaseCartItem, String> colPCartProduct;
    @FXML private TableColumn<PurchaseCartItem, Integer> colPCartSize;
    @FXML private TableColumn<PurchaseCartItem, Integer> colPCartQty;
    @FXML private TableColumn<PurchaseCartItem, BigDecimal> colPCartBuyPrice;
    @FXML private TableColumn<PurchaseCartItem, BigDecimal> colPCartSellPrice;
    @FXML private TableColumn<PurchaseCartItem, BigDecimal> colPCartTotal;

    @FXML private TextField txtPCartQty;
    @FXML private TextField txtPCartBuyPrice;
    @FXML private TextField txtPCartSellPrice;
    @FXML private Label lblPurchaseBatchTotal;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboManufacturer;
    @FXML private Label lblCurrentStock;
    @FXML private Label lblLastPrice;
    @FXML private Label lblEditItemId;
    @FXML private Label lblEditCurrentStock;
    @FXML private Label lblHistoryPurchases;
    @FXML private Label lblHistorySales;

    @FXML private Button btnDailyReport;
    @FXML private Button btnMonthlyReport;
    @FXML private Button btnAnnualReport;
    @FXML private Button btnProductAnalysis;
    @FXML private DatePicker analyticsDatePicker;
    @FXML private Label lblAnalyticsTotalSalesAmt;
    @FXML private Label lblAnalyticsTxCount;
    @FXML private Label lblAnalyticsTotalQty;
    @FXML private Label lblAnalyticsProfit;
    @FXML private Label lblAnalyticsMargin;
    @FXML private HBox analyticsDataState;
    @FXML private VBox analyticsEmptyState;
    @FXML private VBox analyticsDateCard;
    @FXML private VBox analyticsSummarySection;
    @FXML private StackPane productAnalysisContainer;
    @FXML private Label lblDatePicker;
    @FXML private HBox dateSelectorContainer;
    private ComboBox<Month> comboMonth;
    private ComboBox<Integer> comboYear;

    @FXML private TextField txHistorySearch;
    @FXML private DatePicker dpHistoryDate;
    @FXML private TableView<SaleRecord> historyTable;
    @FXML private TableColumn<SaleRecord, Integer> colHistorySaleId;
    @FXML private TableColumn<SaleRecord, Integer> colHistoryQty;
    @FXML private TableColumn<SaleRecord, String> colHistoryProduct;
    @FXML private TableColumn<SaleRecord, Integer> colHistorySize;
    @FXML private TableColumn<SaleRecord, LocalDate> colHistoryDate;
    @FXML private TableColumn<SaleRecord, BigDecimal> colHistoryPrice;
    @FXML private TableColumn<SaleRecord, BigDecimal> colHistoryTotal;
    @FXML private TableColumn<SaleRecord, BigDecimal> colHistoryProfit;
    @FXML private Label lblTotalTransactions;
    @FXML private Label lblTotalSalesAmount;
    @FXML private Label lblTotalUnitsSold;
    @FXML private Label lblHistoryTotalProfit;

    private final ObservableList<Product> dashboardMasterData = FXCollections.observableArrayList();
    private final ObservableList<Product> inventoryPageData = FXCollections.observableArrayList();
    private final ObservableList<Product> posData = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cartData = FXCollections.observableArrayList();
    private final ObservableList<PurchaseCartItem> purchaseCartData = FXCollections.observableArrayList();
    private final ObservableList<SaleRecord> historyData = FXCollections.observableArrayList();

    private Product editingProduct;
    private LocalDate selectedDate = LocalDate.now();
    private int currentPage = 1;
    private boolean lowStockOnly;
    private ReportType currentReportType = ReportType.DAILY;

    private enum ReportType {
        DAILY, MONTHLY, ANNUAL, PRODUCT_ANALYSIS
    }

    private ProductAnalysisController activeProductAnalysisController;

    @FXML
    public void initialize() {
        setupInventoryTable();
        setupSalesTable();
        setupCartTable();
        setupHistoryTable();
        setupPurchaseTables();
        setupDateButtons();
        setupAutoCompletion();
        updateEntryDateLabel();

        if (analyticsDatePicker != null && analyticsDatePicker.getValue() == null) {
            analyticsDatePicker.setValue(selectedDate);
        }

        if (inventoryTable != null) {
            inventoryTable.setItems(inventoryPageData);
        }
        if (salesItemsTable != null) {
            salesItemsTable.setItems(posData);
            refreshPosTable();
        }
        if (historyTable != null) {
            historyTable.setItems(historyData);
            handleHistoryFilter();
        }

        if (purchaseItemsTable != null) {
            purchaseItemsTable.setItems(posData);
            refreshPosTable();
        }
        if (purchaseCartTable != null) {
            purchaseCartTable.setItems(purchaseCartData);
        }

        if (btnNavInventory != null) {
            updateNavStyles(btnNavInventory);
        }

        refreshTable();
        loadAnalyticsData();
    }

    private void setupInventoryTable() {
        if (inventoryTable == null) {
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colSize != null) {
            colSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        }
        colOpeningStock.setCellValueFactory(new PropertyValueFactory<>("openingStock"));
        colPurchases.setCellValueFactory(new PropertyValueFactory<>("totalPurchases"));
        colSales.setCellValueFactory(new PropertyValueFactory<>("totalSales"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        colPurchasePrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colSellingPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        if (colProfit != null) {
            colProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
        }
        colStatus.setCellValueFactory(new PropertyValueFactory<>("stockStatus"));
        
        inventoryTable.setRowFactory(tv -> new TableRow<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (safeInt(item.getCurrentStock()) <= safeInt(item.getLowStockThreshold())) {
                    setStyle("-fx-background-color: #fef2f2;"); // Very light red highlight
                } else {
                    setStyle("");
                }
            }
        });

        inventoryTable.setPlaceholder(new Label("No information exists for the selected date."));

        if (colActions != null) {
            colActions.setCellFactory(param -> new TableCell<>() {
                private final Button editButton = new Button("Edit");
                private final Button batchButton = new Button("Batches");
                private final HBox container = new HBox(8, editButton, batchButton);

                {
                    editButton.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; "
                            + "-fx-cursor: hand; -fx-padding: 4 8;");
                    editButton.setOnAction(event -> handleEditProduct(getTableView().getItems().get(getIndex())));

                    batchButton.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; "
                            + "-fx-cursor: hand; -fx-padding: 4 8; -fx-font-weight: bold;");
                    batchButton.setOnAction(event -> openBatchDetailsPopup(getTableView().getItems().get(getIndex())));
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : container);
                }
            });
        }
    }

    private void setupSalesTable() {
        if (salesItemsTable == null) {
            return;
        }

        colSaleProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colSaleCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colSaleSize != null) {
            colSaleSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        }
        colSaleCurrentStock.setCellValueFactory(new PropertyValueFactory<>("currentStock"));
        if (colSalePrice != null) {
            colSalePrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        }
    }

    private void setupPurchaseTables() {
        if (purchaseItemsTable == null) return;

        colPurchaseItemName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        if (colPurchaseItemSize != null) {
            colPurchaseItemSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        }
        
        // Real-time stock calculation (Current + Pending in Batch)
        colPurchaseItemStock.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            int current = safeInt(p.getCurrentStock());
            int pending = purchaseCartData.stream()
                .filter(item -> item.getProduct().getProductId().equals(p.getProductId()))
                .mapToInt(PurchaseCartItem::getQuantity)
                .sum();
            return new ReadOnlyObjectWrapper<>(current + pending);
        });

        if (colPurchaseItemPurchases != null) {
            colPurchaseItemPurchases.setCellValueFactory(cellData -> {
                Product p = cellData.getValue();
                int pending = purchaseCartData.stream()
                    .filter(item -> item.getProduct().getProductId().equals(p.getProductId()))
                    .mapToInt(PurchaseCartItem::getQuantity)
                    .sum();
                return new ReadOnlyObjectWrapper<>(pending);
            });
        }

        if (colPurchaseItemPrice != null) {
            colPurchaseItemPrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        }
        if (colPurchaseItemSellPrice != null) {
            colPurchaseItemSellPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        }

        if (purchaseCartTable != null) {
            colPCartProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
            if (colPCartSize != null) {
                colPCartSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
            }
            colPCartQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            colPCartBuyPrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
            colPCartSellPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
            colPCartTotal.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        }
    }

    private void setupCartTable() {
        if (cartTable == null) {
            return;
        }

        colCartProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        if (colCartSize != null) {
            colCartSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        }
        colCartQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colCartPrice.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        cartTable.setItems(cartData);
        updateBillTotal();
    }

    private void setupHistoryTable() {
        if (historyTable == null) {
            return;
        }

        colHistorySaleId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colHistoryDate.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        colHistoryProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        if (colHistorySize != null) {
            colHistorySize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        }
        colHistoryQty.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        colHistoryPrice.setCellValueFactory(new PropertyValueFactory<>("salePriceAtTime"));
        colHistoryTotal.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        colHistoryProfit.setCellValueFactory(new PropertyValueFactory<>("profit"));
    }

    private void setupDateButtons() {
        configureQuickDateButton(btnDateToday, 0);
        configureQuickDateButton(btnDateYesterday, 1);
        configureQuickDateButton(btnDate3, 2);
        configureQuickDateButton(btnDate4, 3);
        configureQuickDateButton(btnDate5, 4);
        configureQuickDateButton(btnDate6, 5);
    }

    private void configureQuickDateButton(Button button, int daysAgo) {
        if (button == null) {
            return;
        }
        button.setOnAction(event -> {
            Object userData = button.getUserData();
            if (userData instanceof LocalDate date) {
                setSelectedDate(date);
            }
        });
    }

    private void setupAutoCompletion() {
        List<String> productNames = inventoryService.getAllProductNames();
        if (txtProductName != null) {
            TextFields.bindAutoCompletion(txtProductName, productNames);
        }
        if (comboProductName != null) {
            comboProductName.setEditable(true);
            comboProductName.setItems(FXCollections.observableArrayList(productNames));
            TextFields.bindAutoCompletion(comboProductName.getEditor(), productNames);
            comboProductName.setOnAction(event -> updateSelectedProductInfo());
        }
        
        if (posSearchField != null) {
            AutoCompletionBinding<String> binding = TextFields.bindAutoCompletion(posSearchField, productNames);
            binding.setOnAutoCompleted(event -> {
                String result = event.getCompletion();
                posSearchField.setText(result);
                selectProductInTable(result);
            });
        }
    }

    private void selectProductInTable(String productName) {
        if (purchaseItemsTable == null) return;
        for (Product p : posData) {
            if (safeString(p.getProductName()).equalsIgnoreCase(productName)) {
                purchaseItemsTable.getSelectionModel().select(p);
                purchaseItemsTable.scrollTo(p);
                break;
            }
        }
    }

    private void updateSelectedProductInfo() {
        if (comboProductName == null) {
            return;
        }

        String selectedName = comboProductName.getSelectionModel().getSelectedItem();
        if ((selectedName == null || selectedName.isBlank()) && comboProductName.getEditor() != null) {
            selectedName = comboProductName.getEditor().getText();
        }

        if (selectedName == null || selectedName.isBlank()) {
            return;
        }

        Product product = inventoryService.findActiveProductByName(selectedName.trim());
        if (product == null) {
            if (lblCurrentStock != null) {
                lblCurrentStock.setText("Current Stock: -");
            }
            if (lblLastPrice != null) {
                lblLastPrice.setText("Last Purchase Price: -");
            }
            return;
        }

        if (lblCurrentStock != null) {
            lblCurrentStock.setText("Current Stock: " + safeInt(product.getCurrentStock()));
        }
        if (lblLastPrice != null) {
            lblLastPrice.setText("Last Purchase Price: " + formatCurrency(product.getPurchasePrice()));
        }
    }

    @FXML
    public void refreshTable() {
        if (inventoryTable != null) {
            dashboardMasterData.setAll(inventoryService.getProductsForDate(selectedDate));
            populateCategoryFilter();
            applyInventoryFilters();
            updateDashboardCards();
            updateDateBar();
        }

        updateEntryDateLabel();
    }

    private void populateCategoryFilter() {
        if (categoryFilter == null) {
            return;
        }

        String currentValue = categoryFilter.getValue();
        categoryFilter.getItems().setAll("All Categories");
        categoryFilter.getItems().addAll(inventoryService.getDistinctCategories());
        categoryFilter.setValue(currentValue != null ? currentValue : "All Categories");
    }

    private void applyInventoryFilters() {
        if (inventoryTable == null) {
            return;
        }

        String query = searchField != null && searchField.getText() != null
                ? searchField.getText().trim().toLowerCase(Locale.ENGLISH)
                : "";
        String category = categoryFilter != null ? categoryFilter.getValue() : "All Categories";

        List<Product> filtered = dashboardMasterData.stream()
                .filter(product -> query.isBlank()
                        || safeString(product.getProductName()).toLowerCase(Locale.ENGLISH).contains(query))
                .filter(product -> category == null
                        || category.equals("All Categories")
                        || category.equals(product.getCategory()))
                .filter(product -> !lowStockOnly
                        || safeInt(product.getCurrentStock()) <= safeInt(product.getLowStockThreshold()))
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int fromIndex = Math.min((currentPage - 1) * PAGE_SIZE, filtered.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filtered.size());
        inventoryPageData.setAll(filtered.subList(fromIndex, toIndex));

        if (lblRowCount != null) {
            lblRowCount.setText(filtered.size() + " product(s)");
        }
        if (lblPageInfo != null) {
            lblPageInfo.setText("Page " + currentPage + " of " + totalPages);
        }
        if (btnPrevPage != null) {
            btnPrevPage.setDisable(currentPage <= 1);
        }
        if (btnNextPage != null) {
            btnNextPage.setDisable(currentPage >= totalPages);
        }
    }

    private void updateDashboardCards() {
        if (lblTotalProducts != null) {
            lblTotalProducts.setText(String.valueOf(dashboardMasterData.size()));
        }
        if (lblTotalStock != null) {
            lblTotalStock.setText(String.valueOf(
                    dashboardMasterData.stream().mapToInt(product -> safeInt(product.getCurrentStock())).sum()));
        }
        if (lblLowStockCount != null) {
            long lowStockCount = dashboardMasterData.stream()
                    .filter(product -> safeInt(product.getCurrentStock()) <= safeInt(product.getLowStockThreshold()))
                    .count();
            lblLowStockCount.setText(String.valueOf(lowStockCount));
            
            if (cardLowStock != null) {
                if (lowStockCount > 0) {
                    cardLowStock.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 12; -fx-padding: 18 20; -fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.3), 12, 0, 0, 5); -fx-cursor: hand;");
                    lblLowStockCount.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: white;");
                    if (lblLowStockTitle != null) {
                        lblLowStockTitle.setStyle("-fx-text-fill: #fee2e2; -fx-font-size: 12;");
                    }
                } else {
                    cardLowStock.setStyle(INACTIVE_CARD);
                    lblLowStockCount.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
                    if (lblLowStockTitle != null) {
                        lblLowStockTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
                    }
                }
            }
        }
        if (lblRecentSalesEntries != null) {
            BigDecimal totalProfit = dashboardMasterData.stream()
                    .map(Product::getProfit)
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            lblRecentSalesEntries.setText(formatCurrency(totalProfit));
        }
    }

    private void updateDateBar() {
        if (lblCurrentDate != null) {
            lblCurrentDate.setText("Viewing inventory for " + selectedDate.format(HEADER_DATE_FORMAT));
        }

        LocalDate today = LocalDate.now();
        assignDateButton(btnDateToday, today);
        assignDateButton(btnDateYesterday, today.minusDays(1));
        assignDateButton(btnDate3, today.minusDays(2));
        assignDateButton(btnDate4, today.minusDays(3));
        assignDateButton(btnDate5, today.minusDays(4));
        assignDateButton(btnDate6, today.minusDays(5));

        // Highlight the selected date
        styleDateChip(btnDateToday, today.equals(selectedDate));
        styleDateChip(btnDateYesterday, today.minusDays(1).equals(selectedDate));
        styleDateChip(btnDate3, today.minusDays(2).equals(selectedDate));
        styleDateChip(btnDate4, today.minusDays(3).equals(selectedDate));
        styleDateChip(btnDate5, today.minusDays(4).equals(selectedDate));
        styleDateChip(btnDate6, today.minusDays(5).equals(selectedDate));

        if (btnNextDate != null) {
            btnNextDate.setDisable(!selectedDate.isBefore(today));
        }
    }

    private void styleDateChip(Button button, boolean active) {
        if (button == null) return;
        
        if (active) {
            button.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 5 14; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            button.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand;");
        }
    }

    private void assignDateButton(Button button, LocalDate date) {
        if (button == null) {
            return;
        }
        button.setUserData(date);
        if (button != btnDateToday && button != btnDateYesterday) {
            button.setText(date.format(CHIP_DATE_FORMAT));
        }
    }

    private void setSelectedDate(LocalDate date) {
        if (date == null) {
            return;
        }
        if (date.isAfter(LocalDate.now())) {
            date = LocalDate.now();
        }
        selectedDate = date;
        currentPage = 1;
        refreshTable();
        loadAnalyticsData();
    }

    @FXML
    private void handleDateToday() {
        setSelectedDate(LocalDate.now());
    }

    @FXML
    private void handleDateYesterday() {
        setSelectedDate(LocalDate.now().minusDays(1));
    }

    @FXML
    private void handleDatePrev() {
        setSelectedDate(selectedDate.minusDays(1));
    }

    @FXML
    private void handleDateNext() {
        if (selectedDate.isBefore(LocalDate.now())) {
            setSelectedDate(selectedDate.plusDays(1));
        }
    }

    @FXML
    private void handlePickDate() {
        DatePicker picker = new DatePicker(selectedDate);
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Pick Date");
        dialog.setHeaderText("Select the date you want to view.");
        dialog.getDialogPane().setContent(picker);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? picker.getValue() : null);
        dialog.showAndWait().ifPresent(this::setSelectedDate);
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            applyInventoryFilters();
        }
    }

    @FXML
    private void handleNextPage() {
        currentPage++;
        applyInventoryFilters();
    }

    @FXML
    private void handleSearch() {
        currentPage = 1;
        lowStockOnly = false;
        updateCardStyles(cardTotalProducts);
        applyInventoryFilters();
    }

    @FXML
    private void handleShowDashboard() {
        if (contentArea != null && dashboardView != null) {
            contentArea.getChildren().setAll(dashboardView);
        }
        updateNavStyles(btnNavInventory);
        lowStockOnly = false;
        updateCardStyles(cardTotalProducts);
        refreshTable();
    }

    @FXML
    private void handleOpenAnalytics() {
        loadView("/view/AnalyticsView.fxml");
        updateNavStyles(btnNavAnalytics);
        if (currentReportType == ReportType.PRODUCT_ANALYSIS) {
            showProductAnalysisMode();
        } else {
            loadAnalyticsData();
        }
    }

    @FXML
    private void handleOpenTransactionHistory() {
        loadView("/view/TransactionHistoryView.fxml");
        updateNavStyles(btnNavHistory);
        handleHistoryFilter();
    }

    @FXML
    private void handleOpenPurchaseHistory() {
        loadView("/view/PurchaseHistory.fxml");
        updateNavStyles(btnNavPurchaseHistory);
    }

    @FXML
    public void handleProductAnalysis() {
        currentReportType = ReportType.PRODUCT_ANALYSIS;
        showProductAnalysisMode();
    }

    @FXML
    private void handleDailyReport() {
        currentReportType = ReportType.DAILY;
        loadAnalyticsData();
    }

    @FXML
    private void handleMonthlyReport() {
        currentReportType = ReportType.MONTHLY;
        loadAnalyticsData();
    }

    @FXML
    private void handleAnnualReport() {
        currentReportType = ReportType.ANNUAL;
        loadAnalyticsData();
    }

    @FXML
    private void handleTotalProductsClick() {
        lowStockOnly = false;
        currentPage = 1;
        updateCardStyles(cardTotalProducts);
        applyInventoryFilters();
    }

    @FXML
    private void handleLowStockAlertClick() {
        lowStockOnly = true;
        currentPage = 1;
        updateCardStyles(cardLowStock);
        applyInventoryFilters();
    }

    @FXML
    private void handleAddItem() {
        openModal("/view/AddItemModal.fxml", "Add Item");
    }

    @FXML
    private void handleRecordPurchase() {
        openModal("/view/RecordPurchaseModal.fxml", "Record Purchase");
    }

    @FXML
    private void handleBatchSalesEntry() {
        openModal("/view/BatchSalesView.fxml", "Batch Sales Entry");
    }

    @FXML
    private void handleExportCSV() {
        File targetDirectory = new File(System.getProperty("user.home"), "Desktop");

        try {
            Path csvFile = securityVaultService.exportInventoryCsv(selectedDate, targetDirectory.toPath(), dashboardMasterData);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Complete");
            alert.setHeaderText(null);
            alert.setContentText("Saved " + csvFile.getFileName() + " to your Desktop as read-only.");
            alert.showAndWait();
        } catch (IOException exception) {
            showErrorAlert("Export Error", exception.getMessage());
        }
    }

    private void openModal(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.updateEntryDateLabel();
            controller.setupAutoCompletion();
            controller.setupProductDropdown();
            if (path.contains("BatchSalesView")) {
                controller.refreshPosTable();
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.initModality(Modality.APPLICATION_MODAL);
            if (contentArea != null && contentArea.getScene() != null) {
                stage.initOwner(contentArea.getScene().getWindow());
            }
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshTable();
        } catch (IOException exception) {
            showErrorAlert("Open Window Error", exception.getMessage());
        }
    }

    /**
     * Opens the Batch Details popup for the given product.
     * Can be called from any row action in the inventory table.
     *
     * @param product the product whose batch history should be shown
     */
    public void openBatchDetailsPopup(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/BatchDetailsPopup.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            BatchDetailsController controller = loader.getController();
            controller.initData(product);

            Stage stage = new Stage();
            stage.setTitle("Batch History – " + product.getProductName());
            stage.initModality(Modality.APPLICATION_MODAL);
            if (contentArea != null && contentArea.getScene() != null) {
                stage.initOwner(contentArea.getScene().getWindow());
            }
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            showErrorAlert("Batch Details Error", e.getMessage());
        }
    }

    private void loadView(String path) {
        try {
            java.net.URL resource = MainController.class.getResource(path);
            if (resource == null) {
                showErrorAlert("Navigation Error", "Resource not found: " + path);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root;
            if ("/view/ProductAnalysis.fxml".equals(path)) {
                root = loader.load();
                ProductAnalysisController controller = loader.getController();
                controller.setReportService(springContext.getBean(com.winestore.inventory_system.service.ReportService.class));
            } else {
                loader.setControllerFactory(springContext::getBean);
                root = loader.load();
            }
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (Exception exception) {
            showErrorAlert("Navigation Error", exception.getMessage() != null ? exception.getMessage() : exception.toString());
        }
    }

    public void setupProductDropdown() {
        if (comboProductName == null) {
            return;
        }
        comboProductName.setItems(FXCollections.observableArrayList(inventoryService.getAllProductNames()));
        comboProductName.setOnAction(event -> updateSelectedProductInfo());
        updateSelectedProductInfo();
    }

    private void updateEntryDateLabel() {
        if (lblEntryDate != null) {
            lblEntryDate.setText("Entry Date: " + selectedDate.format(HEADER_DATE_FORMAT));
        }
    }

    @FXML
    private void handleSaveNewItem() {
        try {
            String name = requiredText(txtProductName, "Product name");
            String category = requiredText(txtCategory, "Category");
            int quantity = requiredInt(txtQuantity, "Opening stock");
            int size = txtSize == null || txtSize.getText().isBlank() ? 0 : requiredInt(txtSize, "Size");

            if (inventoryService.isDuplicate(name, size)) {
                showErrorAlert("Duplicate Product", "A product with the same name and size already exists.");
                return;
            }

            Product product = new Product();
            product.setProductName(name);
            product.setCategory(category);
            product.setManufacturer(optionalText(txtManufacturer));
            product.setSizeMl(size);
            product.setOpeningStock(quantity);
            product.setCurrentStock(quantity);
            product.setTotalPurchases(0);
            product.setTotalSales(0);
            product.setPurchasePrice(requiredDecimal(txtPurchasePrice, "Purchase price"));
            product.setSellingPrice(requiredDecimal(txtSellingPrice, "Selling price"));
            product.setLowStockThreshold(
                    txtLowStockThreshold == null || txtLowStockThreshold.getText().isBlank()
                            ? 10
                            : requiredInt(txtLowStockThreshold, "Low stock threshold"));
            product.setIsActive(true);

            inventoryService.saveProduct(product);
            handleCancelModal();
            refreshTable();
        } catch (IllegalArgumentException exception) {
            showErrorAlert("Input Error", exception.getMessage());
        }
    }

    @FXML
    private void handleAddToPurchaseBatch() {
        Product selected = purchaseItemsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorAlert("No Selection", "Please select a product from the list first.");
            return;
        }

        String qtyText = txtPCartQty.getText();
        String buyText = txtPCartBuyPrice.getText();
        String sellText = txtPCartSellPrice.getText();

        if (qtyText.isEmpty() || buyText.isEmpty() || sellText.isEmpty()) {
            showErrorAlert("Missing Information", "Please enter quantity, buy price, and sell price.");
            return;
        }

        try {
            int qty = Integer.parseInt(qtyText);
            BigDecimal buyPrice = new BigDecimal(buyText);
            BigDecimal sellPrice = new BigDecimal(sellText);

            if (qty <= 0) throw new NumberFormatException();

            purchaseCartData.add(new PurchaseCartItem(selected, qty, buyPrice, sellPrice));
            
            txtPCartQty.clear();
            txtPCartBuyPrice.clear();
            txtPCartSellPrice.clear();
            updatePurchaseBatchTotal();
            
            // Refresh the stock preview in the products table
            if (purchaseItemsTable != null) {
                purchaseItemsTable.refresh();
            }

        } catch (NumberFormatException e) {
            showErrorAlert("Invalid Input", "Please enter valid numeric values for quantity and prices.");
        }
    }

    private void updatePurchaseBatchTotal() {
        BigDecimal total = purchaseCartData.stream()
                .map(PurchaseCartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lblPurchaseBatchTotal != null) {
            lblPurchaseBatchTotal.setText("Batch Total: " + formatCurrency(total));
        }
    }

    @FXML
    private void handleConfirmBatchPurchase() {
        if (purchaseCartData.isEmpty()) {
            showErrorAlert("Batch Empty", "Add at least one product before saving the purchase batch.");
            return;
        }

        try {
            for (PurchaseCartItem item : purchaseCartData) {
                inventoryService.recordPurchase(
                    item.getProduct().getProductId(),
                    item.getQuantity(),
                    item.getPurchasePrice(),
                    item.getSellingPrice(),
                    selectedDate
                );
            }

            showInfoAlert("Success", "Batch purchase recorded successfully.");
            purchaseCartData.clear();
            updatePurchaseBatchTotal();
            handleCancelModal();
            refreshTable();

        } catch (Exception e) {
            showErrorAlert("Save Error", "Failed to record batch purchase: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveFromPurchaseBatch() {
        PurchaseCartItem selected = purchaseCartTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            purchaseCartData.remove(selected);
        } else {
            purchaseCartData.clear();
        }
        updatePurchaseBatchTotal();
        
        // Refresh the stock preview
        if (purchaseItemsTable != null) {
            purchaseItemsTable.refresh();
        }
    }

    @FXML
    private void handleCancelModal() {
        Stage stage = null;
        if (txtProductName != null && txtProductName.getScene() != null) {
            stage = (Stage) txtProductName.getScene().getWindow();
        } else if (comboProductName != null && comboProductName.getScene() != null) {
            stage = (Stage) comboProductName.getScene().getWindow();
        } else if (txtProfitMarginDisplay != null && txtProfitMarginDisplay.getScene() != null) {
            stage = (Stage) txtProfitMarginDisplay.getScene().getWindow();
        } else if (purchaseItemsTable != null && purchaseItemsTable.getScene() != null) {
            stage = (Stage) purchaseItemsTable.getScene().getWindow();
        }

        if (stage != null) {
            stage.close();
        }
    }

    @FXML
    private void handleAnalyticsDateChange() {
        if (currentReportType == ReportType.PRODUCT_ANALYSIS) {
            return;
        }
        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        if (dateSelectorContainer == null) {
            return;
        }

        if (currentReportType != ReportType.PRODUCT_ANALYSIS) {
            showAnalyticsSummaryMode();
        }

        updateAnalyticsSelectors();
        
        LocalDate anchorDate = LocalDate.now();
        if (currentReportType == ReportType.DAILY) {
            if (analyticsDatePicker != null && analyticsDatePicker.getValue() != null) {
                anchorDate = analyticsDatePicker.getValue();
            }
        } else {
            int year = (comboYear != null && comboYear.getValue() != null) ? comboYear.getValue() : LocalDate.now().getYear();
            if (currentReportType == ReportType.MONTHLY) {
                Month month = (comboMonth != null && comboMonth.getValue() != null) ? comboMonth.getValue() : LocalDate.now().getMonth();
                anchorDate = LocalDate.of(year, month, 1);
            } else {
                anchorDate = LocalDate.of(year, 1, 1);
            }
        }

        LocalDate startDate = anchorDate;
        LocalDate endDate = anchorDate;

        if (currentReportType == ReportType.MONTHLY) {
            startDate = anchorDate.withDayOfMonth(1);
            endDate = anchorDate.withDayOfMonth(anchorDate.lengthOfMonth());
        } else if (currentReportType == ReportType.ANNUAL || currentReportType == ReportType.PRODUCT_ANALYSIS) {
            startDate = anchorDate.withDayOfYear(1);
            endDate = anchorDate.withDayOfYear(anchorDate.lengthOfYear());
        }

        if (activeProductAnalysisController != null) {
            activeProductAnalysisController.setDateRange(startDate, endDate);
        }

        List<SaleRecord> sales = inventoryService.getSalesBetween(startDate, endDate);
        updateAnalyticsButtons();

        if (currentReportType == ReportType.PRODUCT_ANALYSIS) {
            return;
        }

        if (analyticsDataState != null) {
            analyticsDataState.setVisible(true);
            analyticsDataState.setManaged(true);
        }

        if (sales.isEmpty()) {
            setAnalyticsValues(BigDecimal.ZERO, 0, 0, BigDecimal.ZERO);
            if (analyticsEmptyState != null) {
                analyticsEmptyState.setVisible(true);
                analyticsEmptyState.setManaged(true);
            }
            return;
        }

        if (analyticsEmptyState != null) {
            analyticsEmptyState.setVisible(false);
            analyticsEmptyState.setManaged(false);
        }

        BigDecimal totalSales = sales.stream()
                .map(SaleRecord::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalQuantity = sales.stream().mapToInt(sale -> safeInt(sale.getQuantitySold())).sum();
        BigDecimal totalProfit = inventoryService.calculateProfitForSales(sales);
        setAnalyticsValues(totalSales, sales.size(), totalQuantity, totalProfit);
    }

    private void showAnalyticsSummaryMode() {
        setNodeVisibility(analyticsDateCard, true);
        setNodeVisibility(analyticsSummarySection, true);
        setNodeVisibility(productAnalysisContainer, false);
        updateAnalyticsButtons();
    }

    private void showProductAnalysisMode() {
        if (productAnalysisContainer == null) {
            return;
        }

        try {
            ensureProductAnalysisViewLoaded();
            setNodeVisibility(analyticsDateCard, true);
            setNodeVisibility(analyticsSummarySection, false);
            setNodeVisibility(productAnalysisContainer, true);
            updateAnalyticsButtons();
            updateAnalyticsSelectors();
            loadAnalyticsData();
        } catch (IOException exception) {
            showErrorAlert("Navigation Error", exception.getMessage());
        }
    }

    private void ensureProductAnalysisViewLoaded() throws IOException {
        if (productAnalysisContainer.getChildren().isEmpty()) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductAnalysis.fxml"));
            Parent analysisView = loader.load();
            activeProductAnalysisController = loader.getController();
            activeProductAnalysisController.setReportService(springContext.getBean(com.winestore.inventory_system.service.ReportService.class));
            productAnalysisContainer.getChildren().setAll(analysisView);
        }
    }

    private void setNodeVisibility(javafx.scene.Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateAnalyticsSelectors() {
        if (dateSelectorContainer == null) return;
        
        dateSelectorContainer.getChildren().clear();
        
        if (currentReportType == ReportType.DAILY) {
            if (lblDatePicker != null) lblDatePicker.setText("Select Date");
            if (analyticsDatePicker == null) {
                analyticsDatePicker = new DatePicker(selectedDate);
                analyticsDatePicker.setOnAction(e -> loadAnalyticsData());
                analyticsDatePicker.setPrefHeight(38);
                analyticsDatePicker.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
            }
            dateSelectorContainer.getChildren().add(analyticsDatePicker);
        } else {
            if (comboYear == null) {
                comboYear = new ComboBox<>();
                int currentYear = LocalDate.now().getYear();
                for (int y = currentYear; y >= currentYear - 5; y--) {
                    comboYear.getItems().add(y);
                }
                comboYear.setValue(currentYear);
                comboYear.setOnAction(e -> loadAnalyticsData());
                comboYear.setPrefHeight(38);
                comboYear.setPrefWidth(120);
                comboYear.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
            }
            
            if (currentReportType == ReportType.MONTHLY) {
                if (lblDatePicker != null) lblDatePicker.setText("Select Month");
                if (comboMonth == null) {
                    comboMonth = new ComboBox<>();
                    comboMonth.getItems().addAll(Month.values());
                    comboMonth.setValue(LocalDate.now().getMonth());
                    comboMonth.setOnAction(e -> loadAnalyticsData());
                    comboMonth.setPrefHeight(38);
                    comboMonth.setPrefWidth(150);
                    comboMonth.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
                }
                dateSelectorContainer.getChildren().addAll(comboMonth, comboYear);
            } else {
                if (lblDatePicker != null) lblDatePicker.setText("Select Year");
                dateSelectorContainer.getChildren().add(comboYear);
            }
        }
    }

    private void updateAnalyticsButtons() {
        styleAnalyticsButton(btnDailyReport, currentReportType == ReportType.DAILY);
        styleAnalyticsButton(btnMonthlyReport, currentReportType == ReportType.MONTHLY);
        styleAnalyticsButton(btnAnnualReport, currentReportType == ReportType.ANNUAL);
        styleAnalyticsButton(btnProductAnalysis, currentReportType == ReportType.PRODUCT_ANALYSIS);
    }

    private void styleAnalyticsButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            button.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void setAnalyticsValues(BigDecimal totalSales, int transactionCount, int totalQuantity, BigDecimal totalProfit) {
        if (lblAnalyticsTotalSalesAmt != null) {
            lblAnalyticsTotalSalesAmt.setText(formatCurrency(totalSales));
        }
        if (lblAnalyticsTxCount != null) {
            lblAnalyticsTxCount.setText(transactionCount + " transaction(s)");
        }
        if (lblAnalyticsTotalQty != null) {
            lblAnalyticsTotalQty.setText(String.valueOf(totalQuantity));
        }
        if (lblAnalyticsProfit != null) {
            lblAnalyticsProfit.setText(formatCurrency(totalProfit));
        }
        if (lblAnalyticsMargin != null) {
            if (totalSales.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = totalProfit.multiply(BigDecimal.valueOf(100))
                        .divide(totalSales, 1, RoundingMode.HALF_UP);
                lblAnalyticsMargin.setText(margin + "% margin");
            } else {
                lblAnalyticsMargin.setText("0.0% margin");
            }
        }
    }

    private void updateNavStyles(Button activeButton) {
        String activeStyle =
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 5; "
                        + "-fx-padding: 8 18; -fx-font-weight: bold;";
        String inactiveStyle =
                "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-padding: 8 18; -fx-cursor: hand;";

        if (btnNavInventory != null) {
            btnNavInventory.setStyle(inactiveStyle);
        }
        if (btnNavAnalytics != null) {
            btnNavAnalytics.setStyle(inactiveStyle);
        }
        if (btnNavHistory != null) {
            btnNavHistory.setStyle(inactiveStyle);
        }
        if (btnNavPurchaseHistory != null) {
            btnNavPurchaseHistory.setStyle(inactiveStyle);
        }
        if (activeButton != null) {
            activeButton.setStyle(activeStyle);
        }
    }

    private void updateCardStyles(VBox activeCard) {
        if (cardTotalProducts != null) {
            cardTotalProducts.setStyle(INACTIVE_CARD);
        }
        if (cardTotalStock != null) {
            cardTotalStock.setStyle(INACTIVE_CARD);
        }
        if (cardLowStock != null) {
            cardLowStock.setStyle(INACTIVE_CARD);
        }
        if (cardRecentSales != null) {
            cardRecentSales.setStyle(INACTIVE_CARD);
        }
        if (activeCard != null) {
            activeCard.setStyle(ACTIVE_CARD);
        }
    }

    @FXML
    private void handleAddToBill() {
        Product selectedProduct = salesItemsTable != null ? salesItemsTable.getSelectionModel().getSelectedItem() : null;
        if (selectedProduct == null) {
            showErrorAlert("Selection Error", "Select a product before adding it to the sales batch.");
            return;
        }

        int quantity = 1;
        if (txtSaleQuantity != null && txtSaleQuantity.getText() != null && !txtSaleQuantity.getText().isBlank()) {
            try {
                quantity = Integer.parseInt(txtSaleQuantity.getText().trim());
            } catch (NumberFormatException exception) {
                showErrorAlert("Quantity Error", "Enter a valid numeric quantity.");
                return;
            }
        }

        if (quantity <= 0) {
            showErrorAlert("Quantity Error", "Quantity must be greater than zero.");
            return;
        }
        if (safeInt(selectedProduct.getCurrentStock()) < quantity) {
            showErrorAlert("Stock Error", "Not enough stock is available for this product.");
            return;
        }

        CartItem existingItem = cartData.stream()
                .filter(item -> item.getProduct().getProductId().equals(selectedProduct.getProductId()))
                .findFirst()
                .orElse(null);

        int newQuantity = quantity;
        if (existingItem != null) {
            newQuantity = existingItem.getQuantity() + quantity;
            if (safeInt(selectedProduct.getCurrentStock()) < newQuantity) {
                showErrorAlert("Stock Error", "The requested batch quantity exceeds the current stock.");
                return;
            }
            existingItem.setQuantity(newQuantity);
            cartTable.refresh();
        } else {
            cartData.add(new CartItem(selectedProduct, quantity));
        }

        if (txtSaleQuantity != null) {
            txtSaleQuantity.clear();
        }
        updateBillTotal();
    }

    private void updateBillTotal() {
        BigDecimal total = cartData.stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lblBillTotal != null) {
            lblBillTotal.setText("Batch Total: " + formatCurrency(total));
        }
    }

    @FXML
    private void handleConfirmBatchSale() {
        if (cartData.isEmpty()) {
            showErrorAlert("Batch Empty", "Add at least one product before saving the sales batch.");
            return;
        }

        try {
            List<SaleRecord> records = cartData.stream()
                    .map(item -> {
                        SaleRecord record = new SaleRecord();
                        record.setProductId(item.getProduct().getProductId());
                        record.setQuantitySold(item.getQuantity());
                        record.setSalePriceAtTime(item.getUnitPrice());
                        record.setSaleDate(selectedDate);
                        return record;
                    })
                    .collect(Collectors.toList());

            inventoryService.processBatchSale(records);
            cartData.clear();
            updateBillTotal();
            refreshPosTable();
            refreshTable();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sales Saved");
            alert.setHeaderText(null);
            alert.setContentText("The sales batch was recorded for " + selectedDate.format(HEADER_DATE_FORMAT) + ".");
            alert.showAndWait();
        } catch (RuntimeException exception) {
            showErrorAlert("Transaction Error", exception.getMessage());
        }
    }

    @FXML
    private void handleRemoveFromBill() {
        if (cartTable != null && cartTable.getSelectionModel().getSelectedItem() != null) {
            cartData.remove(cartTable.getSelectionModel().getSelectedItem());
        } else {
            cartData.clear();
        }
        updateBillTotal();
    }

    @FXML
    public void refreshPosTable() {
        posData.setAll(inventoryService.getAllProducts());
        handleSearchPos();
    }

    @FXML
    private void handleSearchPos() {
        if (salesItemsTable == null) {
            return;
        }

        String query = posSearchField != null && posSearchField.getText() != null
                ? posSearchField.getText().trim().toLowerCase(Locale.ENGLISH)
                : "";

        List<Product> filtered = inventoryService.getAllProducts().stream()
                .filter(product -> safeString(product.getProductName()).toLowerCase(Locale.ENGLISH).contains(query))
                .collect(Collectors.toList());
        posData.setAll(filtered);
    }

    @FXML
    private void handleHistoryFilter() {
        if (historyTable == null) {
            return;
        }

        String query = txHistorySearch != null && txHistorySearch.getText() != null
                ? txHistorySearch.getText().trim().toLowerCase(Locale.ENGLISH)
                : "";
        LocalDate date = dpHistoryDate != null ? dpHistoryDate.getValue() : null;

        List<SaleRecord> sales = inventoryService.getSalesBetween(
                date != null ? date : LocalDate.MIN,
                date != null ? date : LocalDate.MAX);

        List<Product> products = inventoryService.getAllProducts();
        List<SaleRecord> filtered = sales.stream()
                .peek(sale -> {
                    Product product = products.stream()
                            .filter(candidate -> candidate.getProductId().equals(sale.getProductId()))
                            .findFirst()
                            .orElse(null);
                    if (product != null) {
                        sale.setProductName(product.getProductName());
                        sale.setSizeMl(product.getSizeMl());
                        BigDecimal purchasePrice = product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO;
                        BigDecimal salePrice = sale.getSalePriceAtTime() != null ? sale.getSalePriceAtTime() : BigDecimal.ZERO;
                        BigDecimal qty = BigDecimal.valueOf(sale.getQuantitySold() != null ? sale.getQuantitySold() : 0);
                        sale.setProfit(salePrice.subtract(purchasePrice).multiply(qty));
                    }
                })
                .filter(sale -> query.isBlank()
                        || safeString(sale.getProductName()).toLowerCase(Locale.ENGLISH).contains(query))
                .collect(Collectors.toList());

        historyData.setAll(filtered);
        updateHistoryStats(filtered);
    }

    private void updateHistoryStats(List<SaleRecord> sales) {
        if (lblTotalTransactions != null) {
            lblTotalTransactions.setText(String.valueOf(sales.size()));
        }

        BigDecimal totalSales = sales.stream()
                .map(SaleRecord::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (lblTotalSalesAmount != null) {
            lblTotalSalesAmount.setText(formatCurrency(totalSales));
        }
        if (lblTotalUnitsSold != null) {
            int totalUnits = sales.stream().mapToInt(sale -> safeInt(sale.getQuantitySold())).sum();
            lblTotalUnitsSold.setText(totalUnits + " units sold");
        }
        if (lblHistoryTotalProfit != null) {
            lblHistoryTotalProfit.setText(formatCurrency(inventoryService.calculateProfitForSales(sales)));
        }
    }

    @FXML
    private void handleClearHistoryFilter() {
        if (txHistorySearch != null) {
            txHistorySearch.clear();
        }
        if (dpHistoryDate != null) {
            dpHistoryDate.setValue(null);
        }
        handleHistoryFilter();
    }

    @FXML
    private void handleOpenSettings() {
        if (inventoryService.isManagerPasswordConfigured()
                && !checkManagerPassword("Enter the current manager password to update settings.")) {
            return;
        }
        openSecuritySettings();
    }

    private void openSecuritySettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/SecuritySettings.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Manager Control Panel");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (contentArea != null && contentArea.getScene() != null) {
                stage.initOwner(contentArea.getScene().getWindow());
            }
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException exception) {
            showErrorAlert("Settings Error", exception.getMessage());
        }
    }

    private boolean checkManagerPassword() {
        return checkManagerPassword("Enter the manager password to edit this item.");
    }

    private boolean checkManagerPassword(String prompt) {
        if (!inventoryService.isManagerPasswordConfigured()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Manager Password Required");
            alert.setHeaderText(null);
            alert.setContentText("No manager password is configured yet. Create one in Settings first.");
            alert.showAndWait();
            return false;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manager Verification");
        dialog.setHeaderText(prompt);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Manager password");
        dialog.getDialogPane().setContent(new VBox(10, new Label("Password"), passwordField));
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) {
            return false;
        }

        if (!inventoryService.validateManagerPassword(passwordField.getText())) {
            showErrorAlert("Access Denied", "Incorrect manager password.");
            return false;
        }

        return true;
    }

    private void handleEditProduct(Product product) {
        if (!checkManagerPassword()) {
            return;
        }
        try {
            editingProduct = inventoryService.findProductById(product.getProductId());
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/EditItemModal.fxml"));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.editingProduct = editingProduct;
            controller.populateEditModal(editingProduct);

            Stage stage = new Stage();
            stage.setTitle("Edit Product");
            stage.initModality(Modality.APPLICATION_MODAL);
            if (contentArea != null && contentArea.getScene() != null) {
                stage.initOwner(contentArea.getScene().getWindow());
            }
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshTable();
        } catch (IOException exception) {
            showErrorAlert("Edit Error", exception.getMessage());
        }
    }

    private void populateEditModal(Product product) {
        if (lblEditItemId != null) {
            lblEditItemId.setText("Item ID: " + safeString(product.getProductId()));
        }
        if (lblEditCurrentStock != null) {
            lblEditCurrentStock.setText("Current Stock: " + safeInt(product.getCurrentStock()));
        }
        if (txtProductName != null) {
            txtProductName.setText(product.getProductName());
        }
        if (txtQuantity != null) {
            txtQuantity.setText(String.valueOf(safeInt(product.getOpeningStock())));
        }
        if (txtPurchasePrice != null && product.getPurchasePrice() != null) {
            txtPurchasePrice.setText(product.getPurchasePrice().toPlainString());
        }
        if (txtSellingPrice != null && product.getSellingPrice() != null) {
            txtSellingPrice.setText(product.getSellingPrice().toPlainString());
        }
        if (txtLowStockThreshold != null) {
            txtLowStockThreshold.setText(String.valueOf(safeInt(product.getLowStockThreshold())));
        }
        if (comboCategory != null) {
            comboCategory.setItems(FXCollections.observableArrayList(inventoryService.getDistinctCategories()));
            comboCategory.setValue(product.getCategory());
        }
        if (lblHistoryPurchases != null) {
            lblHistoryPurchases.setText("Purchases: " + inventoryService.getPurchaseCountForProduct(product.getProductId()));
        }
        if (lblHistorySales != null) {
            lblHistorySales.setText("Sales: " + inventoryService.getSaleCountForProduct(product.getProductId()));
        }
        updateProfitMarginDisplay();
    }

    @FXML
    private void handleProfitInputChange() {
        updateProfitMarginDisplay();
    }

    private void updateProfitMarginDisplay() {
        if (txtProfitMarginDisplay == null) {
            return;
        }

        try {
            BigDecimal purchasePrice = txtPurchasePrice == null || txtPurchasePrice.getText().isBlank()
                    ? BigDecimal.ZERO
                    : new BigDecimal(txtPurchasePrice.getText().trim());
            BigDecimal sellingPrice = txtSellingPrice == null || txtSellingPrice.getText().isBlank()
                    ? BigDecimal.ZERO
                    : new BigDecimal(txtSellingPrice.getText().trim());
            txtProfitMarginDisplay.setText("Margin per unit: " + formatCurrency(sellingPrice.subtract(purchasePrice)));
        } catch (NumberFormatException exception) {
            txtProfitMarginDisplay.setText("Margin per unit: -");
        }
    }

    @FXML
    private void handleUpdateProduct() {
        if (editingProduct == null) {
            return;
        }

        try {
            editingProduct.setProductName(requiredText(txtProductName, "Product name"));
            if (comboCategory != null && comboCategory.getValue() != null) {
                editingProduct.setCategory(comboCategory.getValue());
            }
            editingProduct.setOpeningStock(requiredInt(txtQuantity, "Opening stock"));
            editingProduct.setPurchasePrice(requiredDecimal(txtPurchasePrice, "Purchase price"));
            editingProduct.setSellingPrice(requiredDecimal(txtSellingPrice, "Selling price"));
            if (txtLowStockThreshold != null) {
                editingProduct.setLowStockThreshold(requiredInt(txtLowStockThreshold, "Low stock threshold"));
            }

            inventoryService.saveProduct(editingProduct);
            handleCancelModal();
            refreshTable();
        } catch (IllegalArgumentException exception) {
            showErrorAlert("Update Error", exception.getMessage());
        }
    }

    private String requiredText(TextField field, String label) {
        String value = optionalText(field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    private String optionalText(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private int requiredInt(TextField field, String label) {
        try {
            return Integer.parseInt(requiredText(field, label));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    private BigDecimal requiredDecimal(TextField field, String label) {
        try {
            return new BigDecimal(requiredText(field, label));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be a valid amount.");
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        return "Rs." + safeAmount.toPlainString();
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

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
