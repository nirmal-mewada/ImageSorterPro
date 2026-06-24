package com.imagesorter.controller;

import com.imagesorter.model.ConfigSettings;
import com.imagesorter.model.SortingRule;
import com.imagesorter.service.ConfigService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Rule-Based Sorting configuration dialog
 */
public class RulesController implements Initializable {

    @FXML private ListView<SortingRule> rulesListView;
    @FXML private Button deleteRuleButton;

    @FXML private ComboBox<SortingRule.Field> fieldComboBox;
    @FXML private ComboBox<SortingRule.Operator> operatorComboBox;
    @FXML private TextField valueTextField;
    @FXML private ComboBox<SortingRule.Action> actionComboBox;
    @FXML private TextField destFolderTextField;
    @FXML private Button browseDestFolderButton;
    @FXML private Button addRuleButton;

    @FXML private Button closeButton;

    private ConfigService configService;
    private ObservableList<SortingRule> rulesList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configService = ConfigService.getInstance();
        
        // Setup dropdown options
        fieldComboBox.setItems(FXCollections.observableArrayList(SortingRule.Field.values()));
        actionComboBox.setItems(FXCollections.observableArrayList(SortingRule.Action.values()));
        
        // Update operator list dynamically based on field choice
        fieldComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                operatorComboBox.setItems(FXCollections.observableArrayList(
                    SortingRule.Operator.CONTAINS, 
                    SortingRule.Operator.EQUALS, 
                    SortingRule.Operator.STARTS_WITH, 
                    SortingRule.Operator.ENDS_WITH, 
                    SortingRule.Operator.IS_SET
                ));
            }
        });

        // Toggle value field visibility/readability based on Operator type
        operatorComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                valueTextField.setDisable(newVal == SortingRule.Operator.IS_SET);
                if (newVal == SortingRule.Operator.IS_SET) {
                    valueTextField.clear();
                }
            }
        });

        // Load rules
        rulesList.addAll(configService.getConfig().getSortingRules());
        rulesListView.setItems(rulesList);

        // Bind browse button
        browseDestFolderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Rule Destination Folder");
            
            // Set initial directory if already set
            String current = destFolderTextField.getText();
            if (current != null && !current.isEmpty()) {
                File dir = new File(current);
                if (dir.exists() && dir.isDirectory()) {
                    directoryChooser.setInitialDirectory(dir);
                }
            } else {
                String lastOpened = configService.getConfig().getLastOpenedFolder();
                if (lastOpened != null && new File(lastOpened).exists()) {
                    directoryChooser.setInitialDirectory(new File(lastOpened));
                }
            }

            File selectedDir = directoryChooser.showDialog(browseDestFolderButton.getScene().getWindow());
            if (selectedDir != null) {
                destFolderTextField.setText(selectedDir.getAbsolutePath());
            }
        });

        // Add Rule
        addRuleButton.setOnAction(e -> {
            SortingRule.Field field = fieldComboBox.getValue();
            SortingRule.Operator op = operatorComboBox.getValue();
            String val = valueTextField.getText();
            SortingRule.Action action = actionComboBox.getValue();
            String dest = destFolderTextField.getText();

            if (field == null || op == null || action == null || dest == null || dest.isEmpty()) {
                showAlert("Validation Error", "Please fill in all required fields (Field, Operator, Action, Destination).");
                return;
            }

            if (op != SortingRule.Operator.IS_SET && (val == null || val.trim().isEmpty())) {
                showAlert("Validation Error", "Please fill in a comparison Value.");
                return;
            }

            SortingRule newRule = new SortingRule(field, op, val, action, dest);
            rulesList.add(newRule);
            saveRules();
            
            // Clear form
            fieldComboBox.setValue(null);
            operatorComboBox.setValue(null);
            valueTextField.clear();
            actionComboBox.setValue(null);
            destFolderTextField.clear();
        });

        // Delete Rule
        deleteRuleButton.setOnAction(e -> {
            SortingRule selected = rulesListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                rulesList.remove(selected);
                saveRules();
            } else {
                showAlert("Selection Error", "Please select a rule from the list to delete.");
            }
        });

        // Close
        closeButton.setOnAction(e -> {
            Stage stage = (Stage) closeButton.getScene().getWindow();
            stage.close();
        });
    }

    private void saveRules() {
        configService.getConfig().getSortingRules().clear();
        configService.getConfig().getSortingRules().addAll(rulesList);
        configService.saveConfig();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
