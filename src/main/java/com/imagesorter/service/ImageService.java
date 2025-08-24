// ImageService.java
package com.imagesorter.service;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import com.imagesorter.model.ImageCache;
import com.imagesorter.model.ImageFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service for handling image operations including loading, caching, and file management
 */
public class ImageService {
    
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif"
    ));
    
    private static final int DEFAULT_CACHE_SIZE = 20;
    private static final int THREAD_POOL_SIZE = 4;
    
    private final ImageCache imageCache;
    private final ExecutorService executorService;
    private int processedCount = 0;
    
    public ImageService() {
        this.imageCache = new ImageCache(DEFAULT_CACHE_SIZE);
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
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
            if (file.isFile() && isImageFile(file)) {
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
     * Loads an image from file
     */
    public Image loadImage(ImageFile imageFile) throws IOException {
        String filePath = imageFile.getPath();
        
        // Check cache first
        Image cachedImage = imageCache.get(filePath);
        if (cachedImage != null) {
            return cachedImage;
        }
        
        // Load image from file
        try (FileInputStream fis = new FileInputStream(imageFile.getFile())) {
            Image image = new Image(fis);
            
            // Cache the image
            imageCache.put(filePath, image, imageFile.getSize());
            
            return image;
        }
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
    public void preCacheImages(List<ImageFile> images, int currentIndex, int cacheRadius) {
        if (images == null || images.isEmpty()) {
            return;
        }
        
        int start = Math.max(0, currentIndex - cacheRadius / 2);
        int end = Math.min(images.size() - 1, currentIndex + cacheRadius / 2);
        
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
     * Checks if a file is a supported image format
     */
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');
        
        if (dotIndex >= 0 && dotIndex < name.length() - 1) {
            String extension = name.substring(dotIndex + 1);
            return SUPPORTED_EXTENSIONS.contains(extension);
        }
        
        return false;
    }
    
    /**
     * Gets the list of supported image extensions
     */
    public Set<String> getSupportedExtensions() {
        return new HashSet<>(SUPPORTED_EXTENSIONS);
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

