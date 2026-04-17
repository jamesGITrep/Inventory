package com.winestore.inventory_system.controller;

import org.springframework.stereotype.Component;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

@Component
public class MainController {

    // These IDs must match the "fx:id" you set in Scene Builder
    @FXML
    private TableView<?> inventoryTable; 

    @FXML
    private TextField searchField;

    
    public void initialize() {
        // This code runs as soon as the window opens
        System.out.println("Main Controller Initialized!");
    }
}