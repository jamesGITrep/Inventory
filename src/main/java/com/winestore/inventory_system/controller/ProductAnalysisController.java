package com.winestore.inventory_system.controller;

import java.util.List;
import java.util.Map;

import com.winestore.inventory_system.model.ProductAnalysisRow;
import com.winestore.inventory_system.service.ReportService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductAnalysisController {

    @FXML private BarChart<String, Number> topSellingChart;
    @FXML private PieChart marginPieChart;
    @FXML private TableView<ProductAnalysisRow> detailedSalesTable;

    @FXML private TableColumn<ProductAnalysisRow, Integer> col_pId;
    @FXML private TableColumn<ProductAnalysisRow, String> col_pName;
    @FXML private TableColumn<ProductAnalysisRow, Integer> col_pSize;
    @FXML private TableColumn<ProductAnalysisRow, String> col_pCategory;
    @FXML private TableColumn<ProductAnalysisRow, Integer> col_volume;
    @FXML private TableColumn<ProductAnalysisRow, Double> col_revenue;
    @FXML private TableColumn<ProductAnalysisRow, Double> col_profit;
    @FXML private TableColumn<ProductAnalysisRow, Double> col_margin;

    private ReportService reportService;
    private boolean initialized;

    @FXML
    public void initialize() {
        initialized = true;
        setupTableColumns();
        if (reportService != null) {
            loadAnalysisData();
        }
    }

    public void setReportService(ReportService reportService) {
        this.reportService = reportService;
        if (initialized) {
            loadAnalysisData();
        }
    }

    private void setupTableColumns() {
        col_pId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        col_pName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        col_pSize.setCellValueFactory(new PropertyValueFactory<>("sizeMl"));
        col_pCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        col_volume.setCellValueFactory(new PropertyValueFactory<>("totalVolume"));
        col_revenue.setCellValueFactory(new PropertyValueFactory<>("totalRevenue"));
        col_profit.setCellValueFactory(new PropertyValueFactory<>("totalProfit"));
        col_margin.setCellValueFactory(new PropertyValueFactory<>("avgMargin"));
    }

    private void loadAnalysisData() {
        if (reportService == null) return;
        
        List<ProductAnalysisRow> data = reportService.getProductWiseAnalysis();
        if (detailedSalesTable != null) {
            detailedSalesTable.setItems(FXCollections.observableArrayList(data));
        }

        if (topSellingChart != null) {
            topSellingChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Units Sold");
            data.stream()
                    .limit(5)
                    .forEach(item -> series.getData().add(new XYChart.Data<>(item.getProductName(), item.getTotalVolume())));
            if (!series.getData().isEmpty()) {
                topSellingChart.getData().add(series);
            }
        }

        if (marginPieChart != null) {
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            Map<String, Double> categoryProfits = reportService.getCategoryProfitMap();
            categoryProfits.forEach((category, profit) -> pieData.add(new PieChart.Data(category, profit)));
            marginPieChart.setData(pieData);
        }
    }
}
