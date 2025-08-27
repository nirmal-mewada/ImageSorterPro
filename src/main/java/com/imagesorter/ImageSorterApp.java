package com.imagesorter;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.imagesorter.service.ConfigService;

import java.util.Objects;

/**
 * Main Application class for Image Sorter Pro
 * Initializes JavaFX application and loads the main window
 */
public class ImageSorterApp extends Application {
    
    private static final String APP_TITLE = "Image Sorter by Nirmal";
    private static final String MAIN_FXML = "/com/imagesorter/view/main.fxml";
    private static final String STYLES_CSS = "/com/imagesorter/css/styles.css";
    
    @Override
    public void start(Stage primaryStage) {
        try {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
//            Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
            // Initialize configuration service
            ConfigService configService = ConfigService.getInstance();
            configService.loadConfig();
            
            // Load main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Parent root = loader.load();
            
            // Create scene
            Scene scene = new Scene(root, 1200, 800);
            
            // Add CSS styling
            String cssPath = getClass().getResource(STYLES_CSS).toExternalForm();
            scene.getStylesheets().add(cssPath);
            
            // Configure primary stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(ImageSorterApp.class.getResourceAsStream("/com/imagesorter/images/img_2.png"))));


            // Handle application close
            primaryStage.setOnCloseRequest(event -> {
                // Save configuration before closing
                configService.saveConfig();
                System.exit(0);
            });
            
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to start application: " + e.getMessage());
            System.exit(1);
        }
    }
    
    @Override
    public void stop() {
        // Clean up resources
        ConfigService.getInstance().saveConfig();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}