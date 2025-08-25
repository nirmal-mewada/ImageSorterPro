
package com.imagesorter.model;

import java.io.File;

public class LastAction {
    public enum ActionType {
        MOVE,
        DELETE
    }

    private final ActionType actionType;
    private final File sourceFile;
    private final File destinationFile;

    public LastAction(ActionType actionType, File sourceFile, File destinationFile) {
        this.actionType = actionType;
        this.sourceFile = sourceFile;
        this.destinationFile = destinationFile;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public File getDestinationFile() {
        return destinationFile;
    }
}
