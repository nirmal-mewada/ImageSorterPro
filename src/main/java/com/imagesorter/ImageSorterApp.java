package com.imagesorter;

import atlantafx.base.theme.CupertinoDark;
import atlantafx.base.theme.CupertinoLight;
import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.NordDark;
import atlantafx.base.theme.NordLight;
import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.imagesorter.service.ConfigService;


import java.io.File;
import java.util.Objects;

/**
 * Main Application class for Image Sorter Pro
 * Initializes JavaFX application and loads the main window
 */
public class ImageSorterApp extends Application {
    
    private static final String APP_TITLE = "Image Sorter by Nirmal";
    private static final String MAIN_FXML = "/com/imagesorter/view/main.fxml";
    private static final String STRUCTURE_CSS = "/com/imagesorter/css/styles-structure.css";
    
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
            
            // Load main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 800);

            // styles-structure.css is ALWAYS loaded — pin button sizing, etc. that must
            // survive when styles.css is unloaded for AtlantaFX themes.
            var structureCssUrl = getClass().getResource(STRUCTURE_CSS);
            if (structureCssUrl != null) scene.getStylesheets().add(structureCssUrl.toExternalForm());

            // Apply saved appearance theme
            applyAppearance(configService.getConfig().getTheme(), scene);

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
     * Applies the given AtlantaFX theme to the scene.
     */
    public static void applyAppearance(String appearance, javafx.scene.Scene scene) {
        switch (appearance) {
            case "Primer Light":     Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());    break;
            case "Primer Dark":      Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());     break;
            case "Nord Light":       Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());      break;
            case "Nord Dark":        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());       break;
            case "Cupertino Light":  Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet()); break;
            case "Cupertino Dark":   Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());  break;
            case "Dracula":          Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());        break;
            default:                 Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());  break;
        }
    }
}