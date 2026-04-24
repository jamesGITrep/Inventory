package com.winestore.inventory_system.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.winestore.inventory_system.service.InventoryService;
import com.winestore.inventory_system.service.SecurityVaultService;
import com.winestore.inventory_system.service.SecurityVaultService.BackupResult;
import com.winestore.inventory_system.service.SecurityVaultService.RestoreResult;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

@Controller
public class SecurityController {

    @FXML private Button btnSecureBackup;
    @FXML private Button btnExportCSV;
    @FXML private Button btnUpdatePass;
    @FXML private Button btnRestore;
    @FXML private Label lblLastSecured;
    @FXML private PasswordField txtNewPassword;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SecurityVaultService securityVaultService;

    @FXML
    public void initialize() {
        refreshLastSecuredLabel();
    }

    @FXML
    private void handleSecureBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose Backup Destination");
        chooser.setInitialDirectory(defaultDirectory());

        Stage stage = currentStage();
        java.io.File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory == null) {
            return;
        }

        try {
            BackupResult result = securityVaultService.createSecureBackup(selectedDirectory.toPath());
            refreshLastSecuredLabel();
            showInfo("Secure Backup Created",
                    "Hidden vault created:\n" + result.getVaultFile()
                            + "\n\nVisible SQL file created:\n" + result.getReadableSqlFile());
        } catch (Exception exception) {
            showError("Backup Error", exception.getMessage());
        }
    }

    @FXML
    private void handleExportCSV() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose CSV Export Destination");
        chooser.setInitialDirectory(defaultDirectory());

        java.io.File selectedDirectory = chooser.showDialog(currentStage());
        if (selectedDirectory == null) {
            return;
        }

        try {
            Path csvFile = securityVaultService.exportInventoryCsv(
                    LocalDate.now(),
                    selectedDirectory.toPath(),
                    inventoryService.getProductsForDate(LocalDate.now()));
            showInfo("CSV Exported", "Read-only CSV saved to:\n" + csvFile);
        } catch (IOException exception) {
            showError("Export Error", exception.getMessage());
        }
    }

    @FXML
    private void handleUpdatePassword() {
        String newPassword = txtNewPassword != null && txtNewPassword.getText() != null
                ? txtNewPassword.getText().trim()
                : "";

        if (newPassword.isBlank()) {
            showError("Password Error", "Please enter a new password.");
            return;
        }

        inventoryService.updateManagerPassword(newPassword);
        txtNewPassword.clear();
        showInfo("Password Updated", "Manager password updated successfully.");
    }

    @FXML
    private void handleRestore() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Backup Folder");
        chooser.setInitialDirectory(defaultDirectory());

        java.io.File selectedDirectory = chooser.showDialog(currentStage());
        if (selectedDirectory == null) {
            return;
        }

        try {
            RestoreResult result = securityVaultService.restoreFromVault(selectedDirectory.toPath());
            if (result.isTampered()) {
                showError("Tampering Detected", result.getMessage());
                return;
            }
            showInfo("Restore Complete", result.getMessage());
            refreshLastSecuredLabel();
        } catch (Exception exception) {
            showError("Restore Error", exception.getMessage());
        }
    }

    private void refreshLastSecuredLabel() {
        if (lblLastSecured != null) {
            lblLastSecured.setText("Last Secured: " + securityVaultService.getLastSecuredDisplay());
        }
    }

    private java.io.File defaultDirectory() {
        java.io.File desktop = Path.of(System.getProperty("user.home"), "Desktop").toFile();
        return desktop.exists() ? desktop : Path.of(System.getProperty("user.home")).toFile();
    }

    private Stage currentStage() {
        if (btnSecureBackup != null && btnSecureBackup.getScene() != null) {
            return (Stage) btnSecureBackup.getScene().getWindow();
        }
        return null;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
