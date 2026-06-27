package com.imagesorter.videoplayer;

import com.imagesorter.model.ImageFile;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.net.MalformedURLException;

public class Player extends AbstractVideoPlayer {
    private Media media;
    public MediaPlayer player;
    private MediaView view;
    private StackPane mpane;
    private MediaBar bar;
    ImageFile currentImageFile;
    private ObservableDoubleValue containerWidth;
    private ObservableDoubleValue containerHeight;

    public Player(ImageFile currentImageFile) throws MalformedURLException {
        // Prepare Media and Player
        this.currentImageFile = currentImageFile;
        String file = currentImageFile.getFile().toURI().toURL().toExternalForm();
        media = new Media(file);
        player = new MediaPlayer(media);
        view = new MediaView(player);
        view.setPreserveRatio(true);

        // --- Container to center video properly ---
        mpane = new StackPane(view);
        mpane.setAlignment(Pos.CENTER);
        mpane.setStyle("-fx-background-color: black;"); // Ensures black background

        // Layout
        setCenter(mpane);
        bar = new MediaBar(player,this);
        setBottom(bar);
        setRotation();

        setStyle("-fx-background-color:#bfc2c7;");
//        player.play();
        player.setOnReady(() -> {
            setRotation();
            player.play();
        });

        // Handle error or stalled media
        player.setOnError(() -> System.err.println("Error: " + player.getError()));
        player.setOnEndOfMedia(() -> player.stop());

    }

    /**
     * Constructor that reuses a pre-created (cached) MediaPlayer instance.
     * This avoids the expensive Media parsing and MediaPlayer initialisation
     * that happens when constructing from scratch.
     *
     * @param currentImageFile the video file being played
     * @param cachedPlayer     a pre-created MediaPlayer (typically from VideoPlayerCache)
     */
    public Player(ImageFile currentImageFile, MediaPlayer cachedPlayer) {
        this.currentImageFile = currentImageFile;
        this.media = cachedPlayer.getMedia();
        this.player = cachedPlayer;
        view = new MediaView(player);
        view.setPreserveRatio(true);

        mpane = new StackPane(view);
        mpane.setAlignment(Pos.CENTER);
        mpane.setStyle("-fx-background-color: black;");

        setCenter(mpane);
        bar = new MediaBar(player, this);
        setBottom(bar);
        setRotation();

        setStyle("-fx-background-color:#bfc2c7;");
        player.setMute(false);
        player.setOnReady(() -> {
            setRotation();
            player.play();
        });

        // If already READY (pre-loaded), play immediately
        if (player.getStatus() == MediaPlayer.Status.READY ||
            player.getStatus() == MediaPlayer.Status.PAUSED ||
            player.getStatus() == MediaPlayer.Status.STOPPED) {
            setRotation();
            player.play();
        }

        player.setOnError(() -> System.err.println("Error: " + player.getError()));
        player.setOnEndOfMedia(() -> player.stop());
    }

    @Override
    public void rotate90() {
        Integer exifRotate = currentImageFile.getExifRotate();
        exifRotate = (exifRotate + 90) % 360;
        currentImageFile.setExifRotate(exifRotate);
        setRotation();
    }

    /**
     * Bind the video fit dimensions to the scroll pane that contains this player.
     * Must be called from the controller after construction — same pattern as imageView
     * which binds to imageScrollPane.widthProperty()/.heightProperty().
     * This avoids the layout cycle: view.fitWidth → mpane.prefWidth (native video
     * resolution) → Player.prefWidth → container resize → view.fitWidth.
     */
    @Override
    public void bindToContainer(ObservableDoubleValue w, ObservableDoubleValue h) {
        containerWidth = w;
        containerHeight = h;
        setRotation();
    }

    @Override
    public void setRotation() {
        if (view == null) return; // Player already disposed; callback arrived late
        if (bar == null) return;  // bar not yet initialized; called again after setBottom()
        Integer exifRotateVal = currentImageFile.getExifRotate();
        int rotation = ((exifRotateVal != null ? exifRotateVal : 0) % 360 + 360) % 360;
        switch (rotation) {
            case 90  -> view.setRotate(270);
            case 270 -> view.setRotate(90);
            case 180 -> view.setRotate(180);
            default  -> view.setRotate(0);
        }

        view.fitWidthProperty().unbind();
        view.fitHeightProperty().unbind();

        if (containerWidth == null || containerHeight == null) return; // bindToContainer not yet called

        DoubleBinding videoH = Bindings.createDoubleBinding(
                () -> Math.max(0, containerHeight.get() - bar.getHeight()),
                containerHeight, bar.heightProperty()
        );

        if (rotation == 90 || rotation == 270) {
            view.fitWidthProperty().bind(videoH);
            view.fitHeightProperty().bind(containerWidth);
        } else {
            view.fitWidthProperty().bind(containerWidth);
            view.fitHeightProperty().bind(videoH);
        }
    }

    @Override
    public void dispose() {
        try {
            if (player != null) {
                player.stop();
                if(view != null){
                    view.setMediaPlayer(null);
                }
                MediaPlayer playerToDispose = player;
                player = null;
                // Offload dispose to a background thread to prevent UI blocking/lag
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                        playerToDispose.dispose();
                    } catch (Exception e) {
                        System.err.println("Error disposing MediaPlayer: " + e.getMessage());
                    }
                });
            }
            media = null;
            if (view != null) {
                view.fitWidthProperty().unbind();
                view.fitHeightProperty().unbind();
                view.setMediaPlayer(null);
                view = null;
            }
            if (mpane != null) {
                mpane.getChildren().clear();
                mpane = null;
            }
            bar = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void play() {}

    @Override
    public void pauseOrPlay() {
        if (player == null) return;
        MediaPlayer.Status status = player.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            player.pause();
        } else {
            player.play();
        }
    }

    @Override
    public boolean isPlaying() {
        return player != null && player.getStatus() == MediaPlayer.Status.PLAYING;
    }

    @Override
    public int getVideoWidth() {
        if (player == null || player.getMedia() == null) return 0;
        return player.getMedia().getWidth();
    }

    @Override
    public int getVideoHeight() {
        if (player == null || player.getMedia() == null) return 0;
        return player.getMedia().getHeight();
    }

    @Override
    public double getDurationSeconds() {
        if (player == null || player.getMedia() == null) return 0;
        javafx.util.Duration d = player.getMedia().getDuration();
        return (d != null && d.greaterThan(javafx.util.Duration.ZERO)) ? d.toSeconds() : 0;
    }

    @Override
    public void addReadyListener(Runnable onReady) {
        if (player == null) return;
        player.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (newStatus == MediaPlayer.Status.READY) {
                Platform.runLater(onReady);
            }
        });
    }
}
