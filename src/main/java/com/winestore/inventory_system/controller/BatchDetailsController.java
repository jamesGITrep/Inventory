package com.winestore.inventory_system.controller;

import com.winestore.inventory_system.model.Product;
import com.winestore.inventory_system.model.ProductBatch;
import com.winestore.inventory_system.service.InventoryService;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for BatchDetailsPopup.fxml.
 *
 * <p>Workflow:
 * <ol>
 *   <li>The main dashboard calls {@link #initData(Product)} after loading the FXML.</li>
 *   <li>The controller fetches all batches for the product via {@link InventoryService}.</li>
 *   <li>The TableView is populated so the owner can see the full price-change history.</li>
 * </ol>
 */
@Component
public class BatchDetailsController {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @Autowired
    private InventoryService inventoryService;

    // --- FXML bindings ---
    @FXML private Label lblProductName;
    @FXML private Label lblTotalBatches;
    @FXML private Label lblTotalStock;
    @FXML private Label lblPriceRange;

    @FXML private TableView<ProductBatch>               batchTable;
    @FXML private TableColumn<ProductBatch, Integer>    colBatchId;
    @FXML private TableColumn<ProductBatch, String>     colDate;
    @FXML private TableColumn<ProductBatch, BigDecimal> colBuyPrice;
    @FXML private TableColumn<ProductBatch, BigDecimal> colSellPrice;
    @FXML private TableColumn<ProductBatch, Integer>    colStock;
    @FXML private TableColumn<ProductBatch, String>     colStatus;

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    /**
     * Called by the launching controller (e.g. MainController) immediately
     * after {@code FXMLLoader.load()} to inject the product context.
     *
     * @param product the product whose batch history should be displayed
     */
    public void initData(Product product) {
        if (product == null) return;

        List<ProductBatch> batches = inventoryService.getBatchesForProduct(product.getProductId());
        populateSummaryLabels(product, batches);
        populateTable(batches);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void populateSummaryLabels(Product product, List<ProductBatch> batches) {
        if (lblProductName != null) {
            String size = product.getSizeMl() != null && product.getSizeMl() > 0
                    ? " (" + product.getSizeMl() + " ml)" : "";
            lblProductName.setText(product.getProductName() + size);
        }

        int totalBatches = batches.size();
        int totalStock   = batches.stream().mapToInt(b -> b.getCurrentStockInBatch() == null ? 0 : b.getCurrentStockInBatch()).sum();
        long active      = batches.stream().filter(b -> b.getCurrentStockInBatch() != null && b.getCurrentStockInBatch() > 0).count();

        if (lblTotalBatches != null) {
            lblTotalBatches.setText(totalBatches + " batch(es)  ·  " + active + " active");
        }
        if (lblTotalStock != null) {
            lblTotalStock.setText(totalStock + " units total");
        }

        // Build buy-price range from active batches only
        if (lblPriceRange != null) {
            BigDecimal min = batches.stream()
                    .filter(b -> b.getCurrentStockInBatch() != null && b.getCurrentStockInBatch() > 0)
                    .map(ProductBatch::getPurchasePrice)
                    .filter(p -> p != null)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
            BigDecimal max = batches.stream()
                    .filter(b -> b.getCurrentStockInBatch() != null && b.getCurrentStockInBatch() > 0)
                    .map(ProductBatch::getPurchasePrice)
                    .filter(p -> p != null)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            if (min == null) {
                lblPriceRange.setText("Cost: –");
            } else if (min.compareTo(max) == 0) {
                lblPriceRange.setText("Cost: Rs." + fmt(min));
            } else {
                lblPriceRange.setText("Cost range: Rs." + fmt(min) + " – Rs." + fmt(max));
            }
        }
    }

    private void populateTable(List<ProductBatch> batches) {
        if (batchTable == null) return;

        // Batch ID column
        if (colBatchId != null) {
            colBatchId.setCellValueFactory(new PropertyValueFactory<>("batchId"));
        }

        // Date received column (formatted)
        if (colDate != null) {
            colDate.setCellValueFactory(cellData -> {
                String formatted = cellData.getValue().getBatchCreatedAt() != null
                        ? cellData.getValue().getBatchCreatedAt().format(DATE_FMT)
                        : "–";
                return new ReadOnlyObjectWrapper<>(formatted);
            });
        }

        // Cost price column
        if (colBuyPrice != null) {
            colBuyPrice.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
            colBuyPrice.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : "Rs." + fmt(item));
                }
            });
        }

        // Sell price column
        if (colSellPrice != null) {
            colSellPrice.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
            colSellPrice.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : "Rs." + fmt(item));
                }
            });
        }

        // Qty remaining column
        if (colStock != null) {
            colStock.setCellValueFactory(new PropertyValueFactory<>("currentStockInBatch"));
        }

        // Status column (Active / Depleted)
        if (colStatus != null) {
            colStatus.setCellValueFactory(cellData -> {
                int stock = cellData.getValue().getCurrentStockInBatch() == null
                        ? 0 : cellData.getValue().getCurrentStockInBatch();
                return new ReadOnlyObjectWrapper<>(stock > 0 ? "Active" : "Depleted");
            });
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else if ("Active".equals(item)) {
                        setText("✔  Active");
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else {
                        setText("✖  Depleted");
                        setStyle("-fx-text-fill: #94a3b8;");
                    }
                }
            });
        }

        batchTable.setItems(FXCollections.observableArrayList(batches));
        batchTable.setPlaceholder(new Label("No batch records found for this product."));
    }

    // -------------------------------------------------------------------------
    // FXML handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleClose() {
        Stage stage = (Stage) batchTable.getScene().getWindow();
        stage.close();
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private String fmt(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
