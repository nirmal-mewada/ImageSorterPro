// ConfigSettings.java
package com.imagesorter.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Configuration settings for the application
 * Stores folder mappings for hotkeys 1-9
 */
public class ConfigSettings {
    private Map<Integer, String> allFolderPaths;
    private String lastOpenedFolder;
    private boolean confirmDelete;
    private int cacheSize;
    private int prevCache;
    private int nextCache;
    private int threadPoolSize;
    private HashSet<String> supportedExtensions = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif"
            ));
    
    public ConfigSettings() {
        this.allFolderPaths = new HashMap<>();
        this.confirmDelete = true;
        this.cacheSize = 20;
        this.prevCache = 5;
        this.nextCache= 8;
        this.threadPoolSize = 4;
    }
    
    public String getFolderPath(int hotkeyNumber) {
        if (hotkeyNumber < 1 || hotkeyNumber > 9) {
            throw new IllegalArgumentException("Hotkey number must be between 1 and 9");
        }
        return allFolderPaths.get(hotkeyNumber);
    }
    
    public void setFolderPath(int hotkeyNumber, String path) {
        if (hotkeyNumber < 1 || hotkeyNumber > 9) {
            throw new IllegalArgumentException("Hotkey number must be between 1 and 9");
        }
        if (path == null || path.trim().isEmpty()) {
            allFolderPaths.remove(hotkeyNumber);
        } else {
            allFolderPaths.put(hotkeyNumber, path.trim());
        }
    }
    
    public Map<Integer, String> getAllFolderPaths() {
        return new HashMap<>(allFolderPaths);
    }

    public void setAllFolderPaths(Map<Integer, String> folderPaths) {
        this.allFolderPaths = folderPaths;
    }

    public void clearAllFolderPaths() {
        allFolderPaths.clear();
    }
    
    public boolean hasConfiguredFolders() {
        return !allFolderPaths.isEmpty();
    }
    
    public int getConfiguredFolderCount() {
        return allFolderPaths.size();
    }
    
    // Other getters and setters
    public String getLastOpenedFolder() { return lastOpenedFolder; }
    public void setLastOpenedFolder(String lastOpenedFolder) { this.lastOpenedFolder = lastOpenedFolder; }
    
    public boolean isConfirmDelete() { return confirmDelete; }
    public void setConfirmDelete(boolean confirmDelete) { this.confirmDelete = confirmDelete; }
    
    public int getCacheSize() { return cacheSize; }
    public void setCacheSize(int cacheSize) { 
        this.cacheSize = Math.max(5, Math.min(100, cacheSize)); // Limit cache size
    }

    public int getPrevCache() {
        return prevCache;
    }

    public void setPrevCache(int prevCache) {
        this.prevCache = prevCache;
    }

    public int getNextCache() {
        return nextCache;
    }

    public void setNextCache(int nextCache) {
        this.nextCache = nextCache;
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    @Override
    public String toString() {
        return "ConfigSettings{" +
                "folderPaths=" + allFolderPaths +
                ", lastOpenedFolder='" + lastOpenedFolder + '\'' +
                ", confirmDelete=" + confirmDelete +
                ", cacheSize=" + cacheSize +
                ", prevCache=" + prevCache +
                ", nextCache=" + nextCache +
                ", threadPoolSize=" + threadPoolSize +
                ", supportedExtensions=" + supportedExtensions +
                '}';
    }

    public HashSet<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    public void setSupportedExtensions(HashSet<String> supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }
}