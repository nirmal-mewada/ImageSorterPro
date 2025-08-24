# Image Sorter Pro

A powerful JavaFX desktop application for rapidly sorting images from a source folder into multiple pre-configured destination folders using keyboard hotkeys.

## Features

- **Fast Image Navigation**: Browse through images using arrow keys or mouse clicks
- **Hotkey Sorting**: Instantly move images to configured folders using number keys 1-9
- **Archive Function**: Move images to an archive folder using the 0 key
- **Image Caching**: Pre-loads next 10 images for smooth navigation
- **Progress Tracking**: Real-time status showing current file and progress
- **Flexible Configuration**: Easy setup of destination folders through GUI
- **File Safety**: Automatic handling of naming conflicts

## System Requirements

- Java 11 or higher
- JavaFX 11 or higher
- Windows operating system (primary target)
- Minimum 4GB RAM recommended
- 100MB free disk space

## Quick Start

### 1. Setup Development Environment

1. Install JDK 11+ from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
2. Install [IntelliJ IDEA](https://www.jetbrains.com/idea/)
3. Download JavaFX SDK from [Gluon](https://gluonhq.com/products/javafx/)

### 2. Create Project

1. Open IntelliJ IDEA
2. Create New Project → JavaFX → Maven
3. Set Project Name: `ImageSorterPro`
4. Follow the detailed setup guide in the setup artifact

### 3. Run Application

```bash
# Using Maven
mvn clean javafx:run

# Or in IntelliJ
Run → Run 'ImageSorterApp'
```

## Usage Guide

### Initial Setup

1. **Launch Application**: Start Image Sorter Pro
2. **Configure Folders**: Go to File → Configure Folders
3. **Set Destinations**: Assign folders to hotkeys 1-9 using Browse buttons
4. **Save Configuration**: Click Save to store your settings

### Sorting Workflow

1. **Open Image Folder**: File → Open Folder (Ctrl+O)
2. **Navigate Images**:
    - Right Arrow / Right Click: Next image
    - Left Arrow / Left Click: Previous image
3. **Sort Images**:
    - Press 1-9: Move to configured folder
    - Press 0: Move to Archive folder
    - Delete Key: Delete current image (with confirmation)

### Keyboard Shortcuts

| Key | Action |
|-----|--------|
| Ctrl+O | Open Folder |
| Ctrl+P | Configure Folders |
| 1-9 | Move image to configured folder |
| 0 | Move image to Archive |
| ← → | Navigate previous/next image |
| Delete | Delete current image |
| Alt+F4 | Exit application |

## Project Structure

```
ImageSorterPro/
├── src/main/java/com/imagesorter/
│   ├── ImageSorterApp.java              # Main application class
│   ├── controller/
│   │   ├── MainController.java          # Main window controller
│   │   └── ConfigController.java        # Configuration dialog controller
│   ├── model/
│   │   ├── ImageFile.java               # Image file representation
│   │   ├── ConfigSettings.java          # Configuration data model
│   │   └── ImageCache.java              # LRU cache for images
│   └── service/
│       ├── ImageService.java            # Image loading and caching
│       └── ConfigService.java           # Configuration management
├── src/main/resources/com/imagesorter/
│   ├── view/
│   │   ├── main.fxml                    # Main window layout
│   │   └── config.fxml                  # Configuration dialog layout
│   └── css/
│       └── styles.css                   # Application stylesheet
├── pom.xml                              # Maven configuration
└── README.md                            # This file
```

## Advanced Features

### Image Caching System

- **Smart Pre-loading**: Automatically caches next 10 images for instant navigation
- **Memory Management**: LRU (Least Recently Used) cache eviction
- **Background Loading**: Non-blocking image loading using background threads

### Configuration Management

- **Persistent Settings**: Configuration saved in `~/.imagesorter/config.json`
- **Automatic Backup**: Configuration backed up before changes
- **Validation**: Automatic validation of folder paths and permissions

### File Operations

- **Safe Moving**: Atomic file operations with conflict resolution
- **Naming Conflicts**: Automatic renaming (file_1.jpg, file_2.jpg, etc.)
- **Archive Function**: Creates Archive subfolder in source directory

## Supported Image Formats

- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)
- BMP (.bmp)
- TIFF (.tiff, .tif)

## Configuration File Location

- **Windows**: `%USERPROFILE%\.imagesorter\config.json`
- **Linux/Mac**: `~/.imagesorter/config.json`

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

### Debug Mode

Enable debug logging by adding VM option:
```
-Dcom.imagesorter.debug=true
```

## Building for Distribution

### Create Executable JAR

```bash
mvn clean package
```

### Create Native Installer (Windows)

1. Install [jpackage](https://docs.oracle.com/en/java/javase/14/jpackage/)
2. Run packaging script:

```bash
jpackage --input target/ \
  --name "Image Sorter Pro" \
  --main-jar ImageSorterPro-1.0-SNAPSHOT.jar \
  --main-class com.imagesorter.ImageSorterApp \
  --type exe \
  --win-dir-chooser \
  --win-shortcut \
  --win-menu
```

## Performance Tips

1. **Optimal Cache Size**: Set cache to 15-25 images for best performance
2. **SSD Storage**: Use SSD for source and destination folders
3. **Memory**: Allocate sufficient heap space: `-Xmx2G`
4. **Image Size**: Large images (>10MB) may impact performance

## Development

### Adding New Features

1. **Controllers**: Add UI logic in `controller` package
2. **Models**: Add data structures in `model` package
3. **Services**: Add business logic in `service` package
4. **FXML**: Update UI layouts in `resources/view`
5. **CSS**: Style changes in `resources/css`

### Testing

```bash
# Run tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### Code Style

- Use Java naming conventions
- Document public methods with Javadoc
- Keep methods under 50 lines
- Use meaningful variable names

## Contributing

1. Fork the repository
2. Create feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -m "Add feature"`
4. Push branch: `git push origin feature-name`
5. Submit pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and questions:
- Create an issue on GitHub
- Check troubleshooting section
- Review configuration settings

## Changelog

### Version 1.0.0
- Initial release
- Basic image sorting functionality
- Configuration management
- Keyboard shortcuts
- Image caching system

---

**Image Sorter Pro** - Streamline your image organization workflow!