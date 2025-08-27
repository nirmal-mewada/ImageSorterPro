package com.imagesorter.controller;

import com.imagesorter.MemUtils;
import com.imagesorter.model.ConfigSettings;
import com.imagesorter.model.ImageFile;
import com.imagesorter.model.LastAction;
import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controller for the main application window
 * Handles UI interactions, keyboard shortcuts, and image operations
 */
public class MainController implements Initializable {


    // FXML injected components
    @FXML private MenuItem openFolderMenuItem;
    @FXML private MenuItem configureFoldersMenuItem;
    @FXML private MenuItem exitMenuItem;
    @FXML private MenuItem toggleLeftViewMenuItem;
    @FXML private MenuItem toggleThumbnailBoxMenuItem;
//    @FXML private MenuItem resetFocus;

    @FXML private  CheckBox clickToMoveCheckBox;

    @FXML private ListView<String> hotkeyListView;
    @FXML private ImageView imageView;
    @FXML private HBox thumbnailBox;
    @FXML private VBox leftVBox;
    @FXML private SplitPane horizontalSplitPane;
    @FXML private SplitPane verticalSplitPane;
    @FXML private ScrollPane imageScrollPane;

    @FXML private Label currentFileLabel;
    @FXML private Label progressLabel;
    @FXML private Label remainingLabel;
    @FXML private Label currentFolderLabel;
    @FXML private Label memoryUsage;
    @FXML private Label lastAction;

    @FXML private ProgressBar progressBar;

    // Services
    private ImageService imageService;
    private ConfigService configService;

    // Current state
    private List<ImageFile> currentImages;
    private int currentImageIndex = -1;
    private File currentSourceFolder;
    private final Deque<LastAction> lastActionInfo = new LinkedList<>();

    

    Consumer<Integer> progressUpdaterCallback = (i) -> {
//        Platform.runLater(() -> {
//            remainingLabel.setText(imageService.getCacheStats().toString());
//        });
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize services
        imageService = new ImageService();
        configService = ConfigService.getInstance();

        // Setup UI components
        setupImageView();
        setupHotkeyList();
        setupMenuItems();

        // Update UI with current config
        updateHotkeyList();

        // Set initial status
        updateStatusBar();

        openLastOpenedFolder();
        setupKeyboardFocus();
        updateThumbnails();
    }

    private void openLastOpenedFolder() {
        String lastOpenedPath = configService.getConfig().getLastOpenedFolder();
        if(lastOpenedPath != null && new File(lastOpenedPath).exists()){
            System.out.println("Loading: "+configService.getConfig().getLastOpenedFolder());
            loadImagesFromFolder(new File(lastOpenedPath));
        }
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



        // Bind image view size to scroll pane's viewport
        imageView.fitWidthProperty().bind(imageScrollPane.widthProperty().subtract(10));
        imageView.fitHeightProperty().bind(imageScrollPane.heightProperty().subtract(10));

        // Add mouse click handlers for navigation
        imageView.setOnMouseClicked(this::handleImageClick);


        // Add scroll handler for navigation
        imageScrollPane.setOnScroll(event -> {
            if (event.getDeltaY() < 0) {
                navigateNext();
            } else {
                navigatePrevious();
            }
            event.consume();
        });

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
            setupKeyboardFocus();
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
                    setTooltip(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                            "-fx-font-size: 11px; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-padding: 5 8 5 8;");

                    // Extract the folder path for tooltip
                    if (item.contains("] ") && !item.contains("<Not Configured>")) {
                        String path = item.substring(item.indexOf("] ") + 2);
                        setTooltip(new Tooltip("Click or press hotkey to move image to:\n" + path));
                    }
                }
            }
        });
        hotkeyListView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                Object target = mouseEvent.getTarget();
                while (target instanceof Node && !(target instanceof ListCell)) {
                    target = ((Node) target).getParent();
                }
                if (target instanceof ListCell) {
                    String value = (String) ((ListCell<?>) target).getItem();
//                    System.out.println("Clicked text: " + value);
                    if(value != null && value.trim() !="" && clickToMoveCheckBox.isSelected()){
                        //extract char from squire brackets at start and send ot to move files method
                        String key = value.charAt(1)+"";
                        if(key.matches("[1-9a-z]")){
                            moveToFolder(key,mouseEvent.isShiftDown());
                        }
                    }
                }
            }
        });
    }

    private void setupMenuItems() {
        openFolderMenuItem.setOnAction(e -> openFolder());
        configureFoldersMenuItem.setOnAction(e -> openConfigDialog());
        exitMenuItem.setOnAction(e -> Platform.exit());
        toggleLeftViewMenuItem.setOnAction(e -> toggleNodeVisibility(leftVBox));
        toggleThumbnailBoxMenuItem.setOnAction(e -> toggleNodeVisibility(thumbnailBox));
//        resetFocus.setOnAction(e -> setupKeyboardFocus());
    }

    private void toggleNodeVisibility(Node node) {
        if (node == leftVBox) {
            if (horizontalSplitPane != null) {
                if (horizontalSplitPane.getItems().contains(leftVBox)) {
                    horizontalSplitPane.getItems().remove(leftVBox);
                } else {
                    horizontalSplitPane.getItems().add(0, leftVBox);
                    horizontalSplitPane.setDividerPositions(0.2);
                }
            }
        } else if (node == thumbnailBox) {
            if (verticalSplitPane != null) {
                if (verticalSplitPane.getItems().contains(thumbnailBox)) {
                    verticalSplitPane.getItems().remove(thumbnailBox);
                } else {
                    verticalSplitPane.getItems().add(0, thumbnailBox);
                    verticalSplitPane.setDividerPositions(0.2);
                }
            }
        }
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
//        System.out.println(event);

        KeyCode code = event.getCode();
        String keyText = code.getName().toLowerCase();

        if (event.isControlDown() && code == KeyCode.Z) {
            undoLastAction();
        } else if(event.isControlDown() && event.isShiftDown() && keyText.matches("[1-9a-z]") ){
            chooseOnDemandFolder(event,keyText);
        } else  if (currentImages == null || currentImages.isEmpty()) {
            return;
        } else if(keyText.matches("[1-9a-z]") && !event.isControlDown()){
            moveToFolder(keyText, event.isShiftDown());
        } else {
            switch (code) {
                case RIGHT:
                    navigateNext();
                    break;
                case LEFT:
                    navigatePrevious();
                    break;
                case DELETE:
                    deleteCurrentImage();
                    break;
                case NUMPAD0:
                case DIGIT0:
                    moveToArchive(currentSourceFolder);
                    break;
                case O:
                    if(event.isControlDown())
                        openFolder();
                    break;
                case P:
                    if(event.isControlDown())
                        openConfigDialog();
                    break;
            }
        }
        event.consume();
    }
    private void chooseOnDemandFolder(KeyEvent event, String keyText) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder for Hotkey " + keyText);
        Stage stage = (Stage) imageView.getScene().getWindow();
        String defaultFileChooseLocation = configService.getConfig().getDefaultFileChooseLocation();
        if(defaultFileChooseLocation != null){
            directoryChooser.setInitialDirectory(new File(defaultFileChooseLocation));
        }

        File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory != null) {
            String absolutePath = selectedDirectory.getAbsolutePath();
            configService.getConfig().setFolderPath(keyText, absolutePath);
            lastAction.setText("Configuration saved: "+keyText+"->"+ absolutePath);
            configService.saveConfig();
            updateHotkeyList();
        }
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
        setupKeyboardFocus();

    }

    private void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Image Folder");

        if (currentSourceFolder != null) {
            directoryChooser.setInitialDirectory(currentSourceFolder);
        }

        File selectedDirectory = directoryChooser.showDialog(imageView.getScene().getWindow());

        if (selectedDirectory != null && selectedDirectory.exists()) {
            currentImageIndex = 0;
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
                    if(currentImageIndex < 1 ) // keep current index in case of reload
                        currentImageIndex = 0;

                    if (!currentImages.isEmpty()) {
                        displayCurrentImage();
                        updateThumbnails();
                        // Pre-cache next 10 images
                        imageService.preCacheImages(currentImages, currentImageIndex, configService.getConfig().getPrevCache(),configService.getConfig().getNextCache(), progressUpdaterCallback);
                    } else {
                        showAlert("No Images", "No supported image files found in the selected folder.");
                    }

                    progressBar.setVisible(false);
                    updateStatusBar();
                    configService.getConfig().setLastOpenedFolder(folder.getAbsolutePath());
                    currentFolderLabel.setText(folder.getAbsolutePath());
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

    private synchronized void displayCurrentImage() {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            imageView.setImage(null);
            return;
        }

        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        Image image = imageService.getCachedImage(currentImageFile);

        if (image != null) {
            imageView.setImage(image);
        } else {
            System.out.println("cache miss"+currentImageFile.getName());
            try {
                Image img = imageService.loadImage(currentImageFile);
                imageView.setImage(img);
            } catch (IOException e) {
                showAlert("Error", "Failed to load image: " + currentImageFile.getName());

            }
        }

        updateStatusBar();
        updateThumbnails();

        // Pre-cache surrounding images

        imageService.preCacheImages(currentImages, currentImageIndex, configService.getConfig().getPrevCache(),configService.getConfig().getNextCache(), progressUpdaterCallback);
    }

    private void updateThumbnails() {
        thumbnailBox.getChildren().clear();


        if (currentImages == null || currentImages.isEmpty()) {
            return;
        }

        int thumbnailCount = configService.getConfig().getThumbnailCount();
        int halfThumbnails = thumbnailCount / 2;
        int startIndex = Math.max(0, currentImageIndex - halfThumbnails);
        int endIndex = Math.min(currentImages.size() - 1, currentImageIndex + halfThumbnails);

        for (int i = startIndex; i <= endIndex; i++) {
            ImageFile imageFile = currentImages.get(i);
            Image image = null;
            try {
                image = imageService.loadImage(imageFile);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (image == null) {
                // If the image is not in the cache, we can show a placeholder or load it.
                // For now, let's just skip it.
                continue;
            }

            ImageView thumbnail = new ImageView(image);

            thumbnail.setPreserveRatio(true);
            thumbnail.getStyleClass().add("thumbnail-image");
            double thumbBoxheight = configService.getConfig().getThumbnailSize();
            if (i == currentImageIndex) {
                thumbnail.getStyleClass().add("thumbnail-selected");
            } else {
                thumbBoxheight = thumbBoxheight * 0.7;
            }
            thumbnail.setFitHeight(thumbBoxheight);
            thumbnail.setFitWidth(thumbBoxheight);
            thumbnailBox.getChildren().add(thumbnail);

        }
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

    private void moveToFolder(String hotkey, boolean archive) {
        ConfigSettings config = configService.getConfig();
        String folderPath = config.getFolderPath(hotkey);

        if (folderPath == null || folderPath.trim().isEmpty()) {
            showAlert("Configuration Error",
                    "Folder for hotkey '" + hotkey + "' is not configured. Please configure folders first.");
            return;
        }

        File destinationFolder = new File(folderPath);
        if (!destinationFolder.exists()) {
            if (!destinationFolder.mkdirs()) {
                showAlert("Error", "Failed to create destination folder: " + folderPath);
                return;
            }
        }
        if(archive)
            moveToArchive(destinationFolder);
        else
            moveCurrentImageTo(destinationFolder);
    }

    private void moveToArchive(File parent) {
        if (parent == null) {
            return;
        }

        File archiveFolder = new File(parent, "Archive");
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

        // Store last action
        addLastAction(new LastAction(LastAction.ActionType.MOVE, sourceFile, destinationFile));

        // Move file
        if (sourceFile.renameTo(destinationFile)) {
            // Remove from current list
            System.out.println("Moved: " + sourceFile.getAbsolutePath() + " -> " + destinationFile.getAbsolutePath());
            lastAction.setText("Last Action: [Moved] " + sourceFile.getName() +" ->" + destinationFolder.getPath());
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

        Optional<ButtonType> result = Optional.empty();
        if(configService.getConfig().isConfirmDelete()){
            result = alert.showAndWait();
        }
        if (!configService.getConfig().isConfirmDelete() || (result.isPresent() && result.get() == ButtonType.OK)) {
            ImageFile currentImageFile = currentImages.get(currentImageIndex);
            String trashFolderPath = configService.getConfig().getTrashFolderPath();

            if (trashFolderPath == null || trashFolderPath.trim().isEmpty()) {
                showAlert("Configuration Error", "Trash folder is not configured. Please configure it in the settings.");
                return;
            }

            File trashFolder = new File(trashFolderPath);
            if (!trashFolder.exists()) {
                if (!trashFolder.mkdirs()) {
                    showAlert("Error", "Failed to create trash folder: " + trashFolderPath);
                    return;
                }
            }

            File sourceFile = currentImageFile.getFile();
            File destinationFile = new File(trashFolder, sourceFile.getName());

            // Store last action
            addLastAction(new LastAction(LastAction.ActionType.DELETE, sourceFile, destinationFile));

            if (sourceFile.renameTo(destinationFile)) {
                lastAction.setText("Last Action: [Deleted] " + currentImageFile.getName());
                System.out.println("Last Action: [Deleted] "+currentImageFile.getFile().getAbsolutePath());
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
                showAlert("Error", "Failed to move the image to the trash folder.");
            }
        }
    }

    private void addLastAction(LastAction action) {
        if (lastActionInfo.size() >= configService.getConfig().getUndoSize()) {
            lastActionInfo.removeLast();
        }
        lastActionInfo.addFirst(action);
    }

    private void undoLastAction() {
        if (lastActionInfo.isEmpty()) {
            lastAction.setText("Last Action: No action to undo.");
            return;
        }

        LastAction lastActionToUndo = lastActionInfo.removeFirst();

        switch (lastActionToUndo.getActionType()) {
            case MOVE:
                File sourceFile = lastActionToUndo.getDestinationFile();
                File destinationFolder = lastActionToUndo.getSourceFile().getParentFile();
                File destFile = new File(destinationFolder, sourceFile.getName());
                if (sourceFile.renameTo(destFile)) {
                    lastAction.setText("Last Action: [Undo Move] " + sourceFile.getName());
                    System.out.println("Last Action: [Undo Move] "+sourceFile.getAbsolutePath()+" -> "+destFile.getAbsolutePath());
                    loadImagesFromFolder(currentSourceFolder); // Reload to refresh the list
                } else {
                    showAlert("Error", "Failed to undo move action.");
                }
                break;
            case DELETE:
                File fileToRestore = lastActionToUndo.getDestinationFile();
                File originalLocation = lastActionToUndo.getSourceFile().getParentFile();
                File destToRestore = new File(originalLocation, fileToRestore.getName());
                if (fileToRestore.renameTo(destToRestore)) {
                    lastAction.setText("Last Action: [Restored] " + fileToRestore.getName());
                    System.out.println("Last Action: [Restored] "+fileToRestore.getAbsolutePath()+" -> "+destToRestore.getAbsolutePath());
                    loadImagesFromFolder(currentSourceFolder); // Reload to refresh the list
                } else {
                    showAlert("Error", "Failed to restore the deleted file.");
                }
                break;
        }
    }


    private void openConfigDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imagesorter/view/config.fxml"));
            Parent root = loader.load();

            Stage configStage = new Stage();
            configStage.setTitle("Configure Folders");
            configStage.setScene(new Scene(root, 600, 800)); // Increased height
            configStage.initModality(Modality.APPLICATION_MODAL);
            configStage.initOwner(imageView.getScene().getWindow());

            ConfigController configController = loader.getController();
            configController.setOnConfigSaved(() -> updateHotkeyList());

            configStage.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to open configuration dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateHotkeyList() {
        hotkeyListView.getItems().clear();
        ConfigSettings config = configService.getConfig();

        // Add numbers 1-9
        for (int i = 1; i <= 9; i++) {
            String hotkey = String.valueOf(i);
            String path = config.getFolderPath(hotkey);
            if(path != null && !path.trim().isEmpty()){
                String displayText = String.format("[%s] %s", hotkey, path );
                hotkeyListView.getItems().add(displayText);
            }

        }

        // Add letters a-z
        for (char c = 'a'; c <= 'z'; c++) {
            String hotkey = String.valueOf(c);
            String path = config.getFolderPath(hotkey);
            if (path != null && !path.trim().isEmpty()) {
                String displayText = String.format("[%s] %s", hotkey, path );
                hotkeyListView.getItems().add(displayText);
            }
        }
    }

    private void updateStatusBar() {
        if (currentImages == null || currentImages.isEmpty()) {
            currentFileLabel.setText("No images loaded");
            progressLabel.setText("Image 0 / 0");
//            remainingLabel.setText("Remaining: 0");
            progressBar.setProgress(0);
        } else {
            ImageFile currentImageFile = currentImages.get(currentImageIndex);
            currentFileLabel.setText("Current File: " + currentImageFile.getName());

            int totalOriginal = currentImages.size();
            progressLabel.setText(String.format("Image %d / %d",
                    currentImageIndex + 1, currentImages.size()));
//            remainingLabel.setText("Remaining: " + currentImages.size());

            if (totalOriginal > 0) {
                double progress = (double) currentImageIndex / totalOriginal;
                progressBar.setProgress(progress);
            }
            memoryUsage.setText("Mem:"+ MemUtils.printHeapUsage());
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