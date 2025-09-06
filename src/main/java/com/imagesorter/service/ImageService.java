package com.imagesorter.service;

import com.imagesorter.model.ImageCache;
import com.imagesorter.model.ImageFile;
import com.imagesorter.utils.ImageUtils;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

/**
 * Service for handling image operations including loading, caching, EXIF rotation, and file management
 */
public class ImageService {


    private final ImageCache imageCache;
    private final ThreadPoolExecutor executorService;

    public ImageService() {
        ConfigService configService = ConfigService.getInstance();
        this.imageCache = new ImageCache(configService.getConfig().getCacheSize());
        this.executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(configService.getConfig().getThreadPoolSize());
    }

    /**
     * Loads all supported image files from a directory
     */
    public List<ImageFile> loadImagesFromFolder(File folder) {
        List<ImageFile> imageFiles = new ArrayList<>();

        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            return imageFiles;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return imageFiles;
        }

        for (File file : files) {
            if (file.isFile() && ImageUtils.isImageFile(this, file)) {
                ImageFile imageFile = new ImageFile(file);
                if (imageFile.isValidImageFile()) {
                    imageFiles.add(imageFile);
                }
            }
        }

        // Sort by name for consistent ordering
        imageFiles.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Reset processed count when loading new folder
        return imageFiles;
    }

    /**
     * Loads an image from file with EXIF rotation applied
     */
    public Image loadImage(ImageFile imageFile) throws IOException {
        String filePath = imageFile.getPath();

        // Return cached image if available
        Image cachedImage = imageCache.get(filePath);
        if (cachedImage != null) {
            ensureExifRotation(imageFile);
            return cachedImage;
        }
        // Load image from file
        Image image;
        try (InputStream fis = Files.newInputStream(imageFile.getFile().toPath())) {
            int size = ConfigService.getInstance().getConfig().getImageQualityPx();
            image = new Image(fis, size, 0, true, false);
        }

        ensureExifRotation(imageFile);

        // Apply rotation if needed
        if (imageFile.getExifRotate() != 0) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            bufferedImage = ImageUtils.rotateImage(bufferedImage, imageFile.getExifRotate());
            image = SwingFXUtils.toFXImage(bufferedImage, null);
        }

        // Throw exception if image loading failed
        if (image.isError()) {
            throw new RuntimeException(image.getException());
        }

        // Cache the loaded image
        imageCache.put(filePath, image, imageFile.getSize());

        return image;
    }
//    public Image loadImage1(ImageFile imageFile) throws IOException {
//        String filePath = imageFile.getPath();
//
//        // 1. Return cached image if available
//        Image cachedImage = imageCache.get(filePath);
//        if (cachedImage != null) {
//            return cachedImage;
//        }
//
//        // 2. Read EXIF rotation once
//        ensureExifRotation(imageFile);
//
//        // 3. Load scaled image
//        int size = ConfigService.getInstance().getConfig().getImageQualityPx();
//        Image image;
//        try (InputStream fis = Files.newInputStream(imageFile.getFile().toPath())) {
//            image = new Image(fis, size, 0, true, true);
//        }
//
//        // 4. Apply rotation if needed (JavaFX Canvas, not SwingFXUtils)
//        if (imageFile.getExifRotate() != 0) {
//            image = rotateFXImage(image, imageFile.getExifRotate());
//        }
//
//        // 5. Validate
//        if (image.isError()) {
//            throw new RuntimeException(image.getException());
//        }
//
//        // 6. Cache final image (already rotated)
//        imageCache.put(filePath, image, imageFile.getSize());
//
//        return image;
//    }
//    private Image rotateFXImage(Image src, int angle) {
//        double w = src.getWidth();
//        double h = src.getHeight();
//
//        double newW = (angle % 180 == 0) ? w : h;
//        double newH = (angle % 180 == 0) ? h : w;
//
//        Canvas canvas = new Canvas(newW, newH);
//        GraphicsContext gc = canvas.getGraphicsContext2D();
//
//        gc.save();
//        // Translate to center of new canvas
//        gc.translate(newW / 2, newH / 2);
//        gc.rotate(angle);
//        // Draw with original center aligned
//        gc.drawImage(src, -w / 2, -h / 2);
//        gc.restore();
//
//        SnapshotParameters params = new SnapshotParameters();
//        params.setFill(Color.TRANSPARENT);
//        return canvas.snapshot(params, null);
//    }



    private void ensureExifRotation(ImageFile imageFile) {
        if (imageFile.getExifRotate() == null) {
            imageFile.setExifRotate(ImageUtils.displayImageWithExifCorrection(imageFile.getFile()));
        }
    }





    /**
     * Gets cached image if available
     */
    public Image getCachedImage(ImageFile imageFile) {
        return imageCache.get(imageFile.getPath());
    }

    public List<Image> getRecentImages(int count) {
        return imageCache.getRecentImages(count);
    }

    /**
     * Pre-caches images around the current index for fast navigation
     */
    public void preCacheImages(List<ImageFile> images, int currentIndex, int prevImage, int nextImage, Consumer<Integer> progress) {
        if (images == null || images.isEmpty()) {
            return;
        }

        int start = Math.max(0, currentIndex - prevImage);
        int end = Math.min(images.size() - 1, currentIndex + nextImage );

        // Cache images in background
        for (int i = start; i <= end; i++) {
            final int index = i;
            final ImageFile imageFile = images.get(index);

            // Skip if already cached
            if (imageCache.contains(imageFile.getPath())) {
                continue;
            }

            // Submit loading task
            executorService.submit(() -> {
                try {
                    loadImage(imageFile);
                    if(progress!=null)
                        progress.accept(executorService.getActiveCount()-1);
                } catch (IOException e) {
                    // Log error but don't throw - this is background caching
                    System.err.println("Failed to pre-cache image: " + imageFile.getName() +
                            " - " + e.getMessage());
                }
            });
        }
    }

    /**
     * Loads image asynchronously and returns a Future
     */
    public Future<Image> loadImageAsync(ImageFile imageFile) {
        return executorService.submit(() -> loadImage(imageFile));
    }

    /**
     * Creates a Task for loading image (for JavaFX background processing)
     */
    public Task<Image> createLoadImageTask(ImageFile imageFile) {
        return new Task<Image>() {
            @Override
            protected Image call() throws Exception {
                return loadImage(imageFile);
            }
        };
    }


    /**
     * Clears the image cache
     */
    public void clearCache(ImageFile currentImageFile) {
        imageCache.remove(currentImageFile.getPath());
    }

    /**
     * Gets cache statistics
     */
    public ImageCache.CacheStats getCacheStats() {
        return imageCache.getStats();
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        executorService.shutdown();
        imageCache.clear();
    }
}