package com.winestore.inventory_system.controller;

import com.winestore.inventory_system.service.InventoryService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
public class PasswordController {

    @FXML private Label lblTitle;
    @FXML private Label lblStatus;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblConfirm;
    @FXML private VBox vboxConfirm;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ApplicationContext context;

    private boolean isSettingMode = false;

    @FXML
    public void initialize() {
        if (!inventoryService.isManagerPasswordConfigured()) {
            isSettingMode = true;
            lblTitle.setText("Set Application Password");
            lblStatus.setText("Initial setup: Please set a master password");
            vboxConfirm.setVisible(true);
            vboxConfirm.setManaged(true);
        } else {
            isSettingMode = false;
            lblTitle.setText("Inventory System Access");
            lblStatus.setText("Please enter your password to continue");
            vboxConfirm.setVisible(false);
            vboxConfirm.setManaged(false);
        }
    }

    @FXML
    void handleSubmit(ActionEvent event) {
        String password = txtPassword.getText();
        
        if (isSettingMode) {
            String confirm = txtConfirmPassword.getText();
            if (password.isEmpty()) {
                showError("Password cannot be empty!");
                return;
            }
            if (!password.equals(confirm)) {
                showError("Passwords do not match!");
                return;
            }
            
            inventoryService.updateManagerPassword(password);
            loadMainApp();
        } else {
            if (inventoryService.validateManagerPassword(password)) {
                loadMainApp();
            } else {
                showError("Incorrect password!");
            }
        }
    }

    private void showError(String message) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        
        // Add a simple shake animation effect or just clear password
        txtPassword.clear();
        txtConfirmPassword.clear();
    }

    private void loadMainApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/MainView.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            
            Stage stage = (Stage) txtPassword.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Wine Store Inventory System - Master");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error loading application dashboard.");
        }
    }
}
