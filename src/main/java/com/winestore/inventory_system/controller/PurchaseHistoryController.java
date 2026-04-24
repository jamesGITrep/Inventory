package com.winestore.inventory_system.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.winestore.inventory_system.dto.PurchaseHistoryDTO;
import com.winestore.inventory_system.service.InventoryService;
import com.winestore.inventory_system.service.ReportService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

@Controller
public class PurchaseHistoryController {

    @FXML private TableView<PurchaseHistoryDTO> purchaseTable;
    @FXML private TableColumn<PurchaseHistoryDTO, Integer> colPurchaseId;
    @FXML private TableColumn<PurchaseHistoryDTO, Integer> colProductId;
    @FXML private TableColumn<PurchaseHistoryDTO, Integer> colQty;
    @FXML private TableColumn<PurchaseHistoryDTO, String> colProductName;
    @FXML private TableColumn<PurchaseHistoryDTO, Integer> colSize;
    @FXML private TableColumn<PurchaseHistoryDTO, String> colCategory;
    @FXML private TableColumn<PurchaseHistoryDTO, String> colManufacturer;
    @FXML private TableColumn<PurchaseHistoryDTO, BigDecimal> colPrice;
    @FXML private TableColumn<PurchaseHistoryDTO, BigDecimal> colTotal;
    @FXML private TableColumn<PurchaseHistoryDTO, LocalDate> colDate;

    @FXML private Label lblTotalPurchases;
    @FXML private Label lblTotalExpenditure;
    @FXML private Label lblTotalUnits;
    @FXML private ComboBox<String> comboCategory;
    @FXML private DatePicker dateStart;
    @FXML private DatePicker dateEnd;
    @FXML private TextField txtSearch;

    @Autowired
    private ReportService reportService;

    @Autowired
    private InventoryService inventoryService;

    private final ObservableList<PurchaseHistoryDTO> allData = FXCollections.observableArrayList();
    private final ObservableList<PurchaseHistoryDTO> filteredData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        loadData();
    }

    private void setupTable() {
        colPurchaseId.setCellValueFactory(new PropertyValueFactory<>("purchaseId"));
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colManufacturer.setCellValueFactory(new PropertyValueFactory<>("manufacturerName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantityAdded"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalCost"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        purchaseTable.setItems(filteredData);
    }

    private void setupFilters() {
        List<String> categories = inventoryService.getDistinctCategories();
        comboCategory.getItems().setAll("All");
        comboCategory.getItems().addAll(categories);
        comboCategory.setValue("All");

        dateStart.setValue(LocalDate.now().withDayOfMonth(1));
        dateEnd.setValue(LocalDate.now());
    }

    private void loadData() {
        LocalDate start = dateStart != null ? dateStart.getValue() : null;
        LocalDate end = dateEnd != null ? dateEnd.getValue() : null;

        if (start != null && end != null && start.isAfter(end)) {
            showError("Invalid Date Range", "Start date cannot be after end date.");
            dateStart.setValue(end.withDayOfMonth(1));
            start = dateStart.getValue();
        }

        String category = comboCategory != null ? comboCategory.getValue() : "";
        if ("All".equals(category)) {
            category = "";
        }

        allData.setAll(reportService.getPurchaseHistory(start, end, category));
        applyLocalFilter();
    }

    @FXML
    private void handleFilters() {
        loadData();
    }

    private void applyLocalFilter() {
        String query = txtSearch == null || txtSearch.getText() == null
                ? ""
                : txtSearch.getText().trim().toLowerCase(Locale.ENGLISH);

        List<PurchaseHistoryDTO> filtered = allData.stream()
                .filter(item -> query.isBlank()
                        || safe(item.getProductName()).toLowerCase(Locale.ENGLISH).contains(query))
                .collect(Collectors.toList());

        filteredData.setAll(filtered);
        updateSummaryCards(filtered);
    }

    private void updateSummaryCards(List<PurchaseHistoryDTO> data) {
        int totalPurchases = data.size();
        int totalUnits = data.stream().mapToInt(PurchaseHistoryDTO::getQuantityAdded).sum();
        BigDecimal totalExpenditure = data.stream()
                .map(PurchaseHistoryDTO::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalPurchases.setText(String.valueOf(totalPurchases));
        lblTotalExpenditure.setText(formatCurrency(totalExpenditure));
        lblTotalUnits.setText(totalUnits + " bottles");
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);
        return "Rs." + safeAmount.toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
