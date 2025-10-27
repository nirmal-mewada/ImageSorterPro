# ImageSorterPro

ImageSorterPro is a lightweight, cross-platform desktop application built with JavaFX that helps you quickly sort and organize your image files. It provides a streamlined workflow for reviewing images and moving them to predefined destination folders using keyboard shortcuts or mouse clicks.

## Features

*   **Image Preview:** Large, clear preview of images.
*   **Hotkey-based Sorting:** Assign destination folders to keyboard hotkeys (1-9, a-z) for rapid image categorization.
*   **Click-to-Move:** Optionally move images by clicking on the left/right side of the image preview.
*   **Configurable Folders:** Easily set up and manage your destination folders through a dedicated configuration dialog.
*   **Virtual Delete with Undo:** Move unwanted images to a configurable "trash" folder instead of permanent deletion, with the ability to undo the last 50 actions (moves or virtual deletes).
*   **Navigation:** Navigate through images using arrow keys or mouse clicks.
*   **EXIF Orientation Support:** Automatically rotates images based on EXIF orientation data.
*   **Memory Efficient Caching:** Pre-caches surrounding images for smooth navigation.
*   **Cross-Platform:** Built with JavaFX, runs on Windows, macOS, and Linux.
*   **Adjustable Layout:** Use the SplitPane to adjust the width between the image viewer and the hotkey list.

## Screenshots

### Main Application Window

The main window provides an image preview, a list of configured hotkey folders, and status information.

![Main Application Window](screenshots/main.png)

### Configuration Dialog

The configuration dialog allows you to set up your destination folders for hotkeys and specify a trash folder for virtual deletes.

![Configuration Dialog](screenshots/config.png)

## System Requirements

- Java 11 or higher
- JavaFX 11 or higher
- Windows operating system (primary target)
- Minimum 4GB RAM recommended
- 100MB free disk space

## How to Use

### 1. Open a Folder

Click "File" -> "Open Folder..." or press `Ctrl+O` to select a folder containing your images.

### 2. Configure Destination Folders

Click "File" -> "Configure Folders..." or press `Ctrl+P` to open the configuration dialog.
*   Assign a folder path to each hotkey (1-9, a-z).
*   Specify a "Trash Folder" where deleted images will be moved.
*   Click "Save" to apply your changes.

### 3. Sort Images

*   **Hotkey Sorting:** With an image displayed, press the corresponding hotkey (1-9, a-z) to move the image to its assigned folder.
*   **Click-to-Move:** If "Enable click to move" is checked in the main window, click the right half of the image to go to the next image, and the left half to go to the previous image.
*   **Navigation:** Use the `Right Arrow` key for the next image, and `Left Arrow` key for the previous image.
*   **Virtual Delete:** Press the `Delete` key to move the current image to the configured "Trash Folder".
*   **Undo Last Action:** Press `Ctrl+Z` to undo the last move or virtual delete action. You can undo up to 50 actions.
*   **Archive:** Press `0` (zero) to move the current image to an "Archive" subfolder within the current source directory.

## Building from Source

ImageSorterPro is a Maven project.

### Prerequisites

*   Java Development Kit (JDK) 11 or higher
*   Maven (usually bundled with IDEs like IntelliJ IDEA or can be installed separately)
*   Download JavaFX SDK from [Gluon](https://gluonhq.com/products/javafx/

### Steps

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/ImageSorterPro.git
    cd ImageSorterPro
    ```
2.  **Build the project:**
    ```bash
    ./mvnw clean install
    ```
    This command will compile the source code, run tests, and package the application into a JAR file in the `target/` directory.

## Running the Application

After building, you can run the application from the `target/` directory:

```bash
java -Xms1G -Xmx1G -jar --module-path "D:\n-temp\javafx-sdk-24.0.2\lib" --add-modules javafx.controls,javafx.fxml,javafx.swing,javafx.media ImageSorterPro-1.0-SNAPSHOT.jar
```

Alternatively, you can use the provided wrapper scripts:

*   **Windows:** `mvnw.cmd javafx:run`
*   **Linux/macOS:** `./mvnw javafx:run`

## Troubleshooting

### Common Issues

#### JavaFX Runtime Not Found
```
Error: JavaFX runtime components are missing
```
**Solution**: Add JavaFX modules to VM options:
```
--module-path "C:\javafx-sdk-21\lib" --add-modules javafx.controls,javafx.fxml
```

#### Images Not Loading
- Check image file permissions
- Ensure supported image format
- Verify sufficient disk space

#### Performance Issues
- Reduce cache size in configuration
- Close other memory-intensive applications
- Check available system memory

## Contributing

Feel free to fork the repository, make improvements, and submit pull requests.

## License

This project is licensed under the MIT License - see the `LICENSE` file for details.
