# Project Memory & Rules: ImageSorterPro

This workspace contains **ImageSorterPro**, a lightweight, Maven-based JavaFX desktop application designed to quick-sort and organize images and videos using configurable hotkeys and mouse navigation.

---

## 🛠️ Technology Stack & Dependencies
- **Core**: Java 11/14, JavaFX 21 (Controls, FXML, Swing, Media, graphics).
- **Theme**: AtlantaFX (`PrimerLight` stylesheet for modern aesthetics).
- **Serialization**: Jackson Databind (manages `image_sort_config.json`).
- **Metadata/EXIF**: `metadata-extractor` (reading EXIF/MP4 orientation tags), `commons-imaging` (lossless EXIF rotation/rewrites).
- **Video Utilities**: `jcodec` (for fast video frame/thumbnail extraction).

---

## 🏗️ Architecture & Component Map

### 1. Application Entry
- **[`ImageSorterApp.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/ImageSorterApp.java)**: Sets up the main window, registers stylesheets, handles shutdown hooks to save configuration, and initializes the directory path if passed via CLI argument.

### 2. View Controllers (UI Layer)
- **[`MainController.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/controller/MainController.java)**:
  - Hooks key-pressed events (e.g., `1-9`, `a-z` to move files, navigation arrows, `Delete` to trash, `Ctrl+Z` to undo).
  - Handles image rendering, video player insertion, thumbnails strip updates, and progress reporting.
- **[`ConfigController.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/controller/ConfigController.java)**: Controller backing the modal dialog where hotkeys and directories are customized.

### 3. Service Layer (Business Logic)
- **[`ConfigService.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/service/ConfigService.java)**: Thread-safe singleton that loads/saves configuration properties to `image_sort_config.json`.
- **[`ImageService.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/service/ImageService.java)**: Discovers images/videos inside folders, handles in-memory caching (`ImageCache`), and coordinates background worker pools for asynchronous image pre-caching.

### 4. Custom Media Components
- **[`Player.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/videoplayer/Player.java)**: Custom component wrapping JavaFX's `MediaPlayer` and `MediaView` to play video files, ensuring correct orientation (90/180/270 degrees) and responsive layout.
- **[`MediaBar.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/videoplayer/MediaBar.java)**: Horizontal control panel for playing/pausing, timeline tracking, and volume adjustment.
- **[`FastVideoThumbnailUtil.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/videoplayer/FastVideoThumbnailUtil.java)**: Generates video thumbnails.

### 5. Utility Layer
- **[`ImageUtils.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/utils/ImageUtils.java)**: 
  - Rotation logic (`rotateExifOrientation`, `rotateImage`).
  - Reads EXIF rotation directories and Mp4 video rotation tags.
  - Matches extension types from `ConfigSettings` to categorize images vs videos.

---

## ⚠️ Notes & Guidelines for Future Agents
- **Metadata Integrity**: Keep docstrings and comments intact unless directly requested otherwise.
- **Hardcoded Desktop Paths**: Notice that [`ImageUtils.java`](file:///D:/master-drive/nirmal/Dev/PhotoSort/ImageSorterPro/src/main/java/com/imagesorter/utils/ImageUtils.java#L200) contains a hardcoded temp file path targeting `"C:\\Users\\Nirmal\\Desktop\\"` to rewrite EXIF metadata losslessly. Be cautious of permissions/folder availability if modifications are made to metadata rewriting logic.
- **Supported File Types**: Extensions are dynamically loaded from `ConfigSettings`. Do not hardcode file formats/suffixes in controllers or image loading classes.
- **Video Thumbnail Extraction**: Video files display actual frame thumbnails extracted using `FastVideoThumbnailUtil`'s `ffmpeg` execution. If extraction fails or ffmpeg is absent, it falls back to a safe classpath resolution (`/video.png` relative to the class) with a null check, avoiding unhandled `NullPointerException`s that would crash the thumbnail loading loop.
- **Keyboard Focus & Event Filtering**: Global navigation key presses and hotkeys are registered as a scene-level event filter (`addEventFilter`) rather than event handlers. This ensures hotkeys are processed even when individual sub-components are active. In addition, video player controls are set as non-focusable (`setFocusTraversable(false)`) and clicking inside the media layout automatically invokes `setupKeyboardFocus()` to ensure stable keyboard interactions.


