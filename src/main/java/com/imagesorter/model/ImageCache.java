// ImageCache.java
package com.imagesorter.model;

import javafx.scene.image.Image;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU Cache for storing loaded images in memory
 * Implements Least Recently Used eviction policy
 */
public class ImageCache {
    private final int maxSize;
    private final Map<String, CacheEntry> cache;
    
    private static class CacheEntry {
        final Image image;
        final long loadTime;
        final long fileSize;
        long lastAccessed;
        
        CacheEntry(Image image, long fileSize) {
            this.image = image;
            this.fileSize = fileSize;
            this.loadTime = System.currentTimeMillis();
            this.lastAccessed = this.loadTime;
        }
    }
    
    public ImageCache(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
        this.cache = new LinkedHashMap<String, CacheEntry>(maxSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > ImageCache.this.maxSize;
            }
        };
    }
    
    public synchronized void put(String filePath, Image image, long fileSize) {
        if (filePath == null || image == null) {
            return;
        }
        
        cache.put(filePath, new CacheEntry(image, fileSize));
    }
    
    public synchronized Image get(String filePath) {
        CacheEntry entry = cache.get(filePath);
        if (entry != null) {
            entry.lastAccessed = System.currentTimeMillis();
            return entry.image;
        }
        return null;
    }
    
    public synchronized boolean contains(String filePath) {
        return cache.containsKey(filePath);
    }
    
    public synchronized void remove(String filePath) {
        cache.remove(filePath);
    }
    
    public synchronized void clear() {
        cache.clear();
    }
    
    public synchronized int size() {
        return cache.size();
    }
    
    public synchronized boolean isEmpty() {
        return cache.isEmpty();
    }
    
    public synchronized long getTotalMemoryUsage() {
        return cache.values().stream()
                .mapToLong(entry -> entry.fileSize)
                .sum();
    }
    
    public synchronized int getMaxSize() {
        return maxSize;
    }
    
    public synchronized double getCacheHitRatio() {
        // This would require additional tracking of hits/misses
        // For simplicity, we'll return a placeholder
        return cache.isEmpty() ? 0.0 : 0.8; // Placeholder value
    }
    
    /**
     * Removes entries older than the specified time in milliseconds
     */
    public synchronized void removeOldEntries(long maxAgeMillis) {
        long cutoffTime = System.currentTimeMillis() - maxAgeMillis;
        cache.entrySet().removeIf(entry -> 
            entry.getValue().loadTime < cutoffTime);
    }
    
    /**
     * Gets cache statistics for debugging
     */
    public synchronized CacheStats getStats() {
        return new CacheStats(cache.size(), maxSize, getTotalMemoryUsage());
    }
    
    public static class CacheStats {
        public final int currentSize;
        public final int maxSize;
        public final long totalMemoryUsage;
        
        CacheStats(int currentSize, int maxSize, long totalMemoryUsage) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
            this.totalMemoryUsage = totalMemoryUsage;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{size: %d/%d, memory: %d bytes}", 
                currentSize, maxSize, totalMemoryUsage);
        }
    }
}