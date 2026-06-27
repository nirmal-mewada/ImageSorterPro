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
            
            // Load main FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MAIN_FXML));
            Parent root = loader.load();

            // Create scene with base CSS pre-loaded; applyAppearance() will remove it if an AtlantaFX theme is active
            Scene scene = new Scene(root, 1200, 800);
            String cssPath = getClass().getResource(STYLES_CSS).toExternalForm();
            scene.getStylesheets().add(cssPath);

            // Apply saved appearance — sets UA stylesheet and manages custom CSS files
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
     * Applies the given appearance/theme to the scene.
     * "Light"/"Dark"/"System" use our custom macOS CSS with PrimerLight as the AtlantaFX base.
     * All other values are AtlantaFX theme names: styles.css and styles-dark.css are unloaded
     * so the AtlantaFX theme renders as its designers intended.
     */
    public static void applyAppearance(String appearance, javafx.scene.Scene scene) {
        var lightCssUrl = ImageSorterApp.class.getResource("/com/imagesorter/css/styles.css");
        var darkCssUrl  = ImageSorterApp.class.getResource("/com/imagesorter/css/styles-dark.css");
        String lightCssPath = lightCssUrl != null ? lightCssUrl.toExternalForm() : null;
        String darkCssPath  = darkCssUrl  != null ? darkCssUrl.toExternalForm()  : null;

        if ("Light".equals(appearance) || "Dark".equals(appearance) || "System".equals(appearance)) {
            // Custom macOS mode
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            if (lightCssPath != null && !scene.getStylesheets().contains(lightCssPath)) {
                scene.getStylesheets().add(0, lightCssPath);
            }
            boolean wantDark = "Dark".equals(appearance) || ("System".equals(appearance) && OsTheme.isDark());
            if (darkCssPath != null) {
                scene.getStylesheets().remove(darkCssPath);
                if (wantDark) scene.getStylesheets().add(darkCssPath);
            }
        } else {
            // AtlantaFX theme — unload our custom CSS, let AtlantaFX render unobstructed
            if (lightCssPath != null) scene.getStylesheets().remove(lightCssPath);
            if (darkCssPath  != null) scene.getStylesheets().remove(darkCssPath);
            switch (appearance) {
                case "Primer Light":     Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());    break;
                case "Primer Dark":      Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());     break;
                case "Nord Light":       Application.setUserAgentStylesheet(new NordLight().getUserAgentStylesheet());      break;
                case "Nord Dark":        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());       break;
                case "Cupertino Light":  Application.setUserAgentStylesheet(new CupertinoLight().getUserAgentStylesheet()); break;
                case "Cupertino Dark":   Application.setUserAgentStylesheet(new CupertinoDark().getUserAgentStylesheet());  break;
                case "Dracula":          Application.setUserAgentStylesheet(new Dracula().getUserAgentStylesheet());        break;
                default:                 Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());    break;
            }
        }
    }
}