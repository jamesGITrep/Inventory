package com.winestore.inventory_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@SpringBootApplication
@EnableScheduling
public class InventorySystemApplication extends Application {

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        // Triggers the JavaFX lifecycle
        Application.launch(InventorySystemApplication.class, args);
    }

    @Override
    public void init() throws Exception {
        // Starts the Spring Boot backend and database connection
        springContext = SpringApplication.run(InventorySystemApplication.class);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the Password protection screen first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/PasswordView.fxml"));
            
            // Allows Spring to inject dependencies into the UI Controller
            loader.setControllerFactory(springContext::getBean);
            
            Parent root = loader.load();
            primaryStage.setTitle("Wine Store Inventory System - Security");
            primaryStage.setScene(new Scene(root));
            
            // Show the login/setup screen
            primaryStage.show();
            
        } catch (Exception e) {
            System.err.println("CRITICAL UI ERROR: Could not load PasswordView.fxml");
            System.err.println("REASON: " + e.getMessage());
            e.printStackTrace(); 
        }
    }

    @Override
    public void stop() {
        // Cleanly shuts down the Spring Context and MySQL connections
        if (springContext != null) {
            springContext.close();
        }
        System.exit(0);
    }
}