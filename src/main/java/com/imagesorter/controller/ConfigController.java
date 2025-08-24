package com.imagesorter.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import com.imagesorter.model.ConfigSettings;
import com.imagesorter.service.ConfigService;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the folder configuration dialog
 * Allows users to set up destination folders for hotkeys 1-9
 */
public class ConfigController implements Initializable {

    // FXML injected text fields for folder paths
    @FXML private TextField folder1TextField;
    @FXML private TextField folder2TextField;
    @FXML private TextField folder3TextField;
    @FXML private TextField folder4TextField;
    @FXML private TextField folder5TextField;
    @FXML private TextField folder6TextField;
    @FXML private TextField folder7TextField;
    @FXML private TextField folder8TextField;
    @FXML private TextField folder9TextField;

    // FXML injected browse buttons
    @FXML private Button browse1Button;
    @FXML private Button browse2Button;
    @FXML private Button browse3Button;
    @FXML private Button browse4Button;
    @FXML private Button browse5Button;
    @FXML private Button browse6Button;
    @FXML private Button browse7Button;
    @FXML private Button browse8Button;
    @FXML private Button browse9Button;

    // Action buttons
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button clearAllButton;

    // Services
    private ConfigService configService;

    // Arrays for easy iteration
    private TextField[] textFields;
    private Button[] browseButtons;

    // Callback for when configuration is saved
    private Runnable onConfigSaved;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configService = ConfigService.getInstance();

        // Initialize arrays for easy access
        initializeArrays();

        // Setup button handlers
        setupButtonHandlers();

        // Load current configuration
        loadCurrentConfig();

        // Setup text field validation
        setupTextFieldValidation();
    }

    private void initializeArrays() {
        textFields = new TextField[] {
                folder1TextField, folder2TextField, folder3TextField,
                folder4TextField, folder5TextField, folder6TextField,
                folder7TextField, folder8TextField, folder9TextField
        };

        browseButtons = new Button[] {
                browse1Button, browse2Button, browse3Button,
                browse4Button, browse5Button, browse6Button,
                browse7Button, browse8Button, browse9Button
        };
    }

    private void setupButtonHandlers() {
        // Setup browse button handlers
        for (int i = 0; i < browseButtons.length; i++) {
            final int folderIndex = i + 1;
            final TextField textField = textFields[i];

            browseButtons[i].setOnAction(e -> browseForFolder(folderIndex, textField));
        }

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

    private void browseForFolder(int folderNumber, TextField textField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder for Hotkey " + folderNumber);

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

    private void loadCurrentConfig() {
        ConfigSettings config = configService.getConfig();

        for (int i = 0; i < textFields.length; i++) {
            String path = config.getFolderPath(i + 1);
            textFields[i].setText(path != null ? path : "");
        }

        // Validate all fields after loading
        for (TextField textField : textFields) {
            validateTextField(textField);
        }
    }

    private void saveConfiguration() {
        ConfigSettings config = configService.getConfig();

        // Collect all folder paths
        for (int i = 0; i < textFields.length; i++) {
            String path = textFields[i].getText().trim();
            config.setFolderPath(i + 1, path.isEmpty() ? null : path);
        }

        // Validate configuration
        if (validateConfiguration()) {
            // Save configuration
            configService.saveConfig();

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

        for (int i = 0; i < textFields.length; i++) {
            String path = textFields[i].getText().trim();

            if (!path.isEmpty()) {
                File folder = new File(path);
                if (!folder.exists()) {
                    // Ask user if they want to create the folder
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Create Folder");
                    alert.setHeaderText("Folder does not exist");
                    alert.setContentText("Folder for hotkey " + (i + 1) + " does not exist:\n" +
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
                            "Path for hotkey " + (i + 1) + " is not a directory:\n" + path);
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
                    "You can still use the application, but hotkeys 1-9 won't work until " +
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