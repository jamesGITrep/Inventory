package com.winestore.inventory_system;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFXApplication extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        // This starts the Spring Boot background logic
        this.context = new SpringApplicationBuilder(InventorySystemApplication.class).run();
    }

   @Override
public void start(Stage primaryStage) throws Exception {
    // Load the FXML file from the resources folder
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_dashboard.fxml"));
    
    // Set the Spring context into the loader so we can use Spring beans in Controllers
    loader.setControllerFactory(context::getBean);
    
    Parent root = loader.load();
    Scene scene = new Scene(root);
    
    primaryStage.setTitle("Wine Store Inventory System - Master");
    primaryStage.setScene(scene);
    primaryStage.show();

    }

    @Override
    public void stop() {
        this.context.close();
        Platform.exit();
    }
}