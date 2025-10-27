// ConfigSettings.java
package com.imagesorter.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Configuration settings for the application
 * Stores folder mappings for hotkeys 1-9 and a-z
 */
public class ConfigSettings {
    private Map<String, String> allFolderPaths;
    private String lastOpenedFolder;
    private String defaultFileChooseLocation;
    private boolean confirmDelete;
    private int cacheSize;
    private int prevCache;
    private int nextCache;
    private int undoSize;
    private int threadPoolSize;
    private String trashFolderPath;
    private int thumbnailSize;
    private int thumbnailCount;
     private int imageQualityPx;
    private HashSet<String> supportedExtensions = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif","mp4"
            ));
    
    public ConfigSettings() {
        this.allFolderPaths = new HashMap<>();
        this.confirmDelete = true;
        this.cacheSize = 20;
        this.prevCache = 5;
        this.nextCache= 8;
        this.threadPoolSize = 4;
        defaultFileChooseLocation = "";
        undoSize = 50;
        this.thumbnailSize = 150;
        this.thumbnailCount = 9;
        this.imageQualityPx = 1024;
    }
    
    public String getFolderPath(String hotkey) {
        return allFolderPaths.get(hotkey);
    }
    
    public void setFolderPath(String hotkey, String path) {
        if (path == null || path.trim().isEmpty()) {
            allFolderPaths.remove(hotkey);
        } else {
            allFolderPaths.put(hotkey, path.trim());
        }
    }
    
    public Map<String, String> getAllFolderPaths() {
        return new HashMap<>(allFolderPaths);
    }

    public void setAllFolderPaths(Map<String, String> folderPaths) {
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

    public String getDefaultFileChooseLocation() {
        return defaultFileChooseLocation;
    }

    public void setDefaultFileChooseLocation(String defaultFileChooseLocation) {
        this.defaultFileChooseLocation = defaultFileChooseLocation;
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

    public String getTrashFolderPath() {
        return trashFolderPath;
    }

    public void setTrashFolderPath(String trashFolderPath) {
        this.trashFolderPath = trashFolderPath;
    }

    public int getUndoSize() {
        return undoSize;
    }

    public void setUndoSize(int undoSize) {
        this.undoSize = undoSize;
    }

    public int getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public int getThumbnailCount() {
        return thumbnailCount;
    }

    public void setThumbnailCount(int thumbnailCount) {
        this.thumbnailCount = thumbnailCount;
    }

    public int getImageQualityPx() {
        return imageQualityPx;
    }

    public void setImageQualityPx(int imageQualityPx) {
        this.imageQualityPx = imageQualityPx;
    }

    @Override
    public String toString() {
        return "ConfigSettings{" +
                "allFolderPaths=" + allFolderPaths +
                ", lastOpenedFolder='" + lastOpenedFolder + '\'' +
                ", defaultFileChooseLocation='" + defaultFileChooseLocation + '\'' +
                ", confirmDelete=" + confirmDelete +
                ", cacheSize=" + cacheSize +
                ", prevCache=" + prevCache +
                ", nextCache=" + nextCache +
                ", undoSize=" + undoSize +
                ", threadPoolSize=" + threadPoolSize +
                ", trashFolderPath='" + trashFolderPath + '\'' +
                ", thumbnailSize=" + thumbnailSize +
                ", thumbnailCount=" + thumbnailCount +
                ", imageQualityPx=" + imageQualityPx +
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
