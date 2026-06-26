package com.imagesorter;

import atlantafx.base.theme.*;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.imagesorter.service.ConfigService;
import com.imagesorter.util.OsTheme;

import java.io.File;
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
            // Initialize and load configuration service
            ConfigService configService = ConfigService.getInstance();
            configService.loadConfig();
            
            // Check for command line arguments
            Parameters params = getParameters();
            if (params != null && params.getRaw() != null && !params.getRaw().isEmpty()) {
                String startFolder = params.getRaw().get(0);
                System.out.println("Starting with folder: " + startFolder);
                File startFile = new File(startFolder);
                if (startFile.exists()) {
                    configService.getConfig().setLastOpenedFolder(startFile.getAbsolutePath());
                }
            }
            
            // Set saved theme
            setAppTheme(configService.getConfig().getTheme());

            // Load main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Parent root = loader.load();
            
            // Create scene
            Scene scene = new Scene(root, 1200, 800);
            
            // Add CSS styling
            String cssPath = getClass().getResource(STYLES_CSS).toExternalForm();
            scene.getStylesheets().add(cssPath);
            if (OsTheme.isDark()) {
                var darkCss = getClass().getResource("/com/imagesorter/css/styles-dark.css");
                if (darkCss != null) scene.getStylesheets().add(darkCss.toExternalForm());
            }

            // Configure primary stage
            primaryStage.setTitle(APP_TITLE);
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.getIcons().add(new Image(Objects.requireNonNull(ImageSorterApp.class.getResourceAsStream("/com/imagesorter/images/app_icon.jpg"))));


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

    /**
     * Applies the selected AtlantaFX theme stylesheet
     */
    public static void setAppTheme(String themeName) {
        if (themeName == null) {
            themeName = "Primer Light";
        }
        switch (themeName) {
            case "Primer Dark":
                Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
                break;
            case "Nord Light":
                Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());
                break;
            case "Nord Dark":
                Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
                break;
            case "Cupertino Light":
                Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet());
                break;
            case "Cupertino Dark":
                Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());
                break;
            case "Dracula":
                Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());
                break;
            case "Primer Light":
            default:
                Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
                break;
        }
    }
}