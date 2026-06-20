package com.imagesorter.model;

import java.io.File;

/**
 * Stores information about a staged (deferred) move or copy operation
 */
public class StagedAction {
    public enum Type {
        MOVE,
        COPY
    }

    private final ImageFile imageFile;
    private final File sourceFile;
    private final File destinationFolder;
    private final Type type;
    private final int originalIndex;

    public StagedAction(ImageFile imageFile, File sourceFile, File destinationFolder, Type type, int originalIndex) {
        this.imageFile = imageFile;
        this.sourceFile = sourceFile;
        this.destinationFolder = destinationFolder;
        this.type = type;
        this.originalIndex = originalIndex;
    }

    public ImageFile getImageFile() {
        return imageFile;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public File getDestinationFolder() {
        return destinationFolder;
    }

    public Type getType() {
        return type;
    }

    public int getOriginalIndex() {
        return originalIndex;
    }
}
