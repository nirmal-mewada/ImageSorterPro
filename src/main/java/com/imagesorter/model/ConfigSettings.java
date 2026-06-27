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
    private boolean smooth = true;
    private boolean preloadVideos = false;
    private boolean showLeftPane = true;
    private boolean showRightPane = true;
    private boolean showThumbnailBox = true;
    private int cacheSize;
    private int prevCache;
    private int nextCache;
    private int undoSize;
    private int threadPoolSize;
    private String trashFolderPath;
    private int thumbnailSize;
    private int thumbnailCount;
    private int imageQualityPx;
    private int metadataCacheSize;
    private HashSet<String> supportedExtensions = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif","mp4"
            ));
    private HashSet<String> supportedVideoExtensions = new HashSet<>(Arrays.asList(
            "mp4"
    ));
    private java.util.List<String> bookmarkedFolders = new java.util.ArrayList<>();
    private String actionMode = "MOVE";
    private String theme = "System";
    private String sortField = "Name";
    private String sortOrder = "Ascending";
    private java.util.List<SortingRule> sortingRules = new java.util.ArrayList<>();
    
    public ConfigSettings() {
        this.allFolderPaths = new HashMap<>();
        this.confirmDelete = true;
        this.smooth = true;
        this.showLeftPane = true;
        this.showRightPane = true;
        this.showThumbnailBox = true;
        this.cacheSize = 20;
        this.prevCache = 5;
        this.nextCache= 8;
        this.threadPoolSize = 4;
        defaultFileChooseLocation = "";
        undoSize = 50;
        this.thumbnailSize = 150;
        this.thumbnailCount = 9;
        this.imageQualityPx = 1024;
        this.metadataCacheSize = 5000;
        this.bookmarkedFolders = new java.util.ArrayList<>();
        this.actionMode = "MOVE";
        this.theme = "System";
        this.sortField = "Name";
        this.sortOrder = "Ascending";
        this.sortingRules = new java.util.ArrayList<>();
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
    
    public boolean isSmooth() { return smooth; }
    public void setSmooth(boolean smooth) { this.smooth = smooth; }

    public boolean isPreloadVideos() { return preloadVideos; }
    public void setPreloadVideos(boolean preloadVideos) { this.preloadVideos = preloadVideos; }

    public boolean isShowLeftPane() { return showLeftPane; }
    public void setShowLeftPane(boolean showLeftPane) { this.showLeftPane = showLeftPane; }

    public boolean isShowRightPane() { return showRightPane; }
    public void setShowRightPane(boolean showRightPane) { this.showRightPane = showRightPane; }

    public boolean isShowThumbnailBox() { return showThumbnailBox; }
    public void setShowThumbnailBox(boolean showThumbnailBox) { this.showThumbnailBox = showThumbnailBox; }
    
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

    public int getMetadataCacheSize() {
        return metadataCacheSize;
    }

    public void setMetadataCacheSize(int metadataCacheSize) {
        this.metadataCacheSize = Math.max(10, Math.min(100000, metadataCacheSize));
    }

    @Override
    public String toString() {
        return "ConfigSettings{" +
                "allFolderPaths=" + allFolderPaths +
                ", lastOpenedFolder='" + lastOpenedFolder + '\'' +
                ", defaultFileChooseLocation='" + defaultFileChooseLocation + '\'' +
                ", confirmDelete=" + confirmDelete +
                ", smooth=" + smooth +
                ", showLeftPane=" + showLeftPane +
                ", showRightPane=" + showRightPane +
                ", showThumbnailBox=" + showThumbnailBox +
                ", cacheSize=" + cacheSize +
                ", prevCache=" + prevCache +
                ", nextCache=" + nextCache +
                ", undoSize=" + undoSize +
                ", threadPoolSize=" + threadPoolSize +
                ", trashFolderPath='" + trashFolderPath + '\'' +
                ", thumbnailSize=" + thumbnailSize +
                ", thumbnailCount=" + thumbnailCount +
                ", imageQualityPx=" + imageQualityPx +
                ", metadataCacheSize=" + metadataCacheSize +
                ", supportedExtensions=" + supportedExtensions +
                '}';
    }

    public HashSet<String> getSupportedExtensions() {
        return supportedExtensions;
    }

    public void setSupportedExtensions(HashSet<String> supportedExtensions) {
        this.supportedExtensions = supportedExtensions;
    }
    public HashSet<String> getSupportedVideoExtensions() {
        return supportedVideoExtensions;
    }

    public void setSupportedVideoExtensions(HashSet<String> supportedVideoExtensions) {
        this.supportedVideoExtensions = supportedVideoExtensions;
    }

    public java.util.List<String> getBookmarkedFolders() {
        if (bookmarkedFolders == null) {
            bookmarkedFolders = new java.util.ArrayList<>();
        }
        return bookmarkedFolders;
    }

    public void setBookmarkedFolders(java.util.List<String> bookmarkedFolders) {
        this.bookmarkedFolders = bookmarkedFolders;
    }

    public String getActionMode() {
        if (actionMode == null) {
            actionMode = "MOVE";
        }
        return actionMode;
    }

    public void setActionMode(String actionMode) {
        this.actionMode = actionMode;
    }

    public String getTheme() {
        if (theme == null || theme.trim().isEmpty()) {
            theme = "System";
        }
        // Normalize legacy AtlantaFX theme names to new appearance values
        if (!theme.equals("Light") && !theme.equals("Dark") && !theme.equals("System")) {
            theme = theme.contains("Dark") || theme.contains("dark") ? "Dark" : "System";
        }
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }



    public java.util.List<SortingRule> getSortingRules() {
        if (sortingRules == null) sortingRules = new java.util.ArrayList<>();
        return sortingRules;
    }
    public void setSortingRules(java.util.List<SortingRule> sortingRules) {
        this.sortingRules = sortingRules;
    }

    public String getSortField() {
        if (sortField == null || sortField.trim().isEmpty()) {
            sortField = "Name";
        }
        return sortField;
    }
    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        if (sortOrder == null || sortOrder.trim().isEmpty()) {
            sortOrder = "Ascending";
        }
        return sortOrder;
    }
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }
}
