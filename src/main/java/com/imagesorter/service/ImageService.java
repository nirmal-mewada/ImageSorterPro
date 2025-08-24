package com.imagesorter.service;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Rotate;
import com.imagesorter.model.ImageCache;
import com.imagesorter.model.ImageFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
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
     * Loads an image from file with EXIF rotation applied
     */
    public Image loadImage(ImageFile imageFile) throws IOException {
        String filePath = imageFile.getPath();
        System.out.println("Loading: "+filePath+", Cache: "+this.imageCache.getStats().toString());

        // Check cache first
        Image cachedImage = imageCache.get(filePath);
        if (cachedImage != null) {
            return cachedImage;
        }

        // Load image from file
        Image image;
        try (InputStream fis = new BufferedInputStream (new FileInputStream(imageFile.getFile()))) {
            image = new Image(fis);
        }

        // Apply EXIF rotation if needed
        image = applyExifRotation(image, imageFile);

        // Cache the rotated image
        imageCache.put(filePath, image, imageFile.getSize());

        return image;
    }

    /**
     * Applies EXIF rotation to the image
     */
    private Image applyExifRotation(Image originalImage, ImageFile imageFile) {
        try {
            int orientation = getExifOrientation(imageFile.getFile());

            if (orientation == 1) {
                // No rotation needed
                return originalImage;
            }

            return rotateImage(originalImage, orientation);

        } catch (Exception e) {
            System.err.println("Failed to read EXIF data for " + imageFile.getName() + ": " + e.getMessage());
            return originalImage; // Return original if EXIF reading fails
        }
    }

    /**
     * Reads EXIF orientation from image file
     */
    private int getExifOrientation(File imageFile) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(imageFile)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                return 1; // Default orientation
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);

                // Try to read EXIF metadata
                javax.imageio.metadata.IIOMetadata metadata = reader.getImageMetadata(0);
                if (metadata != null) {
                    return extractOrientationFromMetadata(metadata);
                }

            } finally {
                reader.dispose();
            }
        }

        return 1; // Default orientation if no EXIF found
    }

    /**
     * Extracts orientation from image metadata
     */
    private int extractOrientationFromMetadata(javax.imageio.metadata.IIOMetadata metadata) {
        try {
            // Get metadata as XML
            org.w3c.dom.Node root = metadata.getAsTree(metadata.getNativeMetadataFormatName());

            if (root != null) {
                return findOrientationInNode(root);
            }
        } catch (Exception e) {
            System.err.println("Error reading EXIF metadata: " + e.getMessage());
        }

        return 1; // Default orientation
    }

    /**
     * Recursively searches for orientation in metadata XML nodes
     */
    private int findOrientationInNode(org.w3c.dom.Node node) {
        if (node.getNodeName().equals("tiff:Orientation") ||
                node.getNodeName().equals("Orientation")) {

            org.w3c.dom.NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                org.w3c.dom.Node valueNode = attributes.getNamedItem("value");
                if (valueNode != null) {
                    try {
                        return Integer.parseInt(valueNode.getNodeValue());
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }

        // Check child nodes
        org.w3c.dom.NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            int orientation = findOrientationInNode(children.item(i));
            if (orientation != 1) {
                return orientation;
            }
        }

        return 1; // Default orientation
    }

    /**
     * Rotates image based on EXIF orientation value
     */
    private Image rotateImage(Image originalImage, int orientation) {
        if (orientation == 1) {
            return originalImage; // No rotation needed
        }

        double width = originalImage.getWidth();
        double height = originalImage.getHeight();

        PixelReader pixelReader = originalImage.getPixelReader();
        WritableImage rotatedImage;
        PixelWriter pixelWriter;

        switch (orientation) {
            case 2: // Flip horizontal
                rotatedImage = new WritableImage((int) width, (int) height);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb((int) width - 1 - x, y,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            case 3: // Rotate 180°
                rotatedImage = new WritableImage((int) width, (int) height);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb((int) width - 1 - x, (int) height - 1 - y,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            case 4: // Flip vertical
                rotatedImage = new WritableImage((int) width, (int) height);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb(x, (int) height - 1 - y,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            case 5: // Flip horizontal + rotate 90° CCW
                rotatedImage = new WritableImage((int) height, (int) width);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb(y, x,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            case 6: // Rotate 90° CW
                rotatedImage = new WritableImage((int) height, (int) width);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb((int) height - 1 - y, x,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            case 7: // Flip horizontal + rotate 90° CW
                rotatedImage = new WritableImage((int) height, (int) width);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb(y, (int) width - 1 - x,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            case 8: // Rotate 90° CCW
                rotatedImage = new WritableImage((int) height, (int) width);
                pixelWriter = rotatedImage.getPixelWriter();
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        pixelWriter.setArgb(y, (int) width - 1 - x,
                                pixelReader.getArgb(x, y));
                    }
                }
                return rotatedImage;

            default:
                return originalImage;
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
     * Checks if a file is a supported image format
     */
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex >= 0 && dotIndex < name.length() - 1) {
            String extension = name.substring(dotIndex + 1);
            return configService.getConfig().getSupportedExtensions().contains(extension);
        }

        return false;
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