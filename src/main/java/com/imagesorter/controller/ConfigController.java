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
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            configFileTextField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void loadCurrentConfig() {
        ConfigSettings config = configService.getConfig();

        for (int i = 0; i < hotkeys.size(); i++) {
            String hotkey = hotkeys.get(i);
            String path = config.getFolderPath(hotkey);
            textFields.get(i).setText(path != null ? path : "");
        }

        trashFolderTextField.setText(config.getTrashFolderPath() != null ? config.getTrashFolderPath() : "");
        configFileTextField.setText(configService.getConfigPath() != null ? configService.getConfigPath() : "");

        // Validate all fields after loading
        for (TextField textField : textFields) {
            validateTextField(textField);
        }
        validateTextField(trashFolderTextField);
        validateConfigFileField();
    }

    private void saveConfiguration() {
        ConfigSettings config = configService.getConfig();

        // Collect all folder paths
        for (int i = 0; i < hotkeys.size(); i++) {
            String hotkey = hotkeys.get(i);
            String path = textFields.get(i).getText().trim();
            config.setFolderPath(hotkey, path.isEmpty() ? null : path);
        }

        config.setTrashFolderPath(trashFolderTextField.getText().trim().isEmpty() ? null : trashFolderTextField.getText().trim());

        // Validate configuration
        if (validateConfiguration()) {
            // Check if config file path changed
            String newConfigPath = configFileTextField.getText().trim();
            if (!newConfigPath.isEmpty() && !newConfigPath.equals(configService.getConfigPath())) {
                File targetFile = new File(newConfigPath);
                if (targetFile.exists()) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Config File Exists");
                    confirmAlert.setHeaderText("Configuration File Already Exists");
                    confirmAlert.setContentText("The configuration file at:\n" + newConfigPath + 
                                               "\nalready exists.\n\n" +
                                               "Choose 'OK' to save current settings to this file (overwrite).\n" +
                                               "Choose 'Cancel' to load settings from this file instead.");
                                               
                    if (confirmAlert.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) == javafx.scene.control.ButtonType.OK) {
                        configService.setConfigPath(newConfigPath);
                    } else {
                        // Switch path and load
                        configService.setConfigPath(newConfigPath);
                        configService.loadConfig();
                    }
                } else {
                    configService.setConfigPath(newConfigPath);
                }
            } else {
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
