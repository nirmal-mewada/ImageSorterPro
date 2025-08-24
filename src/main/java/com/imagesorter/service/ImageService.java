package com.imagesorter.service;

import com.imagesorter.MemUtils;
import com.imagesorter.utils.ImageUtils;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import com.imagesorter.model.ImageCache;
import com.imagesorter.model.ImageFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Iterator;

/**
 * Service for handling image operations including loading, caching, EXIF rotation, and file management
 */
public class ImageService {


    private final ImageCache imageCache;
    private final ExecutorService executorService;
    private int processedCount = 0;
    private  ConfigService configService = ConfigService.getInstance();

    public ImageService() {
        ConfigService configService = ConfigService.getInstance();
        this.imageCache = new ImageCache(configService.getConfig().getCacheSize());
        this.executorService = Executors.newFixedThreadPool(configService.getConfig().getThreadPoolSize());
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
        processedCount = 0;

        return imageFiles;
    }

    /**
     * Loads an image from file with EXIF rotation applied
     */
    public Image loadImage(ImageFile imageFile) throws IOException {
        String filePath = imageFile.getPath();
        System.out.println("Loading: "+filePath+", Cache: "+this.imageCache.getStats().toString() +  ", Mem: "+ MemUtils.printHeapUsage());

        // Check cache first
        Image cachedImage = imageCache.get(filePath);
        if (cachedImage != null) {
            return cachedImage;
        }

        // Load image from file
        Image image;
        try (InputStream fis = new BufferedInputStream (new FileInputStream(imageFile.getFile()))) {
            image = new Image(fis, 1024, 0, true, true);
        }

        if(imageFile.getExifRotate() == null){
            imageFile.setExifRotate(ImageUtils.displayImageWithExifCorrection(imageFile.getFile()));
        }
        if(image.isError()){
           throw  new RuntimeException(image.getException());

        }

        // Cache the rotated image
        imageCache.put(filePath, image, imageFile.getSize());

        return image;
    }





    /**
     * Gets cached image if available
     */
    public Image getCachedImage(ImageFile imageFile) {
        return imageCache.get(imageFile.getPath());
    }

    /**
     * Pre-caches images around the current index for fast navigation
     */
    public void preCacheImages(List<ImageFile> images, int currentIndex, int prevImage, int nextImage) {
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
    public void clearCache() {
        imageCache.clear();
    }

    /**
     * Gets cache statistics
     */
    public ImageCache.CacheStats getCacheStats() {
        return imageCache.getStats();
    }

    /**
     * Increments processed count (called when image is moved/deleted)
     */
    public void incrementProcessedCount() {
        processedCount++;
    }

    /**
     * Gets the number of processed images
     */
    public int getProcessedCount() {
        return processedCount;
    }

    /**
     * Resets the processed count
     */
    public void resetProcessedCount() {
        processedCount = 0;
    }

    /**
     * Cleanup resources
     */
    public void shutdown() {
        executorService.shutdown();
        imageCache.clear();
    }
}