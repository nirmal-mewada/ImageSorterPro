// ConfigSettings.java
package com.imagesorter.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration settings for the application
 * Stores folder mappings for hotkeys 1-9
 */
public class ConfigSettings {
    private Map<Integer, String> folderPaths;
    private String lastOpenedFolder;
    private boolean confirmDelete;
    private int cacheSize;
    
    public ConfigSettings() {
        this.folderPaths = new HashMap<>();
        this.confirmDelete = true;
        this.cacheSize = 20; // Default cache size for images
    }
    
    public String getFolderPath(int hotkeyNumber) {
        if (hotkeyNumber < 1 || hotkeyNumber > 9) {
            throw new IllegalArgumentException("Hotkey number must be between 1 and 9");
        }
        return folderPaths.get(hotkeyNumber);
    }
    
    public void setFolderPath(int hotkeyNumber, String path) {
        if (hotkeyNumber < 1 || hotkeyNumber > 9) {
            throw new IllegalArgumentException("Hotkey number must be between 1 and 9");
        }
        if (path == null || path.trim().isEmpty()) {
            folderPaths.remove(hotkeyNumber);
        } else {
            folderPaths.put(hotkeyNumber, path.trim());
        }
    }
    
    public Map<Integer, String> getAllFolderPaths() {
        return new HashMap<>(folderPaths);
    }
    
    public void clearAllFolderPaths() {
        folderPaths.clear();
    }
    
    public boolean hasConfiguredFolders() {
        return !folderPaths.isEmpty();
    }
    
    public int getConfiguredFolderCount() {
        return folderPaths.size();
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
    
    @Override
    public String toString() {
        return "ConfigSettings{" +
                "folderPaths=" + folderPaths +
                ", lastOpenedFolder='" + lastOpenedFolder + '\'' +
                ", confirmDelete=" + confirmDelete +
                ", cacheSize=" + cacheSize +
                '}';
    }
}