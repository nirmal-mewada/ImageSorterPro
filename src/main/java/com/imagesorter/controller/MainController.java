package com.imagesorter.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.imagesorter.model.ConfigSettings;
import com.imagesorter.model.ImageFile;
import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the main application window
 * Handles UI interactions, keyboard shortcuts, and image operations
 */
public class MainController implements Initializable {

    // FXML injected components
    @FXML private MenuItem openFolderMenuItem;
    @FXML private MenuItem configureFoldersMenuItem;
    @FXML private MenuItem exitMenuItem;

    @FXML private ListView<String> hotkeyListView;
    @FXML private ImageView imageView;
    @FXML private ScrollPane imageScrollPane;

    @FXML private Label currentFileLabel;
    @FXML private Label progressLabel;
    @FXML private Label remainingLabel;

    @FXML private ProgressBar progressBar;

    // Services
    private ImageService imageService;
    private ConfigService configService;

    // Current state
    private List<ImageFile> currentImages;
    private int currentImageIndex = -1;
    private File currentSourceFolder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        imageService = new ImageService();
        configService = ConfigService.getInstance();

        // Setup UI components
        setupImageView();
        setupHotkeyList();
        setupMenuItems();
        setupKeyboardFocus();

        // Update UI with current config
        updateHotkeyList();

        // Set initial status
        updateStatusBar();
    }

    private void setupKeyboardFocus() {
        // Set up keyboard focus and event handling
        Platform.runLater(() -> {
            if (imageView.getScene() != null) {
                // Make the root focusable and request focus
                imageView.getScene().getRoot().setFocusTraversable(true);
                imageView.getScene().getRoot().requestFocus();

                // Add key event filter to the scene to catch all key events
                imageView.getScene().setOnKeyPressed(this::handleKeyPressed);

                // Also add to the root node as backup
                imageView.getScene().getRoot().setOnKeyPressed(this::handleKeyPressed);
            }
        });
    }

    private void setupImageView() {
        // Configure image view properties
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setPickOnBounds(true); // Important for mouse events

        // Bind image view size to scroll pane
        imageView.fitWidthProperty().bind(imageScrollPane.widthProperty().subtract(20));
        imageView.fitHeightProperty().bind(imageScrollPane.heightProperty().subtract(20));

        // Add mouse click handlers for navigation
        imageView.setOnMouseClicked(this::handleImageClick);

        // Also add click handler to the scroll pane as backup
        imageScrollPane.setOnMouseClicked(event -> {
            if (event.getTarget() == imageScrollPane ||
                    event.getTarget() == imageScrollPane.getContent()) {

                double clickX = event.getX();
                double centerX = imageScrollPane.getWidth() / 2;

                if (clickX > centerX) {
                    navigateNext();
                } else {
                    navigatePrevious();
                }
                event.consume();
            }
        });
    }

    private void setupHotkeyList() {
        hotkeyListView.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px;");
                }
            }
        });
    }

    private void setupMenuItems() {
        openFolderMenuItem.setOnAction(e -> openFolder());
        configureFoldersMenuItem.setOnAction(e -> openConfigDialog());
        exitMenuItem.setOnAction(e -> Platform.exit());
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        if (currentImages == null || currentImages.isEmpty()) {
            return;
        }

        switch (event.getCode()) {
            case RIGHT:
                navigateNext();
                break;
            case LEFT:
                navigatePrevious();
                break;
            case DELETE:
                deleteCurrentImage();
                break;
            case DIGIT0:
            case NUMPAD0:
                moveToArchive();
                break;
            case DIGIT1: case NUMPAD1: moveToFolder(1); break;
            case DIGIT2: case NUMPAD2: moveToFolder(2); break;
            case DIGIT3: case NUMPAD3: moveToFolder(3); break;
            case DIGIT4: case NUMPAD4: moveToFolder(4); break;
            case DIGIT5: case NUMPAD5: moveToFolder(5); break;
            case DIGIT6: case NUMPAD6: moveToFolder(6); break;
            case DIGIT7: case NUMPAD7: moveToFolder(7); break;
            case DIGIT8: case NUMPAD8: moveToFolder(8); break;
            case DIGIT9: case NUMPAD9: moveToFolder(9); break;
        }

        event.consume();
    }

    private void handleImageClick(MouseEvent event) {
        if (event.getClickCount() == 1) {
            // Get the actual displayed image bounds
            double imageWidth = imageView.getFitWidth();
            if (imageWidth <= 0) {
                // Fallback to bounds if fitWidth is not set
                imageWidth = imageView.getBoundsInLocal().getWidth();
            }

            // Check which side of the image was clicked
            double clickX = event.getX();
            double centerX = imageWidth / 2;

            if (clickX > centerX) {
                navigateNext();
            } else {
                navigatePrevious();
            }
        }
        event.consume();
    }

    private void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Image Folder");

        if (currentSourceFolder != null) {
            directoryChooser.setInitialDirectory(currentSourceFolder);
        }

        File selectedDirectory = directoryChooser.showDialog(imageView.getScene().getWindow());

        if (selectedDirectory != null && selectedDirectory.exists()) {
            loadImagesFromFolder(selectedDirectory);
        }
    }

    private void loadImagesFromFolder(File folder) {
        currentSourceFolder = folder;

        // Show loading indicator
        progressBar.setVisible(true);
        currentFileLabel.setText("Loading images...");

        // Load images in background thread
        Task<List<ImageFile>> loadTask = new Task<List<ImageFile>>() {
            @Override
            protected List<ImageFile> call() throws Exception {
                return imageService.loadImagesFromFolder(folder);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    currentImages = getValue();
                    currentImageIndex = 0;

                    if (!currentImages.isEmpty()) {
                        displayCurrentImage();
                        // Pre-cache next 10 images
                        imageService.preCacheImages(currentImages, currentImageIndex, 10);
                    } else {
                        showAlert("No Images", "No supported image files found in the selected folder.");
                    }

                    progressBar.setVisible(false);
                    updateStatusBar();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showAlert("Error", "Failed to load images from folder: " + getException().getMessage());
                    progressBar.setVisible(false);
                });
            }
        };

        new Thread(loadTask).start();
    }

    private void displayCurrentImage() {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            imageView.setImage(null);
            return;
        }

        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        Image image = imageService.getCachedImage(currentImageFile);

        if (image != null) {
            imageView.setImage(image);
        } else {
            // Load image asynchronously
            Task<Image> imageTask = new Task<Image>() {
                @Override
                protected Image call() throws Exception {
                    return imageService.loadImage(currentImageFile);
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        imageView.setImage(getValue());
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        showAlert("Error", "Failed to load image: " + currentImageFile.getName());
                    });
                }
            };

            new Thread(imageTask).start();
        }

        updateStatusBar();

        // Pre-cache surrounding images
        imageService.preCacheImages(currentImages, currentImageIndex, 10);
    }

    private void navigateNext() {
        if (currentImages != null && currentImageIndex < currentImages.size() - 1) {
            currentImageIndex++;
            displayCurrentImage();
        }
    }

    private void navigatePrevious() {
        if (currentImages != null && currentImageIndex > 0) {
            currentImageIndex--;
            displayCurrentImage();
        }
    }

    private void moveToFolder(int folderNumber) {
        ConfigSettings config = configService.getConfig();
        String folderPath = config.getFolderPath(folderNumber);

        if (folderPath == null || folderPath.trim().isEmpty()) {
            showAlert("Configuration Error",
                    "Folder " + folderNumber + " is not configured. Please configure folders first.");
            return;
        }

        File destinationFolder = new File(folderPath);
        if (!destinationFolder.exists()) {
            if (!destinationFolder.mkdirs()) {
                showAlert("Error", "Failed to create destination folder: " + folderPath);
                return;
            }
        }

        moveCurrentImageTo(destinationFolder);
    }

    private void moveToArchive() {
        if (currentSourceFolder == null) {
            return;
        }

        File archiveFolder = new File(currentSourceFolder, "Archive");
        if (!archiveFolder.exists()) {
            if (!archiveFolder.mkdirs()) {
                showAlert("Error", "Failed to create Archive folder.");
                return;
            }
        }

        moveCurrentImageTo(archiveFolder);
    }

    private void moveCurrentImageTo(File destinationFolder) {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            return;
        }

        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        File sourceFile = currentImageFile.getFile();
        File destinationFile = new File(destinationFolder, sourceFile.getName());

        // Handle file name conflicts
        int counter = 1;
        while (destinationFile.exists()) {
            String name = sourceFile.getName();
            String baseName = name.substring(0, name.lastIndexOf('.'));
            String extension = name.substring(name.lastIndexOf('.'));
            destinationFile = new File(destinationFolder, baseName + "_" + counter + extension);
            counter++;
        }

        // Move file
        if (sourceFile.renameTo(destinationFile)) {
            // Remove from current list
            currentImages.remove(currentImageIndex);

            // Adjust index if necessary
            if (currentImageIndex >= currentImages.size() && !currentImages.isEmpty()) {
                currentImageIndex = currentImages.size() - 1;
            }

            // Display next image or clear if no more images
            if (!currentImages.isEmpty()) {
                displayCurrentImage();
            } else {
                imageView.setImage(null);
                updateStatusBar();
                showAlert("Complete", "All images have been sorted!");
            }
        } else {
            showAlert("Error", "Failed to move file to: " + destinationFolder.getPath());
        }
    }

    private void deleteCurrentImage() {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            return;
        }

        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Image");
        alert.setHeaderText("Delete Current Image");
        alert.setContentText("Are you sure you want to delete this image? This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ImageFile currentImageFile = currentImages.get(currentImageIndex);
            File fileToDelete = currentImageFile.getFile();

            if (fileToDelete.delete()) {
                // Remove from list
                currentImages.remove(currentImageIndex);

                // Adjust index
                if (currentImageIndex >= currentImages.size() && !currentImages.isEmpty()) {
                    currentImageIndex = currentImages.size() - 1;
                }

                // Display next image
                if (!currentImages.isEmpty()) {
                    displayCurrentImage();
                } else {
                    imageView.setImage(null);
                    updateStatusBar();
                }
            } else {
                showAlert("Error", "Failed to delete the image file.");
            }
        }
    }

    private void openConfigDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imagesorter/view/config.fxml"));
            Parent root = loader.load();

            Stage configStage = new Stage();
            configStage.setTitle("Configure Folders");
            configStage.setScene(new Scene(root, 600, 500));
            configStage.initModality(Modality.APPLICATION_MODAL);
            configStage.initOwner(imageView.getScene().getWindow());

            ConfigController configController = loader.getController();
            configController.setOnConfigSaved(() -> updateHotkeyList());

            configStage.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to open configuration dialog: " + e.getMessage());
        }
    }

    private void updateHotkeyList() {
        hotkeyListView.getItems().clear();
        ConfigSettings config = configService.getConfig();

        for (int i = 1; i <= 9; i++) {
            String path = config.getFolderPath(i);
            String displayText = String.format("[%d] %s", i,
                    path != null && !path.trim().isEmpty() ? path : "<Not Configured>");
            hotkeyListView.getItems().add(displayText);
        }
    }

    private void updateStatusBar() {
        if (currentImages == null || currentImages.isEmpty()) {
            currentFileLabel.setText("No images loaded");
            progressLabel.setText("Image 0 / 0");
            remainingLabel.setText("Remaining: 0");
            progressBar.setProgress(0);
        } else {
            ImageFile currentImageFile = currentImages.get(currentImageIndex);
            currentFileLabel.setText("Current File: " + currentImageFile.getName());

            int totalOriginal = currentImages.size() +
                    (currentSourceFolder != null ? imageService.getProcessedCount() : 0);
            progressLabel.setText(String.format("Image %d / %d",
                    currentImageIndex + 1, totalOriginal));
            remainingLabel.setText("Remaining: " + currentImages.size());

            if (totalOriginal > 0) {
                double progress = (double) imageService.getProcessedCount() / totalOriginal;
                progressBar.setProgress(progress);
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}