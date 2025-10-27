// ImageFile.java
package com.imagesorter.model;

import com.imagesorter.service.ConfigService;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Represents an image file in the application
 */
public class ImageFile {
    private File file;
    private String name;
    private long size;
    private LocalDateTime lastModified;
    private String extension;

    private Integer exifRotate = null;
    
    public ImageFile(File file) {
        this.file = file;
        this.name = file.getName();
        this.size = file.length();
        this.lastModified = LocalDateTime.ofEpochSecond(
            file.lastModified() / 1000, 0, 
            java.time.ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now())
        );
        
        int dotIndex = name.lastIndexOf('.');
        this.extension = dotIndex >= 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }

    public boolean isVideoFile() {
        return extension.equalsIgnoreCase("mp4");
    }
    
    // Getters
    public File getFile() { return file; }
    public String getName() { return name; }
    public long getSize() { return size; }
    public LocalDateTime getLastModified() { return lastModified; }
    public String getExtension() { return extension; }
    
    public String getPath() {
        return file.getAbsolutePath();
    }
    
    public boolean exists() {
        return file.exists();
    }
    
    public boolean isValidImageFile() {
        return ConfigService.getInstance().getConfig().getSupportedExtensions().contains(extension);
//        return extension.matches("jpg|jpeg|png|gif|bmp|tiff|tif|");
    }

    public Integer getExifRotate() {
        return exifRotate;
    }

    public void setExifRotate(Integer exifRotate) {
        this.exifRotate = exifRotate;
    }

    @Override
    public String toString() {
        return name;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ImageFile imageFile = (ImageFile) obj;
        return file.equals(imageFile.file);
    }
    
    @Override
    public int hashCode() {
        return file.hashCode();
    }
}



