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
    private final ThreadPoolExecutor thumbnailExecutor;
    private final java.util.Map<String, java.util.concurrent.Future<?>> pendingTasks = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong seqGenerator = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.Map<String, java.util.Map<String, String>> metadataCache;

    public ImageService() {
        ConfigService configService = ConfigService.getInstance();
        this.imageCache = new ImageCache(configService.getConfig().getCacheSize());
        this.executorService = new ThreadPoolExecutor(
            configService.getConfig().getThreadPoolSize(),
            configService.getConfig().getThreadPoolSize(),
            0L, java.util.concurrent.TimeUnit.MILLISECONDS,
            new java.util.concurrent.PriorityBlockingQueue<Runnable>()
        );
        this.thumbnailExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
        
        int metaCacheSize = configService.getConfig().getMetadataCacheSize();
        this.metadataCache = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, java.util.Map<String, String>>(metaCacheSize + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, java.util.Map<String, String>> eldest) {
                    return size() > metaCacheSize;
                }
            }
        );
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
            ensureExifRotation(imageFile);
        } else {
            // Load image from file

            try (InputStream fis = Files.newInputStream(imageFile.getFile().toPath())) {
                int size = ConfigService.getInstance().getConfig().getImageQualityPx();
                image = new Image(fis, size, 0, true, ConfigService.getInstance().getConfig().isSmooth());
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
     * Loads thumbnail version of an image file (always uses main image cache)
     */
    public Image loadThumbnail(ImageFile imageFile) throws IOException {
        return loadImage(imageFile);
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
        return getCachedImage(imageFile);
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
                future.cancel(true); // Cancel task immediately
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

            // Submit loading task as a FutureTask with PriorityRunnable
            java.util.concurrent.FutureTask<Void> futureTask = new java.util.concurrent.FutureTask<>(() -> {
                try {
                    if (Thread.currentThread().isInterrupted()) return null;
                    // 1. Pre-cache full image (also used as thumbnail)
                    loadImage(imageFile);

                    if (Thread.currentThread().isInterrupted()) return null;
                    // 2. Pre-cache metadata
                    try {
                        getOrLoadMetadata(imageFile);
                    } catch (Exception e) {
                        System.err.println("Failed to pre-cache metadata: " + imageFile.getName() + " - " + e.getMessage());
                    }

                    if (progress != null)
                        progress.accept(executorService.getActiveCount() - 1);
                } catch (IOException e) {
                    System.err.println("Failed to pre-cache image: " + imageFile.getName() +
                            " - " + e.getMessage());
                } finally {
                    pendingTasks.remove(path);
                }
                return null;
            });

            PriorityRunnable priorityRunnable = new PriorityRunnable(futureTask, 0, seqGenerator.getAndIncrement());
            executorService.execute(priorityRunnable);
            pendingTasks.put(path, futureTask);
        }
    }

    /**
     * Cancels all currently pending pre-cache tasks
     */
    public void cancelAllPendingPreCacheTasks() {
        pendingTasks.forEach((path, future) -> {
            if (future != null) {
                future.cancel(true);
            }
        });
        pendingTasks.clear();
    }

    /**
     * Loads image asynchronously and returns a Future
     */
    public Future<Image> loadImageAsync(ImageFile imageFile) {
        java.util.concurrent.FutureTask<Image> futureTask = new java.util.concurrent.FutureTask<>(() -> loadImage(imageFile));
        PriorityRunnable priorityRunnable = new PriorityRunnable(futureTask, 10, seqGenerator.getAndIncrement());
        executorService.execute(priorityRunnable);
        return futureTask;
    }

    public void submitTask(Runnable task) {
        PriorityRunnable priorityRunnable = new PriorityRunnable(task, 10, seqGenerator.getAndIncrement());
        executorService.execute(priorityRunnable);
    }

    public void submitThumbnailTask(Runnable task) {
        thumbnailExecutor.submit(task);
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
     * Clears the image and metadata cache for the specified file
     */
    public void clearCache(ImageFile currentImageFile) {
        if (currentImageFile != null) {
            imageCache.remove(currentImageFile.getPath());
            metadataCache.remove(currentImageFile.getPath());
        }
    }

    /**
     * Clears all images from the cache
     */
    public void clearImageCache() {
        imageCache.clear();
    }

    /**
     * Gets cached metadata if available
     */
    public java.util.Map<String, String> getMetadata(ImageFile imageFile) {
        if (imageFile == null) return null;
        return metadataCache.get(imageFile.getPath());
    }

    /**
     * Puts metadata in the cache
     */
    public void putMetadata(ImageFile imageFile, java.util.Map<String, String> metadata) {
        if (imageFile == null || metadata == null) return;
        metadataCache.put(imageFile.getPath(), metadata);
    }

    /**
     * Gets metadata from the cache, or loads it if not present
     */
    public java.util.Map<String, String> getOrLoadMetadata(ImageFile imageFile) {
        if (imageFile == null) return null;
        String path = imageFile.getPath();
        java.util.Map<String, String> cached = metadataCache.get(path);
        if (cached == null) {
            cached = ImageUtils.getMetadataMap(imageFile.getFile());
            metadataCache.put(path, cached);
        }
        return cached;
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
        thumbnailExecutor.shutdown();
        imageCache.clear();
        metadataCache.clear();
    }

    /**
     * Priority Runnable wrapper for the priority thread pool executor
     */
    public static class PriorityRunnable implements Runnable, Comparable<PriorityRunnable> {
        private final Runnable runnable;
        private final int priority;
        private final long seq;

        public PriorityRunnable(Runnable runnable, int priority, long seq) {
            this.runnable = runnable;
            this.priority = priority;
            this.seq = seq;
        }

        @Override
        public void run() {
            runnable.run();
        }

        @Override
        public int compareTo(PriorityRunnable other) {
            int diff = Integer.compare(other.priority, this.priority); // High priority first
            if (diff != 0) {
                return diff;
            }
            return Long.compare(this.seq, other.seq); // FIFO order for equal priority
        }
    }
}