package com.imagesorter.videoplayer;

import com.imagesorter.model.ImageFile;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.net.MalformedURLException;

public class Player extends BorderPane {
    private Media media;
    public MediaPlayer player;
    private MediaView view;
    private StackPane mpane;
    private MediaBar bar;
    ImageFile currentImageFile;

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

        // Handle rotation (normalize)
        setRotation();
        // Layout
        setCenter(mpane);
        bar = new MediaBar(player,this);
        setBottom(bar);

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
    public void rotate90() {
        Integer exifRotate = currentImageFile.getExifRotate();
        exifRotate = (exifRotate + 90) % 360;
        currentImageFile.setExifRotate(exifRotate);
        setRotation();
    }

    public void setRotation() {
        int rotation = ((currentImageFile.getExifRotate() % 360) + 360) % 360;
        switch (rotation) {
            case 90 -> {
                view.setRotate(270);
//                view.setScaleY(-1); // flips vertically to correct upside-down
            }
            case 270 -> {
                view.setRotate(90);
//                view.setScaleX(-1); // flips horizontally to correct upside-down
            }
            case 180 -> {
                view.setRotate(180);
            }
            default -> {
                view.setRotate(0);
            }
        }

        // Fit bindings (handle both portrait & landscape)
        if (rotation == 90 || rotation == 270) {
            view.fitWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> mpane.getHeight(),
                    mpane.heightProperty()
            ));
            view.fitHeightProperty().bind(Bindings.createDoubleBinding(
                    () -> mpane.getWidth(),
                    mpane.widthProperty()
            ));
        } else {
            view.fitWidthProperty().bind(mpane.widthProperty());
            view.fitHeightProperty().bind(mpane.heightProperty());
        }
    }

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

    public void play() {
//        player.play();
    }
}
