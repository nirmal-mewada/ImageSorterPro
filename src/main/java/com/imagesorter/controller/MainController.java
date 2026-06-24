package com.imagesorter.controller;

import com.imagesorter.MemUtils;
import com.imagesorter.model.ConfigSettings;
import com.imagesorter.model.ImageFile;
import com.imagesorter.model.LastAction;
import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;
import com.imagesorter.utils.ImageUtils;
import com.imagesorter.model.StagedAction;
import com.imagesorter.model.SortingRule;
import com.imagesorter.videoplayer.Player;
import com.imagesorter.videoplayer.FastVideoThumbnailUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

import static com.imagesorter.utils.ImageUtils.*;

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
    @FXML private MenuItem toggleRightViewMenuItem;
    @FXML private MenuItem toggleThumbnailBoxMenuItem;
    @FXML private MenuItem helpShortcutsMenuItem;
//    @FXML private MenuItem resetFocus;

    @FXML private  CheckBox clickToMoveCheckBox;
    @FXML private  CheckBox autoAdvanceCheckBox;

    @FXML private ListView<String> hotkeyListView;
    @FXML private VBox rightVBox;
    @FXML private GridPane metadataGridPane;
    @FXML private ImageView imageView;
    @FXML private HBox thumbnailBox;
    @FXML private HBox thumbnailContainer;
    @FXML private ToggleButton pinLeftButton;
    @FXML private ToggleButton pinRightButton;
    @FXML private ToggleButton pinThumbnailButton;
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

    // Injected components for new features
    @FXML private Menu bookmarksMenu;
    @FXML private MenuItem addBookmarkMenuItem;
    @FXML private MenuItem removeBookmarkMenuItem;
    @FXML private SeparatorMenuItem bookmarksSeparator;

    @FXML private RadioMenuItem actionModeMoveMenuItem;
    @FXML private RadioMenuItem actionModeCopyMenuItem;
    @FXML private RadioMenuItem actionModeStagedMoveMenuItem;
    @FXML private RadioMenuItem actionModeStagedCopyMenuItem;

    @FXML private CheckMenuItem slideshowPlayMenuItem;
    @FXML private RadioMenuItem interval1sMenuItem;
    @FXML private RadioMenuItem interval2sMenuItem;
    @FXML private RadioMenuItem interval3sMenuItem;
    @FXML private RadioMenuItem interval5sMenuItem;
    @FXML private RadioMenuItem interval10sMenuItem;
    @FXML private RadioMenuItem intervalCustomMenuItem;

    @FXML private MenuItem toggleFullScreenMenuItem;

    @FXML private Menu themeMenu;
    @FXML private RadioMenuItem themePrimerLightMenuItem;
    @FXML private RadioMenuItem themePrimerDarkMenuItem;
    @FXML private RadioMenuItem themeNordLightMenuItem;
    @FXML private RadioMenuItem themeNordDarkMenuItem;
    @FXML private RadioMenuItem themeCupertinoLightMenuItem;
    @FXML private RadioMenuItem themeCupertinoDarkMenuItem;
    @FXML private RadioMenuItem themeDraculaMenuItem;

    @FXML private MenuItem configureRulesMenuItem;
    @FXML private MenuItem applyRulesMenuItem;

    @FXML private RadioMenuItem sortByNameMenuItem;
    @FXML private RadioMenuItem sortByCreatedMenuItem;
    @FXML private RadioMenuItem sortByModifiedMenuItem;
    @FXML private RadioMenuItem sortBySizeMenuItem;
    @FXML private RadioMenuItem sortByRatingMenuItem;
    @FXML private RadioMenuItem sortByColorLabelMenuItem;
    @FXML private RadioMenuItem sortOrderAscMenuItem;
    @FXML private RadioMenuItem sortOrderDescMenuItem;


    @FXML private HBox starRatingBox;
    @FXML private Button star1;
    @FXML private Button star2;
    @FXML private Button star3;
    @FXML private Button star4;
    @FXML private Button star5;

    @FXML private HBox colorLabelBox;
    @FXML private Button colorRed;
    @FXML private Button colorYellow;
    @FXML private Button colorGreen;
    @FXML private Button colorBlue;
    @FXML private Button colorClear;

    @FXML private TextField metaTitleField;
    @FXML private TextField metaDescField;
    @FXML private TextField metaTagsField;
    @FXML private Button saveMetaButton;

    @FXML private HBox batchControlsBox;
    @FXML private Label stagedCountLabel;
    @FXML private Button commitBatchButton;
    @FXML private Button clearBatchButton;

    // Additional state variables
    private final List<StagedAction> stagedActions = new ArrayList<>();
    private Timeline slideshowTimeline;
    private int slideshowInterval = 3;
    private boolean isFullScreenListenerRegistered = false;
    private boolean wasLeftVisibleBeforeFS = true;
    private boolean wasRightVisibleBeforeFS = true;
    private boolean wasThumbnailVisibleBeforeFS = true;
    private boolean isUpdatingSortingUI = false;

    // Services
    private ImageService imageService;
    private ConfigService configService;

    // Current state
    private List<ImageFile> currentImages;
    private int currentImageIndex = -1;
    private File currentSourceFolder;
    private final Deque<LastAction> lastActionInfo = new LinkedList<>();
    @FXML private StackPane mediaContainer;
    private  Player currentMediaPlayer;
    private boolean keyFilterRegistered = false;
    private Task<Image> currentImageLoadTask = null;
    private String lastThumbnailStateKey = "";



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
        setupNewFeatures();
        setupMetadataEditor();

        // Update UI with current config
        updateHotkeyList();

        // Set initial status
        updateStatusBar();

        openLastOpenedFolder();
        setupKeyboardFocus();

        // Listen to width changes to dynamically recalculate and redraw thumbnails
        thumbnailBox.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                updateThumbnails();
            }
        });

        // Listen to height changes to dynamically resize thumbnails when separator is adjusted
        thumbnailBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                updateThumbnails();
            }
        });

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
            if (mediaContainer.getScene() != null) {
                // Make the root focusable and request focus
                mediaContainer.getScene().getRoot().setFocusTraversable(true);
                mediaContainer.getScene().getRoot().requestFocus();

                setupFullScreenListener();

                if (!keyFilterRegistered) {
                    mediaContainer.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
                    keyFilterRegistered = true;
                }
            }
        });
    }

    private void setupImageView() {
        // Configure image view properties
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(false);
        imageView.setPickOnBounds(true); // Important for mouse events



        // Bind image view size to scroll pane's viewport
        imageView.fitWidthProperty().bind(imageScrollPane.widthProperty().subtract(10));
        imageView.fitHeightProperty().bind(imageScrollPane.heightProperty().subtract(10));

        // Add mouse click handlers for navigation
        imageView.setOnMouseClicked(this::handleImageClick);


        // Add scroll handler for navigation
        EventHandler<ScrollEvent> scrollEventEventHandler = event -> {
            if (event.getDeltaY() < 0) {
                navigateNext();
            } else {
                navigatePrevious();
            }
            event.consume();
        };
        imageScrollPane.setOnScroll(scrollEventEventHandler);

        thumbnailBox.setOnScroll(scrollEventEventHandler);


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
                            "-fx-font-size: 12px; " +
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
        configureRulesMenuItem.setOnAction(e -> openRulesDialog());
        applyRulesMenuItem.setOnAction(e -> applyRulesToFolder());
        exitMenuItem.setOnAction(e -> Platform.exit());
        toggleLeftViewMenuItem.setOnAction(e -> toggleNodeVisibility(leftVBox));
        toggleRightViewMenuItem.setOnAction(e -> toggleNodeVisibility(rightVBox));
        toggleThumbnailBoxMenuItem.setOnAction(e -> toggleNodeVisibility(thumbnailContainer));
        helpShortcutsMenuItem.setOnAction(e -> showShortcuts());
//        resetFocus.setOnAction(e -> setupKeyboardFocus());
    }

    private void showShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("Here are the available keyboard shortcuts:");

        String shortcuts =
                "MOVE:\n" +
                        "  - 1-9, a-z: Move to configured folder.\n" +
                        "  - Shift + 1-9, a-z: Move to configured folder and archive.\n" +
                        "  - 0 or Numpad 0: Move to global archive folder.\n" +
                        "\n" +
                        "NAVIGATION:\n" +
                        "  - Right Arrow: Next image.\n" +
                        "  - Left Arrow: Previous image.\n" +
                        "  - Mouse Click: Left side for previous, right side for next.\n" +
                        "  - Mouse Scroll: Up for previous, down for next.\n" +
                        "\n" +
                        "ACTIONS:\n" +
                        "  - Ctrl + Z: Undo last action.\n" +
                        "  - Delete or -: Delete current image.\n" +
                        "  - Ctrl + R: Rotate image right.\n" +
                        "  - Ctrl + L: Rotate image left.\n" +
                        "  - Ctrl + Alt + O: Open image in external viewer.\n" +
                        "\n" +
                        "CONFIGURATION:\n" +
                        "  - Ctrl + Shift + 1-9, a-z: Choose on-demand folder.\n" +
                        "  - Ctrl + O: Open folder.\n" +
                        "  - Ctrl + P: Open configuration dialog.\n" +
                        "\n" +
                        "VIEW:\n" +
                        "  - F1: Show this help dialog.\n";

        alert.setContentText(shortcuts);
        alert.showAndWait();
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
        } else if (node == rightVBox) {
            if (horizontalSplitPane != null) {
                if (horizontalSplitPane.getItems().contains(rightVBox)) {
                    horizontalSplitPane.getItems().remove(rightVBox);
                } else {
                    horizontalSplitPane.getItems().add(rightVBox);
                }
            }
        } else if (node == thumbnailContainer || node == thumbnailBox) {
            if (verticalSplitPane != null) {
                if (verticalSplitPane.getItems().contains(thumbnailContainer)) {
                    verticalSplitPane.getItems().remove(thumbnailContainer);
                } else {
                    verticalSplitPane.getItems().add(0, thumbnailContainer);
                    verticalSplitPane.setDividerPositions(0.1);
                }
            }
        }
    }

    @FXML
    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        String keyText = code.getName().toLowerCase();
        boolean handled = true;

        if (event.isAltDown()) {
            if (code == KeyCode.DIGIT1 || code == KeyCode.NUMPAD1) { setRating(1); return; }
            if (code == KeyCode.DIGIT2 || code == KeyCode.NUMPAD2) { setRating(2); return; }
            if (code == KeyCode.DIGIT3 || code == KeyCode.NUMPAD3) { setRating(3); return; }
            if (code == KeyCode.DIGIT4 || code == KeyCode.NUMPAD4) { setRating(4); return; }
            if (code == KeyCode.DIGIT5 || code == KeyCode.NUMPAD5) { setRating(5); return; }
            if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) { setRating(0); return; }
            if (code == KeyCode.R) { setColorLabel("Red"); return; }
            if (code == KeyCode.Y) { setColorLabel("Yellow"); return; }
            if (code == KeyCode.G) { setColorLabel("Green"); return; }
            if (code == KeyCode.B) { setColorLabel("Blue"); return; }
            if (code == KeyCode.C) { setColorLabel(""); return; }
        }

        if (event.isControlDown() && code == KeyCode.Z) {
            undoLastAction();
        } else if (event.isControlDown() && event.isShiftDown() && keyText.matches("[1-9a-z]")) {
            chooseOnDemandFolder(event, keyText);
        } else if (event.isControlDown() && event.isAltDown() && code == KeyCode.O) {
            openInExternalViewer();
        } else if (currentImages == null || currentImages.isEmpty()) {
            handled = false;
        } else if (keyText.matches("[1-9a-z]") && !event.isControlDown()) {
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
                case MINUS:
                    deleteCurrentImage();
                    break;
                case NUMPAD0:
                case DIGIT0:
                    moveToArchive(currentSourceFolder);
                    break;
                case O:
                    if (event.isControlDown())
                        openFolder();
                    else
                        handled = false;
                    break;
                case P:
                    if (event.isControlDown())
                        openConfigDialog();
                    else
                        handled = false;
                    break;
                default:
                    handled = false;
                    break;
            }
        }
        if (handled) {
            event.consume();
        }
    }

    /**
     * TODO Not verified
     * @param rotateRight
     */
    private void rotateCurrentImage(boolean  rotateRight) {
        if (currentImages != null && currentImageIndex >= 0 && currentImageIndex < currentImages.size()) {
            ImageFile currentImageFile = currentImages.get(currentImageIndex);
            File fileToRotate = currentImageFile.getFile();
            if (fileToRotate.exists()) {
                try {
                    rotateExifOrientation(fileToRotate, rotateRight);
                    imageService.clearCache(currentImageFile);
                    currentImageFile.setExifRotate(null); // reset to force re-read
                    displayCurrentImage();
                } catch (Exception e) {
                    showAlert("Error", "Could not rotate image: " + e.getMessage());
                }
            } else {
                showAlert("Error", "Image file does not exist.");
            }
        }
    }

    private void openInExternalViewer() {
        if (currentImages != null && currentImageIndex >= 0 && currentImageIndex < currentImages.size()) {
            ImageFile currentImageFile = currentImages.get(currentImageIndex);
            File fileToOpen = currentImageFile.getFile();
            if (fileToOpen.exists()) {
                try {
                    Desktop.getDesktop().open(fileToOpen);
                } catch (IOException e) {
                    showAlert("Error", "Could not open image in external viewer: " + e.getMessage());
                }
            } else {
                showAlert("Error", "Image file does not exist.");
            }
        }
    }

    private void chooseOnDemandFolder(KeyEvent event, String keyText) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Folder for Hotkey " + keyText);
        Stage stage = (Stage) mediaContainer.getScene().getWindow();
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

    private void handleVideoClick(MouseEvent event) {
        // If clicking on the bottom controls area (MediaBar), don't trigger navigation
        if (event.getY() > mediaContainer.getHeight() - 50) {
            setupKeyboardFocus();
            return;
        }

        if (event.getClickCount() == 1 && clickToMoveCheckBox.isSelected()) {
            double clickX = event.getX();
            double centerX = mediaContainer.getWidth() / 2;

            if (clickX > centerX) {
                navigateNext();
            } else {
                navigatePrevious();
            }
        }
        setupKeyboardFocus();
    }

    private void openFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Image Folder");

        File selectedDirectory;
        if (currentSourceFolder != null) {
            directoryChooser.setInitialDirectory(currentSourceFolder);
        }
        selectedDirectory = directoryChooser.showDialog(mediaContainer.getScene().getWindow());


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

    private synchronized void displayCurrentImage() throws RuntimeException {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            imageView.setImage(null);
            return;
        }

        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        clearVideo();

        if(currentImageFile.isVideoFile()){
            System.out.println("Video file detected, skipping: "+ currentImageFile.getName());
            mediaContainer.getChildren().clear();
            Player videoPlayer;
            imageService.ensureExifRotation(currentImageFile);
            try {
                videoPlayer = new Player(currentImageFile);
                videoPlayer.setOnMouseClicked(this::handleVideoClick);
            } catch (Exception e) {
                throw  new RuntimeException(e);
            }


            mediaContainer.getChildren().clear();

            // Use your Player class
            mediaContainer.getChildren().add(videoPlayer);

            // Keep reference for stopping later
            currentMediaPlayer = videoPlayer;
            currentMediaPlayer.play();

            // Refresh metadata details once the media player loads dimensions/duration
            currentMediaPlayer.player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == javafx.scene.media.MediaPlayer.Status.READY) {
                    Platform.runLater(this::updateMetadataPanel);
                }
            });

        } else {
            mediaContainer.getChildren().clear();
            mediaContainer.getChildren().add(imageView);

            // Cancel any running image load task
            if (currentImageLoadTask != null && currentImageLoadTask.isRunning()) {
                currentImageLoadTask.cancel();
            }

            Image image = imageService.getCachedImage(currentImageFile);

            if (image != null) {
                imageView.setImage(image);
            } else {
                System.out.println("cache miss" + currentImageFile.getName());
                imageView.setImage(null);
                currentImageLoadTask = imageService.createLoadImageTask(currentImageFile);
                currentImageLoadTask.setOnSucceeded(e -> {
                    Image img = currentImageLoadTask.getValue();
                    if (img != null) {
                        imageView.setImage(img);
                        updateMetadataPanel();
                        mediaContainer.requestLayout();
                        imageScrollPane.requestLayout();
                    }
                });
                currentImageLoadTask.setOnFailed(e -> {
                    Throwable ex = currentImageLoadTask.getException();
                    System.err.println("Failed to load image: " + currentImageFile.getName() + " - " + (ex != null ? ex.getMessage() : ""));
                });
                imageService.submitTask(currentImageLoadTask);
            }
        }
        updateStatusBar();
        updateThumbnails();
        updateMetadataPanel();
        displayCustomMetadata();

        // Pre-cache surrounding images

        imageService.preCacheImages(currentImages, currentImageIndex, configService.getConfig().getPrevCache(), configService.getConfig().getNextCache(), progressUpdaterCallback);

    }

    private void updateThumbnails() {
        if (currentImages == null || currentImages.isEmpty()) {
            thumbnailBox.getChildren().clear();
            lastThumbnailStateKey = "";
            return;
        }

        double width = thumbnailBox.getWidth();
        double height = thumbnailBox.getHeight();
        double thumbSize;

        if (height <= 0) {
            thumbSize = configService.getConfig().getThumbnailSize();
        } else {
            thumbSize = Math.max(40.0, height - 10.0);
        }

        int targetCapacity;
        if (width <= 0) {
            targetCapacity = configService.getConfig().getThumbnailCount();
        } else {
            double spacing = 5.0; // Spacing configured in main.fxml HBox spacing="5.0"
            double selectedSize = thumbSize;
            double otherSize = thumbSize * 0.7;
            double margin = 20.0; // Minimal safety margin since thumbnailBox doesn't include the pin button
            double availableWidth = width - margin;
            
            // selectedSize + (K - 1) * (otherSize + spacing) <= availableWidth
            targetCapacity = 1 + Math.max(0, (int) ((availableWidth - selectedSize) / (otherSize + spacing)));
        }

        // De-duplicate layout updates to prevent flickering and performance overhead
        String stateKey = currentSourceFolder + "_" + currentImageIndex + "_" + targetCapacity + "_" + (int) thumbSize + "_" + currentImages.size();
        if (stateKey.equals(lastThumbnailStateKey)) {
            return;
        }
        lastThumbnailStateKey = stateKey;

        thumbnailBox.getChildren().clear();

        int totalImages = currentImages.size();
        int targetSize = Math.min(targetCapacity, totalImages);
        
        // Sliding window calculation to fill full width at boundaries
        int startIndex = currentImageIndex - (targetSize - 1) / 2;
        if (startIndex < 0) {
            startIndex = 0;
        }
        int endIndex = startIndex + targetSize - 1;
        if (endIndex >= totalImages) {
            endIndex = totalImages - 1;
            startIndex = Math.max(0, endIndex - targetSize + 1);
        }

        for (int i = startIndex; i <= endIndex; i++) {
            ImageFile imageFile = currentImages.get(i);

            ImageView thumbnail = new ImageView();
            thumbnail.setPreserveRatio(true);
            thumbnail.getStyleClass().add("thumbnail-image");
            double thumbBoxheight = thumbSize;
            if (i == currentImageIndex) {
                thumbnail.getStyleClass().add("thumbnail-selected");
            } else {
                thumbBoxheight = thumbBoxheight * 0.7;
            }
            thumbnail.setFitHeight(thumbBoxheight);
            thumbnail.setFitWidth(thumbBoxheight);
            
            // Navigate to image on click
            final int targetIndex = i;
            thumbnail.setOnMouseClicked(event -> {
                currentImageIndex = targetIndex;
                displayCurrentImage();
                setupKeyboardFocus();
                event.consume();
            });
            thumbnail.setCursor(javafx.scene.Cursor.HAND);

            thumbnailBox.getChildren().add(thumbnail);

            Image cachedThumb = imageService.getCachedThumbnail(imageFile);
            if (cachedThumb != null) {
                thumbnail.setImage(cachedThumb);
            } else {
                // Set initial placeholder if video
                if (imageFile.isVideoFile()) {
                    try (java.io.InputStream fis = FastVideoThumbnailUtil.class.getResourceAsStream("/video.png")) {
                        if (fis != null) {
                            thumbnail.setImage(new Image(fis, 200, 0, true, false));
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }

                // Load thumbnail asynchronously
                final ImageView finalThumbnail = thumbnail;
                Task<Image> loadThumbTask = new Task<>() {
                    @Override
                    protected Image call() throws Exception {
                        return imageService.loadThumbnail(imageFile);
                    }
                };
                loadThumbTask.setOnSucceeded(e -> {
                    Image thumbImg = loadThumbTask.getValue();
                    if (thumbImg != null) {
                        finalThumbnail.setImage(thumbImg);
                        thumbnailBox.requestLayout();
                    }
                });
                loadThumbTask.setOnFailed(e -> {
                    System.err.println("Failed to load thumbnail for: " + imageFile.getName());
                });
                imageService.submitThumbnailTask(loadThumbTask);
            }
        }
    }

    private void updateMetadataPanel() {
        if (metadataGridPane == null) return;
        metadataGridPane.getChildren().clear();
        if (currentImages == null || currentImages.isEmpty() || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            return;
        }

        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        File file = currentImageFile.getFile();

        // Get metadata map from cache or load it
        Map<String, String> cachedMetadata = currentImageFile.getMetadataMap();
        Map<String, String> metadata;
        if (cachedMetadata != null) {
            metadata = new LinkedHashMap<>(cachedMetadata);
        } else {
            cachedMetadata = ImageUtils.getMetadataMap(file);
            currentImageFile.setMetadataMap(cachedMetadata);
            metadata = new LinkedHashMap<>(cachedMetadata);
        }

        // Add dimensions if we have loaded the image/video
        if (!currentImageFile.isVideoFile()) {
            Image image = imageView.getImage();
            if (image != null && !image.isError()) {
                metadata.put("Dimensions", String.format("%.0f x %.0f", image.getWidth(), image.getHeight()));
            }
        } else if (currentMediaPlayer != null && currentMediaPlayer.player != null) {
            javafx.scene.media.Media media = currentMediaPlayer.player.getMedia();
            if (media != null) {
                // Dimensions might only be non-zero after ready state
                if (media.getWidth() > 0 && media.getHeight() > 0) {
                    metadata.put("Dimensions", String.format("%d x %d", media.getWidth(), media.getHeight()));
                }
                if (media.getDuration() != null && media.getDuration().greaterThan(javafx.util.Duration.ZERO)) {
                    double seconds = media.getDuration().toSeconds();
                    metadata.put("Duration", String.format("%.1f s", seconds));
                }
            }
        }

        int row = 0;
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            Label keyLabel = new Label(entry.getKey() + ":");
            keyLabel.getStyleClass().add("metadata-key");

            Label valueLabel = new Label(entry.getValue());
            valueLabel.setWrapText(true);
            valueLabel.getStyleClass().add("metadata-value");

            metadataGridPane.add(keyLabel, 0, row);
            metadataGridPane.add(valueLabel, 1, row);
            row++;
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

        // Intercept for Staging Mode
        String mode = configService.getConfig().getActionMode();
        if ("STAGED_MOVE".equals(mode) || "STAGED_COPY".equals(mode)) {
            StagedAction.Type actionType = "STAGED_MOVE".equals(mode) ? StagedAction.Type.MOVE : StagedAction.Type.COPY;
            StagedAction stagedAction = new StagedAction(currentImageFile, sourceFile, destinationFolder, actionType, currentImageIndex);
            stagedActions.add(stagedAction);
            updateStagedCount();
            
            lastAction.setText("Staged [" + (actionType == StagedAction.Type.MOVE ? "Move" : "Copy") + "] to " + destinationFolder.getName());
            
            clearVideo();
            currentImages.remove(currentImageIndex);

            if (currentImageIndex >= currentImages.size() && !currentImages.isEmpty()) {
                currentImageIndex = currentImages.size() - 1;
            }

            if (autoAdvanceCheckBox.isSelected()) {
                if (!currentImages.isEmpty()) {
                    displayCurrentImage();
                } else {
                    imageView.setImage(null);
                    updateStatusBar();
                    showAlert("Complete", "All images sorted/staged!");
                }
            } else {
                imageView.setImage(null);
                clearVideo();
                currentFileLabel.setText("File staged. Press Arrow keys to navigate.");
            }
            return;
        }

        boolean isCopyMode = "COPY".equals(mode);
        File destinationFile = new File(destinationFolder, sourceFile.getName());

        int counter = 1;
        while (destinationFile.exists()) {
            String name = sourceFile.getName();
            String baseName = name.substring(0, name.lastIndexOf('.'));
            String extension = name.substring(name.lastIndexOf('.'));
            destinationFile = new File(destinationFolder, baseName + "_" + counter + extension);
            counter++;
        }

        if (!isCopyMode) {
            addLastAction(new LastAction(LastAction.ActionType.MOVE, sourceFile, destinationFile));
        }

        clearVideo();
        boolean success = false;
        if (isCopyMode) {
            try {
                java.nio.file.Files.copy(sourceFile.toPath(), destinationFile.toPath(), 
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                success = true;
                lastAction.setText("Last Action: [Copied] " + sourceFile.getName() +" -> " + destinationFolder.getPath());
            } catch (Exception e) {
                showAlert("Error", "Failed to copy file: " + e.getMessage());
            }
        } else {
            success = sourceFile.renameTo(destinationFile);
            if (success) {
                lastAction.setText("Last Action: [Moved] " + sourceFile.getName() +" -> " + destinationFolder.getPath());
                System.out.println("Moved: " + sourceFile.getAbsolutePath() + " -> " + destinationFile.getAbsolutePath());
                configService.updateMetadataPath(sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath());
            }
        }

        if (success) {
            currentImages.remove(currentImageIndex);

            if (currentImageIndex >= currentImages.size() && !currentImages.isEmpty()) {
                currentImageIndex = currentImages.size() - 1;
            }

            if (autoAdvanceCheckBox.isSelected()) {
                if (!currentImages.isEmpty()) {
                    displayCurrentImage();
                } else {
                    imageView.setImage(null);
                    updateStatusBar();
                    showAlert("Complete", "All images have been sorted!");
                }
            } else {
                imageView.setImage(null);
                clearVideo();
                currentFileLabel.setText(isCopyMode ? "File copied. Press Arrow keys to navigate." : "File moved. Press Arrow keys to navigate.");
            }
        } else {
            if (!isCopyMode) {
                showAlert("Error", "Failed to move file to: " + destinationFolder.getPath());
            }
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

            clearVideo();
            if (sourceFile.renameTo(destinationFile)) {
                lastAction.setText("Last Action: [Deleted] " + currentImageFile.getName());
                System.out.println("Last Action: [Deleted] "+currentImageFile.getFile().getAbsolutePath());
                configService.updateMetadataPath(sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath());
                // Remove from list
                currentImages.remove(currentImageIndex);

                // Adjust index
                if (currentImageIndex >= currentImages.size() && !currentImages.isEmpty()) {
                    currentImageIndex = currentImages.size() - 1;
                }

                if (autoAdvanceCheckBox.isSelected()) {
                    // Display next image
                    if (!currentImages.isEmpty()) {
                        displayCurrentImage();
                    } else {
                        imageView.setImage(null);
                        updateStatusBar();
                    }
                } else {
                    imageView.setImage(null);
                    clearVideo();
                    currentFileLabel.setText("File deleted. Press Arrow keys to navigate.");
                }
            } else {
                showAlert("Error", "Failed to move the image to the trash folder.");
            }
        }
    }

    private void clearVideo() {
        if(currentMediaPlayer!= null){
            currentMediaPlayer.dispose();
            currentMediaPlayer = null;
        }
    }

    private void addLastAction(LastAction action) {
        if (lastActionInfo.size() >= configService.getConfig().getUndoSize()) {
            lastActionInfo.removeLast();
        }
        lastActionInfo.addFirst(action);
    }

    private void undoLastAction() {
        if (!stagedActions.isEmpty()) {
            StagedAction lastStaged = stagedActions.remove(stagedActions.size() - 1);
            updateStagedCount();
            
            if (currentImages != null) {
                int index = Math.min(lastStaged.getOriginalIndex(), currentImages.size());
                currentImages.add(index, lastStaged.getImageFile());
                currentImageIndex = index;
                try {
                    displayCurrentImage();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                lastAction.setText("Last Action: [Undo Staged] " + lastStaged.getSourceFile().getName());
            }
            return;
        }

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
            if (mediaContainer.getScene() != null && mediaContainer.getScene().getWindow() != null) {
                configStage.initOwner(mediaContainer.getScene().getWindow());
            }

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
            String text = String.format("Current File: %s, Size: %s", currentImageFile.getName(), humanReadableByteCountSI(currentImageFile.getFile().length()));
            currentFileLabel.setText(text);

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

    private void setupNewFeatures() {
        ConfigSettings config = configService.getConfig();

        // 1. Setup Bookmarks Menu
        refreshBookmarksMenu();
        addBookmarkMenuItem.setOnAction(e -> {
            if (currentSourceFolder != null) {
                String path = currentSourceFolder.getAbsolutePath();
                List<String> bookmarks = config.getBookmarkedFolders();
                if (!bookmarks.contains(path)) {
                    bookmarks.add(path);
                    configService.saveConfig();
                    refreshBookmarksMenu();
                    lastAction.setText("Added bookmark: " + currentSourceFolder.getName());
                }
            } else {
                showAlert("Info", "Please open a folder first to add it to bookmarks.");
            }
        });
        removeBookmarkMenuItem.setOnAction(e -> {
            List<String> bookmarks = config.getBookmarkedFolders();
            if (bookmarks == null || bookmarks.isEmpty()) {
                showAlert("Info", "No bookmarks to remove.");
                return;
            }
            ChoiceDialog<String> dialog = new ChoiceDialog<>(bookmarks.get(0), bookmarks);
            dialog.setTitle("Remove Bookmark");
            dialog.setHeaderText("Select a bookmark to remove:");
            dialog.setContentText("Bookmark:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(selected -> {
                bookmarks.remove(selected);
                configService.saveConfig();
                refreshBookmarksMenu();
                lastAction.setText("Removed bookmark: " + selected);
            });
        });

        // 2. Setup Action Mode Menu Items
        ToggleGroup actionModeGroup = new ToggleGroup();
        actionModeMoveMenuItem.setToggleGroup(actionModeGroup);
        actionModeCopyMenuItem.setToggleGroup(actionModeGroup);
        actionModeStagedMoveMenuItem.setToggleGroup(actionModeGroup);
        actionModeStagedCopyMenuItem.setToggleGroup(actionModeGroup);

        String currentMode = config.getActionMode();
        if ("COPY".equals(currentMode)) {
            actionModeCopyMenuItem.setSelected(true);
        } else if ("STAGED_MOVE".equals(currentMode)) {
            actionModeStagedMoveMenuItem.setSelected(true);
        } else if ("STAGED_COPY".equals(currentMode)) {
            actionModeStagedCopyMenuItem.setSelected(true);
        } else {
            actionModeMoveMenuItem.setSelected(true);
        }

        actionModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String configMode = "MOVE";
                boolean isStaged = false;
                if (newVal == actionModeCopyMenuItem) {
                    configMode = "COPY";
                } else if (newVal == actionModeStagedMoveMenuItem) {
                    configMode = "STAGED_MOVE";
                    isStaged = true;
                } else if (newVal == actionModeStagedCopyMenuItem) {
                    configMode = "STAGED_COPY";
                    isStaged = true;
                }
                config.setActionMode(configMode);
                configService.saveConfig();

                batchControlsBox.setVisible(isStaged);
                batchControlsBox.setManaged(isStaged);
                updateStagedCount();
            }
        });
        
        boolean isStagedInit = "STAGED_MOVE".equals(currentMode) || "STAGED_COPY".equals(currentMode);
        batchControlsBox.setVisible(isStagedInit);
        batchControlsBox.setManaged(isStagedInit);

        commitBatchButton.setOnAction(e -> commitBatch());
        clearBatchButton.setOnAction(e -> clearBatch());

        // 3. Setup Slideshow Menu Items
        slideshowPlayMenuItem.setOnAction(e -> {
            if (slideshowPlayMenuItem.isSelected()) {
                startSlideshow();
            } else {
                stopSlideshow();
            }
        });

        ToggleGroup intervalGroup = new ToggleGroup();
        interval1sMenuItem.setToggleGroup(intervalGroup);
        interval2sMenuItem.setToggleGroup(intervalGroup);
        interval3sMenuItem.setToggleGroup(intervalGroup);
        interval5sMenuItem.setToggleGroup(intervalGroup);
        interval10sMenuItem.setToggleGroup(intervalGroup);
        intervalCustomMenuItem.setToggleGroup(intervalGroup);

        intervalGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if (newVal == interval1sMenuItem) {
                    slideshowInterval = 1;
                } else if (newVal == interval2sMenuItem) {
                    slideshowInterval = 2;
                } else if (newVal == interval3sMenuItem) {
                    slideshowInterval = 3;
                } else if (newVal == interval5sMenuItem) {
                    slideshowInterval = 5;
                } else if (newVal == interval10sMenuItem) {
                    slideshowInterval = 10;
                } else if (newVal == intervalCustomMenuItem) {
                    TextInputDialog dialog = new TextInputDialog(String.valueOf(slideshowInterval));
                    dialog.setTitle("Custom Slideshow Interval");
                    dialog.setHeaderText("Set custom slideshow interval in seconds:");
                    dialog.setContentText("Interval (seconds):");
                    Optional<String> result = dialog.showAndWait();
                    if (result.isPresent()) {
                        try {
                            int customVal = Integer.parseInt(result.get());
                            if (customVal > 0) {
                                slideshowInterval = customVal;
                                intervalCustomMenuItem.setText("Custom (" + customVal + "s)");
                            } else {
                                showAlert("Error", "Interval must be greater than 0.");
                                interval3sMenuItem.setSelected(true);
                            }
                        } catch (NumberFormatException nfe) {
                            showAlert("Error", "Invalid number format.");
                            interval3sMenuItem.setSelected(true);
                        }
                    } else {
                        interval3sMenuItem.setSelected(true);
                    }
                }
                
                if (slideshowPlayMenuItem.isSelected()) {
                    startSlideshow();
                }
            }
        });

        toggleFullScreenMenuItem.setOnAction(e -> toggleFullScreen());

        // 4. Setup Theme Menu Items
        ToggleGroup themeGroup = new ToggleGroup();
        themePrimerLightMenuItem.setToggleGroup(themeGroup);
        themePrimerDarkMenuItem.setToggleGroup(themeGroup);
        themeNordLightMenuItem.setToggleGroup(themeGroup);
        themeNordDarkMenuItem.setToggleGroup(themeGroup);
        themeCupertinoLightMenuItem.setToggleGroup(themeGroup);
        themeCupertinoDarkMenuItem.setToggleGroup(themeGroup);
        themeDraculaMenuItem.setToggleGroup(themeGroup);

        String activeTheme = config.getTheme();
        switch (activeTheme) {
            case "Primer Dark":
                themePrimerDarkMenuItem.setSelected(true);
                break;
            case "Nord Light":
                themeNordLightMenuItem.setSelected(true);
                break;
            case "Nord Dark":
                themeNordDarkMenuItem.setSelected(true);
                break;
            case "Cupertino Light":
                themeCupertinoLightMenuItem.setSelected(true);
                break;
            case "Cupertino Dark":
                themeCupertinoDarkMenuItem.setSelected(true);
                break;
            case "Dracula":
                themeDraculaMenuItem.setSelected(true);
                break;
            case "Primer Light":
            default:
                themePrimerLightMenuItem.setSelected(true);
                break;
        }

        themeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String selectedTheme = "Primer Light";
                if (newVal == themePrimerDarkMenuItem) {
                    selectedTheme = "Primer Dark";
                } else if (newVal == themeNordLightMenuItem) {
                    selectedTheme = "Nord Light";
                } else if (newVal == themeNordDarkMenuItem) {
                    selectedTheme = "Nord Dark";
                } else if (newVal == themeCupertinoLightMenuItem) {
                    selectedTheme = "Cupertino Light";
                } else if (newVal == themeCupertinoDarkMenuItem) {
                    selectedTheme = "Cupertino Dark";
                } else if (newVal == themeDraculaMenuItem) {
                    selectedTheme = "Dracula";
                }
                config.setTheme(selectedTheme);
                configService.saveConfig();
                com.imagesorter.ImageSorterApp.setAppTheme(selectedTheme);
                lastAction.setText("Changed theme to: " + selectedTheme);
            }
        });

        // 5. Setup Sorting Menu Items
        ToggleGroup sortFieldGroup = new ToggleGroup();
        sortByNameMenuItem.setToggleGroup(sortFieldGroup);
        sortByCreatedMenuItem.setToggleGroup(sortFieldGroup);
        sortByModifiedMenuItem.setToggleGroup(sortFieldGroup);
        sortBySizeMenuItem.setToggleGroup(sortFieldGroup);
        sortByRatingMenuItem.setToggleGroup(sortFieldGroup);
        sortByColorLabelMenuItem.setToggleGroup(sortFieldGroup);

        ToggleGroup sortOrderGroup = new ToggleGroup();
        sortOrderAscMenuItem.setToggleGroup(sortOrderGroup);
        sortOrderDescMenuItem.setToggleGroup(sortOrderGroup);

        // Initialize UI from config
        syncSortingUI();

        // Listeners for Menu Items
        sortFieldGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSortingUI || newVal == null) return;
            String field = "Name";
            if (newVal == sortByCreatedMenuItem) {
                field = "Date Created";
            } else if (newVal == sortByModifiedMenuItem) {
                field = "Date Modified";
            } else if (newVal == sortBySizeMenuItem) {
                field = "Size";
            } else if (newVal == sortByRatingMenuItem) {
                field = "Rating";
            } else if (newVal == sortByColorLabelMenuItem) {
                field = "Color Label";
            }
            config.setSortField(field);
            configService.saveConfig();
            syncSortingUI();
            sortCurrentImages();
            lastAction.setText("Sorting changed to: " + field);
        });

        sortOrderGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSortingUI || newVal == null) return;
            String order = "Ascending";
            if (newVal == sortOrderDescMenuItem) {
                order = "Descending";
            }
            config.setSortOrder(order);
            configService.saveConfig();
            syncSortingUI();
            sortCurrentImages();
            lastAction.setText("Sort order changed to: " + order);
        });
    }

    private void syncSortingUI() {
        if (isUpdatingSortingUI) return;
        isUpdatingSortingUI = true;
        try {
            String activeField = configService.getConfig().getSortField();
            String activeOrder = configService.getConfig().getSortOrder();

            // Sync menu items
            switch (activeField) {
                case "Date Created":
                    sortByCreatedMenuItem.setSelected(true);
                    break;
                case "Date Modified":
                    sortByModifiedMenuItem.setSelected(true);
                    break;
                case "Size":
                    sortBySizeMenuItem.setSelected(true);
                    break;
                case "Rating":
                    sortByRatingMenuItem.setSelected(true);
                    break;
                case "Color Label":
                    sortByColorLabelMenuItem.setSelected(true);
                    break;
                case "Name":
                default:
                    sortByNameMenuItem.setSelected(true);
                    break;
            }

            if ("Descending".equals(activeOrder)) {
                sortOrderDescMenuItem.setSelected(true);
            } else {
                sortOrderAscMenuItem.setSelected(true);
            }
        } finally {
            isUpdatingSortingUI = false;
        }
    }

    private void refreshBookmarksMenu() {
        bookmarksMenu.getItems().clear();
        bookmarksMenu.getItems().addAll(addBookmarkMenuItem, removeBookmarkMenuItem, bookmarksSeparator);
        
        List<String> bookmarks = configService.getConfig().getBookmarkedFolders();
        if (bookmarks == null || bookmarks.isEmpty()) {
            MenuItem emptyItem = new MenuItem("No Bookmarks Saved");
            emptyItem.setDisable(true);
            bookmarksMenu.getItems().add(emptyItem);
        } else {
            for (String path : bookmarks) {
                File file = new File(path);
                MenuItem item = new MenuItem(file.getName() + " (" + path + ")");
                item.setOnAction(e -> {
                    currentImageIndex = 0;
                    loadImagesFromFolder(file);
                });
                bookmarksMenu.getItems().add(item);
            }
        }
    }

    private void startSlideshow() {
        if (slideshowTimeline != null) {
            slideshowTimeline.stop();
        }
        slideshowTimeline = new Timeline(new KeyFrame(Duration.seconds(slideshowInterval), e -> {
            if (currentImages != null && !currentImages.isEmpty()) {
                if (currentImageIndex < currentImages.size() - 1) {
                    currentImageIndex++;
                } else {
                    currentImageIndex = 0;
                }
                displayCurrentImage();
            }
        }));
        slideshowTimeline.setCycleCount(Timeline.INDEFINITE);
        slideshowTimeline.play();
        slideshowPlayMenuItem.setSelected(true);
        lastAction.setText("Slideshow started with interval: " + slideshowInterval + "s");
    }

    private void stopSlideshow() {
        if (slideshowTimeline != null) {
            slideshowTimeline.stop();
            slideshowTimeline = null;
        }
        slideshowPlayMenuItem.setSelected(false);
        lastAction.setText("Slideshow paused.");
    }

    private void updateStagedCount() {
        if (stagedCountLabel != null) {
            stagedCountLabel.setText("Staged: " + stagedActions.size());
        }
    }

    private void clearBatch() {
        if (stagedActions.isEmpty()) return;
        
        stagedActions.sort((a, b) -> Integer.compare(a.getOriginalIndex(), b.getOriginalIndex()));
        for (StagedAction action : stagedActions) {
            if (!currentImages.contains(action.getImageFile())) {
                currentImages.add(Math.min(action.getOriginalIndex(), currentImages.size()), action.getImageFile());
            }
        }
        stagedActions.clear();
        updateStagedCount();
        displayCurrentImage();
        lastAction.setText("Cleared staged batch queue.");
    }

    private void commitBatch() {
        if (stagedActions.isEmpty()) {
            showAlert("Info", "No staged actions to commit.");
            return;
        }

        progressBar.setVisible(true);
        progressBar.setProgress(0);
        
        List<StagedAction> actionsToProcess = new ArrayList<>(stagedActions);
        stagedActions.clear();
        updateStagedCount();

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            int total = actionsToProcess.size();
            int successCount = 0;
            
            for (int i = 0; i < total; i++) {
                StagedAction action = actionsToProcess.get(i);
                final double progress = (double) (i + 1) / total;
                
                boolean success = false;
                try {
                    File source = action.getSourceFile();
                    File destDir = action.getDestinationFolder();
                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }
                    
                    File destinationFile = new File(destDir, source.getName());
                    int counter = 1;
                    while (destinationFile.exists()) {
                        String name = source.getName();
                        int dotIdx = name.lastIndexOf('.');
                        String baseName = dotIdx > 0 ? name.substring(0, dotIdx) : name;
                        String extension = dotIdx > 0 ? name.substring(dotIdx) : "";
                        destinationFile = new File(destDir, baseName + "_" + counter + extension);
                        counter++;
                    }

                    if (action.getType() == StagedAction.Type.MOVE) {
                        success = source.renameTo(destinationFile);
                    } else {
                        java.nio.file.Files.copy(source.toPath(), destinationFile.toPath(), 
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        success = true;
                    }
                } catch (Exception e) {
                    System.err.println("Failed staged action: " + e.getMessage());
                }

                if (success) {
                    successCount++;
                }
                
                Platform.runLater(() -> {
                    progressBar.setProgress(progress);
                });
            }

            final int finalSuccess = successCount;
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                progressBar.setProgress(0);
                showAlert("Batch Completed", "Successfully processed " + finalSuccess + " of " + total + " actions.");
                loadImagesFromFolder(currentSourceFolder);
            });
        });
        executor.shutdown();
    }

    private void toggleFullScreen() {
        Scene scene = mediaContainer.getScene();
        if (scene != null) {
            Stage stage = (Stage) scene.getWindow();
            if (stage != null) {
                stage.setFullScreen(!stage.isFullScreen());
            }
        }
    }

    private void setupFullScreenListener() {
        if (isFullScreenListenerRegistered) return;
        Scene scene = mediaContainer.getScene();
        if (scene == null) {
            mediaContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setupFullScreenListener();
                }
            });
            return;
        }
        
        Stage stage = (Stage) scene.getWindow();
        if (stage == null) {
            scene.windowProperty().addListener((obs, oldWin, newWin) -> {
                if (newWin != null) {
                    setupFullScreenListener();
                }
            });
            return;
        }
        
        isFullScreenListenerRegistered = true;
        
        stage.fullScreenProperty().addListener((obs, wasFS, isFS) -> {
            if (isFS) {
                wasLeftVisibleBeforeFS = horizontalSplitPane.getItems().contains(leftVBox);
                wasRightVisibleBeforeFS = horizontalSplitPane.getItems().contains(rightVBox);
                wasThumbnailVisibleBeforeFS = verticalSplitPane.getItems().contains(thumbnailContainer);
                
                if (wasLeftVisibleBeforeFS && (pinLeftButton == null || !pinLeftButton.isSelected())) {
                    horizontalSplitPane.getItems().remove(leftVBox);
                }
                if (wasRightVisibleBeforeFS && (pinRightButton == null || !pinRightButton.isSelected())) {
                    horizontalSplitPane.getItems().remove(rightVBox);
                }
                if (wasThumbnailVisibleBeforeFS && (pinThumbnailButton == null || !pinThumbnailButton.isSelected())) {
                    verticalSplitPane.getItems().remove(thumbnailContainer);
                }
                lastAction.setText("Entered Full Screen. Move mouse to screen edges to reveal panels.");
            } else {
                if (wasLeftVisibleBeforeFS && !horizontalSplitPane.getItems().contains(leftVBox)) {
                    horizontalSplitPane.getItems().add(0, leftVBox);
                }
                if (wasRightVisibleBeforeFS && !horizontalSplitPane.getItems().contains(rightVBox)) {
                    horizontalSplitPane.getItems().add(rightVBox);
                }
                if (wasThumbnailVisibleBeforeFS && !verticalSplitPane.getItems().contains(thumbnailContainer)) {
                    verticalSplitPane.getItems().add(0, thumbnailContainer);
                }
                
                // Restore split pane divider positions based on which panels are visible
                int count = horizontalSplitPane.getItems().size();
                if (count == 3) {
                    horizontalSplitPane.setDividerPositions(0.25, 0.75);
                } else if (count == 2) {
                    if (horizontalSplitPane.getItems().contains(leftVBox)) {
                        horizontalSplitPane.setDividerPosition(0, 0.25);
                    } else {
                        horizontalSplitPane.setDividerPosition(0, 0.75);
                    }
                }
                
                if (verticalSplitPane.getItems().contains(thumbnailContainer)) {
                    verticalSplitPane.setDividerPosition(0, 0.1);
                }
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (stage.isFullScreen()) {
                double mouseX = event.getSceneX();
                double mouseY = event.getSceneY();
                double width = scene.getWidth();
                
                // Fallback widths / heights to handle layout delay
                double leftWidth = leftVBox.getWidth() > 0 ? leftVBox.getWidth() : leftVBox.getPrefWidth();
                double rightWidth = rightVBox.getWidth() > 0 ? rightVBox.getWidth() : rightVBox.getPrefWidth();
                double thumbnailHeight = thumbnailContainer.getHeight() > 0 ? thumbnailContainer.getHeight() : 100.0;

                // 1. Left view hover reveal / auto-hide
                boolean isLeftShowing = horizontalSplitPane.getItems().contains(leftVBox);
                boolean isLeftPinned = pinLeftButton != null && pinLeftButton.isSelected();
                if (!isLeftShowing && mouseX <= 15) {
                    horizontalSplitPane.getItems().add(0, leftVBox);
                    if (horizontalSplitPane.getItems().contains(rightVBox)) {
                        horizontalSplitPane.setDividerPositions(0.2, 0.8);
                    } else {
                        horizontalSplitPane.setDividerPosition(0, 0.2);
                    }
                } else if (isLeftShowing && !isLeftPinned && mouseX > leftWidth + 20) {
                    horizontalSplitPane.getItems().remove(leftVBox);
                }
                
                // 2. Right view hover reveal / auto-hide
                boolean isRightShowing = horizontalSplitPane.getItems().contains(rightVBox);
                boolean isRightPinned = pinRightButton != null && pinRightButton.isSelected();
                if (!isRightShowing && mouseX >= width - 15) {
                    horizontalSplitPane.getItems().add(rightVBox);
                    if (horizontalSplitPane.getItems().contains(leftVBox)) {
                        horizontalSplitPane.setDividerPositions(0.2, 0.8);
                    } else {
                        horizontalSplitPane.setDividerPosition(0, 0.8);
                    }
                } else if (isRightShowing && !isRightPinned && mouseX < width - rightWidth - 20) {
                    horizontalSplitPane.getItems().remove(rightVBox);
                }

                // 3. Top thumbnail box hover reveal / auto-hide
                boolean isThumbnailShowing = verticalSplitPane.getItems().contains(thumbnailContainer);
                boolean isThumbnailPinned = pinThumbnailButton != null && pinThumbnailButton.isSelected();
                if (!isThumbnailShowing && mouseY <= 15) {
                    verticalSplitPane.getItems().add(0, thumbnailContainer);
                    verticalSplitPane.setDividerPosition(0, 0.15);
                } else if (isThumbnailShowing && !isThumbnailPinned && mouseY > thumbnailHeight + 20) {
                    verticalSplitPane.getItems().remove(thumbnailContainer);
                }
            }
        });
    }

    private void openRulesDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/imagesorter/view/rules.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Configure Rules");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(saveMetaButton.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();
            setupKeyboardFocus();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load Rules dialog: " + e.getMessage());
        }
    }

    private void applyRulesToFolder() {
        if (currentImages == null || currentImages.isEmpty()) {
            showAlert("Info", "No images loaded in the folder to sort.");
            return;
        }

        List<SortingRule> rules = configService.getConfig().getSortingRules();
        if (rules == null || rules.isEmpty()) {
            showAlert("Info", "No rules configured. Please configure sorting rules first.");
            return;
        }

        int count = 0;
        List<ImageFile> imagesCopy = new ArrayList<>(currentImages);

        for (ImageFile imgFile : imagesCopy) {
            File file = imgFile.getFile();
            String path = file.getAbsolutePath();
            
            int rating = configService.getConfig().getRatings().getOrDefault(path, 0);
            String label = configService.getConfig().getColorLabels().getOrDefault(path, "");
            
            String camera = "";
            try {
                com.drew.metadata.Metadata metadata = com.drew.imaging.ImageMetadataReader.readMetadata(file);
                com.drew.metadata.exif.ExifIFD0Directory ifd0Dir = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifIFD0Directory.class);
                if (ifd0Dir != null) {
                    String make = ifd0Dir.getString(com.drew.metadata.exif.ExifIFD0Directory.TAG_MAKE);
                    String model = ifd0Dir.getString(com.drew.metadata.exif.ExifIFD0Directory.TAG_MODEL);
                    camera = ((make != null ? make.trim() : "") + " " + (model != null ? model.trim() : "")).trim();
                }
            } catch (Exception ignored) {}

            for (SortingRule rule : rules) {
                if (rule.matches(path, file.getName(), rating, label, camera)) {
                    File destDir = new File(rule.getDestinationFolder());
                    if (!destDir.exists()) {
                        destDir.mkdirs();
                    }
                    
                    File destFile = new File(destDir, file.getName());
                    int counter = 1;
                    while (destFile.exists()) {
                        String name = file.getName();
                        int lastDot = name.lastIndexOf('.');
                        String base = lastDot >= 0 ? name.substring(0, lastDot) : name;
                        String ext = lastDot >= 0 ? name.substring(lastDot) : "";
                        destFile = new File(destDir, base + "_" + counter + ext);
                        counter++;
                    }

                    boolean isCopy = rule.getAction() == SortingRule.Action.COPY;
                    boolean success = false;

                    try {
                        if (isCopy) {
                            java.nio.file.Files.copy(file.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            success = true;
                        } else {
                            success = file.renameTo(destFile);
                            if (success) {
                                configService.updateMetadataPath(path, destFile.getAbsolutePath());
                                currentImages.remove(imgFile);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Rule execution failed for: " + file.getName() + " - " + e.getMessage());
                    }

                    if (success) {
                        count++;
                        break;
                    }
                }
            }
        }

        configService.saveConfig();
        showAlert("Rules Applied", "Successfully sorted " + count + " files according to active rules.");
        
        if (currentImages.isEmpty()) {
            imageView.setImage(null);
            clearVideo();
            currentImageIndex = -1;
        } else {
            if (currentImageIndex >= currentImages.size()) {
                currentImageIndex = currentImages.size() - 1;
            }
            displayCurrentImage();
        }
        updateHotkeyList();
        updateThumbnails();
    }

    private void setupMetadataEditor() {
        star1.setOnAction(e -> setRating(1));
        star2.setOnAction(e -> setRating(2));
        star3.setOnAction(e -> setRating(3));
        star4.setOnAction(e -> setRating(4));
        star5.setOnAction(e -> setRating(5));

        colorRed.setOnAction(e -> setColorLabel("Red"));
        colorYellow.setOnAction(e -> setColorLabel("Yellow"));
        colorGreen.setOnAction(e -> setColorLabel("Green"));
        colorBlue.setOnAction(e -> setColorLabel("Blue"));
        colorClear.setOnAction(e -> setColorLabel(""));

        saveMetaButton.setOnAction(e -> saveCustomMetadata());
    }

    private void setRating(int rating) {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            return;
        }
        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        String path = currentImageFile.getFile().getAbsolutePath();
        configService.getConfig().getRatings().put(path, rating);
        configService.saveConfig();
        updateRatingUI(rating);
        lastAction.setText("Rated " + rating + " stars: " + currentImageFile.getFile().getName());
        
        String sortField = configService.getConfig().getSortField();
        if ("Rating".equals(sortField)) {
            sortCurrentImages();
        }
    }

    private void setColorLabel(String label) {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            return;
        }
        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        String path = currentImageFile.getFile().getAbsolutePath();
        if (label.isEmpty()) {
            configService.getConfig().getColorLabels().remove(path);
        } else {
            configService.getConfig().getColorLabels().put(path, label);
        }
        configService.saveConfig();
        updateColorLabelUI(label);
        lastAction.setText("Labeled '" + (label.isEmpty() ? "None" : label) + "': " + currentImageFile.getFile().getName());
        
        String sortField = configService.getConfig().getSortField();
        if ("Color Label".equals(sortField)) {
            sortCurrentImages();
        }
    }

    private void saveCustomMetadata() {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            return;
        }
        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        String path = currentImageFile.getFile().getAbsolutePath();
        
        String title = metaTitleField.getText();
        String desc = metaDescField.getText();
        String tags = metaTagsField.getText();

        if (title == null || title.trim().isEmpty()) {
            configService.getConfig().getTitles().remove(path);
        } else {
            configService.getConfig().getTitles().put(path, title.trim());
        }

        if (desc == null || desc.trim().isEmpty()) {
            configService.getConfig().getDescriptions().remove(path);
        } else {
            configService.getConfig().getDescriptions().put(path, desc.trim());
        }

        if (tags == null || tags.trim().isEmpty()) {
            configService.getConfig().getTags().remove(path);
        } else {
            configService.getConfig().getTags().put(path, tags.trim());
        }

        configService.saveConfig();
        lastAction.setText("Saved metadata: " + currentImageFile.getFile().getName());
    }

    private void displayCustomMetadata() {
        if (currentImages == null || currentImageIndex < 0 || currentImageIndex >= currentImages.size()) {
            updateRatingUI(0);
            updateColorLabelUI("");
            metaTitleField.clear();
            metaDescField.clear();
            metaTagsField.clear();
            return;
        }
        
        ImageFile currentImageFile = currentImages.get(currentImageIndex);
        String path = currentImageFile.getFile().getAbsolutePath();

        int rating = configService.getConfig().getRatings().getOrDefault(path, 0);
        updateRatingUI(rating);

        String label = configService.getConfig().getColorLabels().getOrDefault(path, "");
        updateColorLabelUI(label);

        metaTitleField.setText(configService.getConfig().getTitles().getOrDefault(path, ""));
        metaDescField.setText(configService.getConfig().getDescriptions().getOrDefault(path, ""));
        metaTagsField.setText(configService.getConfig().getTags().getOrDefault(path, ""));
    }

    private void updateRatingUI(int rating) {
        star1.setText(rating >= 1 ? "★" : "☆");
        star2.setText(rating >= 2 ? "★" : "☆");
        star3.setText(rating >= 3 ? "★" : "☆");
        star4.setText(rating >= 4 ? "★" : "☆");
        star5.setText(rating >= 5 ? "★" : "☆");
    }

    private void updateColorLabelUI(String label) {
        colorRed.setStyle("-fx-background-color: #ff5555; -fx-min-width: 16px; -fx-min-height: 16px; -fx-max-width: 16px; -fx-max-height: 16px; -fx-background-radius: 8; -fx-cursor: hand;" + ("Red".equals(label) ? " -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;" : ""));
        colorYellow.setStyle("-fx-background-color: #ffaa00; -fx-min-width: 16px; -fx-min-height: 16px; -fx-max-width: 16px; -fx-max-height: 16px; -fx-background-radius: 8; -fx-cursor: hand;" + ("Yellow".equals(label) ? " -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;" : ""));
        colorGreen.setStyle("-fx-background-color: #55ff55; -fx-min-width: 16px; -fx-min-height: 16px; -fx-max-width: 16px; -fx-max-height: 16px; -fx-background-radius: 8; -fx-cursor: hand;" + ("Green".equals(label) ? " -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;" : ""));
        colorBlue.setStyle("-fx-background-color: #5555ff; -fx-min-width: 16px; -fx-min-height: 16px; -fx-max-width: 16px; -fx-max-height: 16px; -fx-background-radius: 8; -fx-cursor: hand;" + ("Blue".equals(label) ? " -fx-border-color: white; -fx-border-width: 2; -fx-border-radius: 8;" : ""));
    }

    private void sortCurrentImages() {
        if (currentImages == null || currentImages.isEmpty()) {
            return;
        }

        ImageFile currentFile = (currentImageIndex >= 0 && currentImageIndex < currentImages.size()) 
                ? currentImages.get(currentImageIndex) : null;

        String sortField = configService.getConfig().getSortField();
        String sortOrder = configService.getConfig().getSortOrder();
        boolean ascending = "Ascending".equals(sortOrder);

        currentImages.sort((a, b) -> {
            int cmp = 0;
            switch (sortField) {
                case "Date Created":
                    long timeA = ImageUtils.getCreationTime(a.getFile());
                    long timeB = ImageUtils.getCreationTime(b.getFile());
                    cmp = Long.compare(timeA, timeB);
                    break;
                case "Date Modified":
                    cmp = Long.compare(a.getFile().lastModified(), b.getFile().lastModified());
                    break;
                case "Size":
                    cmp = Long.compare(a.getFile().length(), b.getFile().length());
                    break;
                case "Rating":
                    int ratingA = configService.getConfig().getRatings().getOrDefault(a.getFile().getAbsolutePath(), 0);
                    int ratingB = configService.getConfig().getRatings().getOrDefault(b.getFile().getAbsolutePath(), 0);
                    cmp = Integer.compare(ratingA, ratingB);
                    if (cmp == 0) {
                        cmp = a.getName().compareToIgnoreCase(b.getName());
                    }
                    break;
                case "Color Label":
                    String colorA = configService.getConfig().getColorLabels().getOrDefault(a.getFile().getAbsolutePath(), "");
                    String colorB = configService.getConfig().getColorLabels().getOrDefault(b.getFile().getAbsolutePath(), "");
                    cmp = colorA.compareToIgnoreCase(colorB);
                    if (cmp == 0) {
                        cmp = a.getName().compareToIgnoreCase(b.getName());
                    }
                    break;
                case "Name":
                default:
                    cmp = a.getName().compareToIgnoreCase(b.getName());
                    break;
            }
            return ascending ? cmp : -cmp;
        });

        if (currentFile != null) {
            currentImageIndex = currentImages.indexOf(currentFile);
            if (currentImageIndex == -1) {
                currentImageIndex = 0;
            }
        } else {
            currentImageIndex = 0;
        }

        displayCurrentImage();
        updateThumbnails();
    }
}