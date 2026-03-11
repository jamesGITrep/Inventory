package com.inventory.system.controller;

import com.inventory.system.service.InventoryService;
import com.inventory.system.entity.Product;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MainController {

    private final InventoryService inventoryService;

    @FXML private TableView<Product> productTable;

    @FXML
    public void handleAddProduct() {
        // This is where you will open the 'Add Product' popup
        System.out.println("Opening Add Product Dialog...");
    }

    @FXML
    public void handleRecordSale() {
        // Calls the service logic you wrote last week
        System.out.println("Processing Sale...");
    }
}