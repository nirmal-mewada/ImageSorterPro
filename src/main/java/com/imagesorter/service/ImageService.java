package com.imagesorter.service;

import com.imagesorter.model.ImageCache;
import com.imagesorter.model.ImageFile;
import com.imagesorter.utils.ImageUtils;
import com.imagesorter.videoplayer.FastVideoThumbnailUtil;
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
    private final java.util.Map<String, java.util.concurrent.Future<?>> pendingTasks = new java.util.concurrent.ConcurrentHashMap<>();

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
                if (imageFile.isValidImageFile() || imageFile.isVideoFile()) {
                    imageFiles.add(imageFile);
                }
            }
        }

        // Sort according to configuration
        com.imagesorter.model.ConfigSettings config = ConfigService.getInstance().getConfig();
        String sortField = config.getSortField();
        String sortOrder = config.getSortOrder();
        boolean ascending = "Ascending".equals(sortOrder);

        imageFiles.sort((a, b) -> {
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
                    int ratingA = config.getRatings().getOrDefault(a.getFile().getAbsolutePath(), 0);
                    int ratingB = config.getRatings().getOrDefault(b.getFile().getAbsolutePath(), 0);
                    cmp = Integer.compare(ratingA, ratingB);
                    if (cmp == 0) {
                        cmp = a.getName().compareToIgnoreCase(b.getName());
                    }
                    break;
                case "Color Label":
                    String colorA = config.getColorLabels().getOrDefault(a.getFile().getAbsolutePath(), "");
                    String colorB = config.getColorLabels().getOrDefault(b.getFile().getAbsolutePath(), "");
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
        Image image;

        if(imageFile.isVideoFile()) {
            image = FastVideoThumbnailUtil.createVideoThumbnail(imageFile.getFile());
        } else {
            // Load image from file

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
        }
        // Cache the loaded image
        imageCache.put(filePath, image, imageFile.getSize());

        return image;
    }

    /**
     * Loads a small thumbnail version of an image file to save memory and CPU
     */
    public Image loadThumbnail(ImageFile imageFile) throws IOException {
        String filePath = imageFile.getPath() + "_thumb";

        // Return cached image if available
        Image cachedImage = imageCache.get(filePath);
        if (cachedImage != null) {
            return cachedImage;
        }

        Image image;
        if (imageFile.isVideoFile()) {
            image = FastVideoThumbnailUtil.createVideoThumbnail(imageFile.getFile());
        } else {
            try (InputStream fis = Files.newInputStream(imageFile.getFile().toPath())) {
                // Smaller size (200px) specifically for thumbnails to reduce memory and load time
                image = new Image(fis, 200, 0, true, false);
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
        }

        // Cache the loaded thumbnail (estimate memory usage as 1/10th of original)
        imageCache.put(filePath, image, imageFile.getSize() / 10);

        return image;
    }

    public void ensureExifRotation(ImageFile imageFile) {
        if (imageFile.getExifRotate() == null) {
            if(imageFile.isVideoFile()){
                imageFile.setExifRotate(ImageUtils.getVideoRotation(imageFile.getFile()));
            }else {
                imageFile.setExifRotate(ImageUtils.displayImageWithExifCorrection(imageFile.getFile()));
            }
        }
    }

    /**
     * Gets cached image if available
     */
    public Image getCachedImage(ImageFile imageFile) {
        return imageCache.get(imageFile.getPath());
    }

    public Image getCachedThumbnail(ImageFile imageFile) {
        if (imageFile == null) return null;
        return imageCache.get(imageFile.getPath() + "_thumb");
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

        // Cancel pending pre-cache tasks that are no longer within the navigation window
        pendingTasks.forEach((path, future) -> {
            boolean inWindow = false;
            for (int i = start; i <= end; i++) {
                if (images.get(i).getPath().equals(path)) {
                    inWindow = true;
                    break;
                }
            }
            if (!inWindow) {
                future.cancel(false); // Cancel task softly
                pendingTasks.remove(path);
            }
        });

        // Cache images in background
        for (int i = start; i <= end; i++) {
            final int index = i;
            final ImageFile imageFile = images.get(index);
            final String path = imageFile.getPath();

            // Skip if already cached or already being processed
            if (imageCache.contains(path) || pendingTasks.containsKey(path)) {
                continue;
            }

            // Submit loading task
            java.util.concurrent.Future<?> future = executorService.submit(() -> {
                try {
                    // 1. Pre-cache full image
                    loadImage(imageFile);

                    // 2. Pre-cache thumbnail
                    try {
                        loadThumbnail(imageFile);
                    } catch (Exception e) {
                        System.err.println("Failed to pre-cache thumbnail: " + imageFile.getName() + " - " + e.getMessage());
                    }

                    // 3. Pre-cache metadata
                    if (imageFile.getMetadataMap() == null) {
                        try {
                            imageFile.setMetadataMap(ImageUtils.getMetadataMap(imageFile.getFile()));
                        } catch (Exception e) {
                            System.err.println("Failed to pre-cache metadata: " + imageFile.getName() + " - " + e.getMessage());
                        }
                    }

                    if(progress!=null)
                        progress.accept(executorService.getActiveCount()-1);
                } catch (IOException e) {
                    System.err.println("Failed to pre-cache image: " + imageFile.getName() +
                            " - " + e.getMessage());
                } finally {
                    pendingTasks.remove(path);
                }
            });
            pendingTasks.put(path, future);
        }
    }

    /**
     * Loads image asynchronously and returns a Future
     */
    public Future<Image> loadImageAsync(ImageFile imageFile) {
        return executorService.submit(() -> loadImage(imageFile));
    }

    public void submitTask(Runnable task) {
        executorService.submit(task);
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