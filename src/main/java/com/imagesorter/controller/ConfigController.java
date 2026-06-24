package com.imagesorter.controller;

import com.imagesorter.model.ConfigSettings;
import com.imagesorter.service.ConfigService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the folder configuration dialog
 * Allows users to set up destination folders for hotkeys 1-9 and a-z
 */
public class ConfigController implements Initializable {

    @FXML private GridPane gridPane;

    // Action buttons
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button clearAllButton;

    @FXML private TextField trashFolderTextField;
    @FXML private Button browseTrashFolderButton;

    @FXML private TextField configFileTextField;
    @FXML private Button browseConfigFileButton;

    // Advanced configuration fields
    @FXML private javafx.scene.control.CheckBox confirmDeleteCheckBox;
    @FXML private TextField cacheSizeField;
    @FXML private TextField prevCacheField;
    @FXML private TextField nextCacheField;
    @FXML private TextField undoSizeField;
    @FXML private TextField threadPoolSizeField;
    @FXML private TextField thumbnailSizeField;
    @FXML private TextField thumbnailCountField;
    @FXML private TextField imageQualityPxField;
    @FXML private TextField metadataCacheSizeField;
    @FXML private TextField supportedExtensionsField;
    @FXML private TextField supportedVideoExtensionsField;

    // Services
    private ConfigService configService;

    // Arrays for easy iteration
    private final List<TextField> textFields = new ArrayList<>();
    private final List<Button> browseButtons = new ArrayList<>();
    private final List<String> hotkeys = new ArrayList<>();

    // Callback for when configuration is saved
    private Runnable onConfigSaved;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configService = ConfigService.getInstance();

        // Initialize hotkeys
        initializeHotkeys();

        // Create and add UI elements dynamically
        populateGridPane();

        // Setup button handlers
        setupButtonHandlers();

        // Load current configuration
        loadCurrentConfig();

        // Setup text field validation
        setupTextFieldValidation();
    }

    private void initializeHotkeys() {
        // Add numbers 1-9
        for (int i = 1; i <= 9; i++) {
            hotkeys.add(String.valueOf(i));
        }
        // Add letters a-z
        for (char c = 'a'; c <= 'z'; c++) {
            hotkeys.add(String.valueOf(c));
        }
    }

    private void populateGridPane() {
        for (int i = 0; i < hotkeys.size(); i++) {
            String hotkey = hotkeys.get(i);

            // Label
            Label label = new Label("Hotkey " + hotkey + ":");
            label.getStyleClass().add("hotkey-label");

            // TextField
            TextField textField = new TextField();
            textField.setPromptText("Select folder for hotkey " + hotkey + "...");
            textFields.add(textField);

            // Button
            Button button = new Button("Browse...");
            button.getStyleClass().add("browse-button");
            browseButtons.add(button);

            gridPane.add(label, 0, i);
            gridPane.add(textField, 1, i);
            gridPane.add(button, 2, i);
        }
    }

    private void setupButtonHandlers() {
        // Setup browse button handlers
        for (int i = 0; i < browseButtons.size(); i++) {
            final String hotkey = hotkeys.get(i);
            final TextField textField = textFields.get(i);

            browseButtons.get(i).setOnAction(e -> browseForFolder(hotkey, textField));
        }

        browseTrashFolderButton.setOnAction(e -> browseForFolder("Trash Folder", trashFolderTextField));
        browseConfigFileButton.setOnAction(e -> browseForConfigFile());

        // Setup action buttons
        saveButton.setOnAction(e -> saveConfiguration());
        cancelButton.setOnAction(e -> closeDialog());
        clearAllButton.setOnAction(e -> clearAllFields());
    }

    private void setupTextFieldValidation() {
        // Add real-time validation for text fields
        for (TextField textField : textFields) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                validateTextField(textField);
            });
        }
        trashFolderTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateTextField(trashFolderTextField);
        });
        configFileTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateConfigFileField();
        });

        // Advanced settings numeric field validation
        TextField[] numericFields = {
            cacheSizeField, prevCacheField, nextCacheField, undoSizeField,
            threadPoolSizeField, metadataCacheSizeField, thumbnailSizeField, thumbnailCountField, imageQualityPxField
        };
        for (TextField f : numericFields) {
            if (f != null) {
                f.textProperty().addListener((observable, oldValue, newValue) -> {
                    validateNumericField(f);
                });
            }
        }
    }

    private void validateConfigFileField() {
        String path = configFileTextField.getText().trim();
        if (path.isEmpty()) {
            configFileTextField.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
        } else {
            File file = new File(path);
            File parent = file.getParentFile();
            boolean parentExists = parent == null || parent.exists();
            boolean isJson = path.toLowerCase().endsWith(".json");
            if (parentExists && isJson) {
                configFileTextField.setStyle("-fx-border-color: green; -fx-border-width: 1px;");
            } else {
                configFileTextField.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
            }
        }
    }

    private void validateTextField(TextField textField) {
        String path = textField.getText().trim();

        if (path.isEmpty()) {
            // Empty is valid (means not configured)
            textField.setStyle("");
        } else {
            File folder = new File(path);
            if (folder.exists() && folder.isDirectory()) {
                textField.setStyle("-fx-border-color: green; -fx-border-width: 1px;");
            } else {
                textField.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
            }
        }
    }

    private void validateNumericField(TextField textField) {
        String val = textField.getText().trim();
        try {
            int num = Integer.parseInt(val);
            if (num >= 0) {
                textField.setStyle("-fx-border-color: green; -fx-border-width: 1px;");
            } else {
                textField.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
            }
        } catch (NumberFormatException e) {
            textField.setStyle("-fx-border-color: red; -fx-border-width: 1px;");
        }
    }

    private void browseForFolder(String hotkey, TextField textField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder for Hotkey " + hotkey);

        // Set initial directory if current path is valid
        String currentPath = textField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                directoryChooser.setInitialDirectory(currentDir);
            } else if (currentDir.getParentFile() != null && currentDir.getParentFile().exists()) {
                directoryChooser.setInitialDirectory(currentDir.getParentFile());
            }
        }

        Stage stage = (Stage) textField.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);

        if (selectedDirectory != null) {
            textField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    private void browseForConfigFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose Configuration File");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        
        String currentPath = configFileTextField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            File parentDir = currentFile.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                fileChooser.setInitialDirectory(parentDir);
            }
            fileChooser.setInitialFileName(currentFile.getName());
        } else {
            fileChooser.setInitialFileName("image_sort_config.json");
        }
        
        Stage stage = (Stage) configFileTextField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            configFileTextField.setText(selectedFile.getAbsolutePath());
            if (selectedFile.exists()) {
                ConfigSettings loadedSettings = configService.loadConfigFromPath(selectedFile.getAbsolutePath());
                if (loadedSettings != null) {
                    loadConfigIntoUI(loadedSettings);
                    showAlert(Alert.AlertType.INFORMATION, "Config Loaded", "Configuration loaded from existing file:\n" + selectedFile.getName());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to load configuration from:\n" + selectedFile.getName());
                }
            }
        }
    }

    private void loadCurrentConfig() {
        configFileTextField.setText(configService.getConfigPath() != null ? configService.getConfigPath() : "");
        loadConfigIntoUI(configService.getConfig());
    }

    private void loadConfigIntoUI(ConfigSettings config) {
        if (config == null) return;

        for (int i = 0; i < hotkeys.size(); i++) {
            String hotkey = hotkeys.get(i);
            String path = config.getFolderPath(hotkey);
            textFields.get(i).setText(path != null ? path : "");
        }

        trashFolderTextField.setText(config.getTrashFolderPath() != null ? config.getTrashFolderPath() : "");

        confirmDeleteCheckBox.setSelected(config.isConfirmDelete());
        cacheSizeField.setText(String.valueOf(config.getCacheSize()));
        prevCacheField.setText(String.valueOf(config.getPrevCache()));
        nextCacheField.setText(String.valueOf(config.getNextCache()));
        undoSizeField.setText(String.valueOf(config.getUndoSize()));
        threadPoolSizeField.setText(String.valueOf(config.getThreadPoolSize()));
        metadataCacheSizeField.setText(String.valueOf(config.getMetadataCacheSize()));
        thumbnailSizeField.setText(String.valueOf(config.getThumbnailSize()));
        thumbnailCountField.setText(String.valueOf(config.getThumbnailCount()));
        imageQualityPxField.setText(String.valueOf(config.getImageQualityPx()));

        if (config.getSupportedExtensions() != null) {
            supportedExtensionsField.setText(String.join(", ", config.getSupportedExtensions()));
        } else {
            supportedExtensionsField.setText("");
        }

        if (config.getSupportedVideoExtensions() != null) {
            supportedVideoExtensionsField.setText(String.join(", ", config.getSupportedVideoExtensions()));
        } else {
            supportedVideoExtensionsField.setText("");
        }

        // Validate all fields after loading
        for (TextField textField : textFields) {
            validateTextField(textField);
        }
        validateTextField(trashFolderTextField);
        validateConfigFileField();
        
        // Validate advanced fields
        validateNumericField(cacheSizeField);
        validateNumericField(prevCacheField);
        validateNumericField(nextCacheField);
        validateNumericField(undoSizeField);
        validateNumericField(threadPoolSizeField);
        validateNumericField(metadataCacheSizeField);
        validateNumericField(thumbnailSizeField);
        validateNumericField(thumbnailCountField);
        validateNumericField(imageQualityPxField);
    }

    private void applyFieldsToConfig(ConfigSettings config) {
        // Collect all folder paths
        for (int i = 0; i < hotkeys.size(); i++) {
            String hotkey = hotkeys.get(i);
            String path = textFields.get(i).getText().trim();
            config.setFolderPath(hotkey, path.isEmpty() ? null : path);
        }

        config.setTrashFolderPath(trashFolderTextField.getText().trim().isEmpty() ? null : trashFolderTextField.getText().trim());

        // Advanced settings
        config.setConfirmDelete(confirmDeleteCheckBox.isSelected());
        config.setCacheSize(parseInteger(cacheSizeField.getText(), config.getCacheSize()));
        config.setPrevCache(parseInteger(prevCacheField.getText(), config.getPrevCache()));
        config.setNextCache(parseInteger(nextCacheField.getText(), config.getNextCache()));
        config.setUndoSize(parseInteger(undoSizeField.getText(), config.getUndoSize()));
        config.setThreadPoolSize(parseInteger(threadPoolSizeField.getText(), config.getThreadPoolSize()));
        config.setMetadataCacheSize(parseInteger(metadataCacheSizeField.getText(), config.getMetadataCacheSize()));
        config.setThumbnailSize(parseInteger(thumbnailSizeField.getText(), config.getThumbnailSize()));
        config.setThumbnailCount(parseInteger(thumbnailCountField.getText(), config.getThumbnailCount()));
        config.setImageQualityPx(parseInteger(imageQualityPxField.getText(), config.getImageQualityPx()));

        config.setSupportedExtensions(parseExtensions(supportedExtensionsField.getText()));
        config.setSupportedVideoExtensions(parseExtensions(supportedVideoExtensionsField.getText()));
    }

    private int parseInteger(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private java.util.HashSet<String> parseExtensions(String text) {
        java.util.HashSet<String> set = new java.util.HashSet<>();
        if (text == null || text.trim().isEmpty()) {
            return set;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            String clean = part.trim().toLowerCase();
            if (clean.startsWith(".")) {
                clean = clean.substring(1);
            }
            if (!clean.isEmpty()) {
                set.add(clean);
            }
        }
        return set;
    }

    private void saveConfiguration() {
        ConfigSettings config = configService.getConfig();

        // Validate configuration
        if (validateConfiguration()) {
            // Check if config file path changed
            String newConfigPath = configFileTextField.getText().trim();
            if (!newConfigPath.isEmpty() && !newConfigPath.equals(configService.getConfigPath())) {
                File targetFile = new File(newConfigPath);
                if (targetFile.exists()) {
                    // Switch path and reload instead of overwriting
                    ConfigSettings loadedSettings = configService.loadConfigFromPath(newConfigPath);
                    if (loadedSettings != null) {
                        configService.setConfigPath(newConfigPath);
                        configService.loadConfig();
                        loadCurrentConfig();
                        showAlert(Alert.AlertType.INFORMATION, "Config Loaded", "Configuration loaded from existing file:\n" + targetFile.getName());
                        return; // Return so user can inspect and modify
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Error", "Failed to load configuration from:\n" + targetFile.getName());
                        return;
                    }
                } else {
                    applyFieldsToConfig(config);
                    configService.setConfigPath(newConfigPath);
                    configService.saveConfig();
                }
            } else {
                applyFieldsToConfig(config);
                // Save configuration to current path
                configService.saveConfig();
            }

            // Notify parent that config was saved
            if (onConfigSaved != null) {
                onConfigSaved.run();
            }

            // Show success message
            showAlert(Alert.AlertType.INFORMATION, "Success",
                    "Configuration saved successfully!");

            closeDialog();
        }
    }

    private boolean validateConfiguration() {
        boolean hasValidConfig = false;

        for (int i = 0; i < textFields.size(); i++) {
            String path = textFields.get(i).getText().trim();
            String hotkey = hotkeys.get(i);

            if (!path.isEmpty()) {
                File folder = new File(path);
                if (!folder.exists()) {
                    // Ask user if they want to create the folder
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Create Folder");
                    alert.setHeaderText("Folder does not exist");
                    alert.setContentText("Folder for hotkey " + hotkey + " does not exist:\n" +
                            path + "\n\nWould you like to create it?");

                    if (alert.showAndWait().orElse(null) ==
                            javafx.scene.control.ButtonType.OK) {

                        if (!folder.mkdirs()) {
                            showAlert(Alert.AlertType.ERROR, "Error",
                                    "Failed to create folder: " + path);
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else if (!folder.isDirectory()) {
                    showAlert(Alert.AlertType.ERROR, "Error",
                            "Path for hotkey " + hotkey + " is not a directory:\n" + path);
                    return false;
                }

                hasValidConfig = true;
            }
        }

        if (!hasValidConfig) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Configuration");
            alert.setHeaderText("No folders configured");
            alert.setContentText("You haven't configured any destination folders. " +
                    "You can still use the application, but hotkeys won't work until " +
                    "folders are configured.\n\nDo you want to continue?");

            return alert.showAndWait().orElse(null) ==
                    javafx.scene.control.ButtonType.OK;
        }

        return true;
    }

    private void clearAllFields() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear All");
        alert.setHeaderText("Clear all folder configurations");
        alert.setContentText("Are you sure you want to clear all configured folders?");

        if (alert.showAndWait().orElse(null) == javafx.scene.control.ButtonType.OK) {
            for (TextField textField : textFields) {
                textField.setText("");
            }
        }
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    public void setOnConfigSaved(Runnable callback) {
        this.onConfigSaved = callback;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
