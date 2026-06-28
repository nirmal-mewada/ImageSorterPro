# Project Memory & Rules: ImageSorterPro

This workspace contains **ImageSorterPro**, a lightweight, Maven-based JavaFX desktop application designed to quick-sort and organize images and videos using configurable hotkeys and mouse navigation.

---

## 🛠️ Technology Stack & Dependencies
- **Core**: Java 11/14, JavaFX 21 (Controls, FXML, Swing, Media, graphics).
- **Theme**: AtlantaFX (`CupertinoDark` stylesheet as default theme).
- **Serialization**: Jackson Databind (manages `image_sort_config.json`).
- **Metadata/EXIF**: `metadata-extractor` (reading EXIF/MP4 orientation tags), `commons-imaging` (lossless EXIF rotation/rewrites).
- **Video Utilities**: `jcodec` (for fast video frame/thumbnail extraction).

---

## 🏗️ Architecture & Component Map

### 1. Application Entry
- **[`ImageSorterApp.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/ImageSorterApp.java)**: Sets up the main window, registers stylesheets, handles shutdown hooks to save configuration, and initializes the directory path if passed via CLI argument.

### 2. View Controllers (UI Layer)
- **[`MainController.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/controller/MainController.java)**:
  - Hooks key-pressed events (e.g., `1-9`, `a-z` to move files, navigation arrows, `Delete` to trash, `Ctrl+Z` to undo).
  - Handles image rendering, video player insertion, thumbnails strip updates, and progress reporting.
- **[`ConfigController.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/controller/ConfigController.java)**: Controller backing the modal dialog where hotkeys and directories are customized.

### 3. Service Layer (Business Logic)
- **[`ConfigService.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/service/ConfigService.java)**: Thread-safe singleton that loads/saves configuration properties to `image_sort_config.json`.
- **[`ImageService.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/service/ImageService.java)**: Discovers images/videos inside folders, handles in-memory caching (`ImageCache`), and coordinates background worker pools for asynchronous image pre-caching.

### 4. Custom Media Components
- **[`Player.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/videoplayer/Player.java)**: Custom component wrapping JavaFX's `MediaPlayer` and `MediaView` to play video files, ensuring correct orientation (90/180/270 degrees) and responsive layout.
- **[`MediaBar.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/videoplayer/MediaBar.java)**: Horizontal control panel for playing/pausing, timeline tracking, and volume adjustment.
- **[`FastVideoThumbnailUtil.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/videoplayer/FastVideoThumbnailUtil.java)**: Generates video thumbnails.

### 5. Utility Layer
- **[`ImageUtils.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/utils/ImageUtils.java)**: 
  - Rotation logic (`rotateExifOrientation`, `rotateImage`).
  - Reads EXIF rotation directories and Mp4 video rotation tags.
  - Matches extension types from `ConfigSettings` to categorize images vs videos.

---

## ⚠️ Notes & Guidelines for Future Agents
- **Metadata Integrity**: Keep docstrings and comments intact unless directly requested otherwise.
- **Hardcoded Desktop Paths**: Notice that [`ImageUtils.java`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/utils/ImageUtils.java#L200) contains a hardcoded temp file path targeting `"C:\\Users\\Nirmal\\Desktop\\"` to rewrite EXIF metadata losslessly. Be cautious of permissions/folder availability if modifications are made to metadata rewriting logic.
- **Supported File Types**: Extensions are dynamically loaded from `ConfigSettings`. Do not hardcode file formats/suffixes in controllers or image loading classes.
- **Video Thumbnail Extraction**: Video files display actual frame thumbnails extracted using `FastVideoThumbnailUtil`'s `ffmpeg` execution. If extraction fails or ffmpeg is absent, it falls back to a safe classpath resolution (`/video.png` relative to the class) with a null check, avoiding unhandled `NullPointerException`s that would crash the thumbnail loading loop.
- **Keyboard Focus & Event Filtering**: Global navigation key presses and hotkeys are registered as a scene-level event filter (`addEventFilter`) rather than event handlers. This ensures hotkeys are processed even when individual sub-components are active. In addition, video player controls are set as non-focusable (`setFocusTraversable(false)`) and clicking inside the media layout automatically invokes `setupKeyboardFocus()` to ensure stable keyboard interactions. Spacebar pauses/resumes video playback when a video is active.
- **EXIF Metadata Properties Pane**: The collapsible right panel (`rightVBox` containing `metadataGridPane`) dynamically displays details read via `ImageUtils.getMetadataMap()` (shutter speed, ISO, aperture, focal length, camera make/model, date taken, dimensions, and duration for videos). A View menu toggle control toggles its presence.
- **Auto-Advance Toggle**: The `autoAdvanceCheckBox` checkbox controls behavior when a file is moved/deleted: auto-advancing to the next file (if checked) or clearing to a prompt state waiting for manual navigation (if unchecked).
- **Workspace Bookmarks**: Bookmarks can be added/removed using `addBookmarkMenuItem` and `removeBookmarkMenuItem` in the top-level **Bookmarks** Menu, and are saved in `ConfigSettings` to persist across restarts. Selecting a bookmark loads its directory.
- **Action Modes**: Options include `Move Directly`, `Copy Directly`, `Stage Move (Batch)`, and `Stage Copy (Batch)` set as mutually exclusive `RadioMenuItem` toggles in the **View > Action Mode** submenu.
- **Batch Staging**: When in Staged modes, hotkeys queue files in `stagedActions` without disk I/O. Staged counts and execution buttons (`Commit Staged`, `Clear Batch`) appear in the bottom status bar (`batchControlsBox`) dynamically when staging modes are active. Undo (`Ctrl+Z`) pops the last staged item instantly from the queue.
- **Slideshow Mode**: Toggled via F5 or **View > Slideshow > Play Slideshow**. Speed intervals are selected via **View > Slideshow > Interval** presets (1s, 2s, 3s, 5s, 10s) or set as a custom duration using `TextInputDialog`.
- **Full Screen Mode**: F11 or Enter toggles full screen. When active, the left panel automatically hides and reveals itself when the mouse moves to the left edge of the screen (< 15px), sliding back out of view when the mouse exits its bounds.
- **Video Player Pre-caching**: Controlled by the `preloadVideos` flag in `ConfigSettings` (default: `false`) and toggled via **View > Preload Video Players**. When enabled, the pre-caching pipeline in `ImageService.preCacheImages()` also creates `Media` + `MediaPlayer` objects for nearby video files using [`VideoPlayerCache`](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/java/com/imagesorter/model/VideoPlayerCache.java). Cached players are muted and paused until consumed via `take()`, which transfers ownership to the caller. `Player` has a second constructor `Player(ImageFile, MediaPlayer)` that reuses these pre-created players for near-instant video display. The cache uses an LRU eviction policy matching the image cache size and properly disposes evicted `MediaPlayer` instances. This is a high-memory feature and is opt-in.
- **GraalVM Windows Native Image compilation**:
  - The `native` profile in `pom.xml` compiles raw native binaries on Windows using the `native-maven-plugin`.
  - Builds require the Liberica NIK Full JDK distribution (`java-package: 'jdk+fx'` parameter in the setup-graalvm action) so JavaFX dynamic libraries/headers (`.dll`/`.lib`) are present in the compiler environment.
  - Native image configuration files ([reflect-config.json](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/resources/META-INF/native-image/reflect-config.json) and [resource-config.json](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/src/main/resources/META-INF/native-image/resource-config.json)) are located statically in `src/main/resources/META-INF/native-image/` to cover reflection classes (controllers, Jackson deserialization model targets) and resources (FXMLs, CSS, themes, assets, and AtlantaFX package files).
  - Native compilation runs are memory-restricted via `<buildArg>-J-Xmx8g</buildArg>` to prevent builder OOM failures on standard CI runners.
- **Windows Context Menu Shell Integration**:
  - Context menu script files are located under `scripts/windows/` ([context-menu-install.bat](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/scripts/windows/context-menu-install.bat) and [context-menu-uninstall.bat](file:///Users/nirmal.s/Documents/Projects/ImageSorterPro/scripts/windows/context-menu-uninstall.bat)).
  - They register/unregister "Open with ImageSorterPro" in the Windows shell on both folder right-click (`Directory\shell`) and folder background right-click (`Directory\Background\shell`) under `HKEY_CURRENT_USER\Software\Classes`.
  - These scripts are copied to the installer directory before `jpackage` runs to bundle them inside the Windows EXE installer.
- **CI/CD Pipeline (release.yml)**:
  - Builds three main installer/package outputs: Windows EXE installer (with embedded JRE), Windows Native binary, and macOS Apple Silicon ZIP app bundle. macOS Intel builds are disabled to optimize pipeline latency.
  - Attaches these packages along with a portable Cross-platform Fat JAR and context-menu registry scripts to GitHub Releases.
