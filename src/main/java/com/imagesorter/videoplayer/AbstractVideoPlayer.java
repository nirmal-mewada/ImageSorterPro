package com.imagesorter.videoplayer;

import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.layout.BorderPane;

public abstract class AbstractVideoPlayer extends BorderPane {
    public void play() {}
    public void setRotation() {}
    public abstract void bindToContainer(ObservableDoubleValue w, ObservableDoubleValue h);
    public abstract void dispose();
    public abstract void pauseOrPlay();
    public abstract boolean isPlaying();
    public abstract void rotate90();
    public abstract int getVideoWidth();
    public abstract int getVideoHeight();
    public abstract double getDurationSeconds();
    public abstract void addReadyListener(Runnable onReady);
}
