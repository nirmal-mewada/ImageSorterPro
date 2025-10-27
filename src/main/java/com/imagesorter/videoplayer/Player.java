package com.imagesorter.videoplayer;

import com.imagesorter.model.ImageFile;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import java.net.MalformedURLException;

public class Player extends BorderPane // Player class extend BorderPane
        // in order to divide the media
        // player into regions
{
    Media media;
    public MediaPlayer player;
    MediaView view;
    Pane mpane;
    MediaBar bar;

    public Player(ImageFile currentImageFile) throws MalformedURLException { // Default constructor
        String file = currentImageFile.getFile().toURI().toURL().toExternalForm();
        media = new Media(file);
        player = new MediaPlayer(media);
        view = new MediaView(player);
        mpane = new Pane();
        mpane.getChildren().add(view); // Calling the function getChildren

        view.setPreserveRatio(true);
//        int rotation = ((currentImageFile.getExifRotate() % 360) + 360) % 360;
        int rotation = Math.abs(currentImageFile.getExifRotate());

        view.setRotate(rotation);

        if (rotation == 90 || rotation == 270) {
            view.fitWidthProperty().bind(heightProperty());
            view.fitHeightProperty().bind(widthProperty());
        } else {
//            // For 0 or 180 degrees, bindings are standard.
            view.fitWidthProperty().bind(widthProperty());
            view.fitHeightProperty().bind(heightProperty());
        }

        // inorder to add the view
        setCenter(mpane);
        bar = new MediaBar(player); // Passing the player to MediaBar
        setBottom(bar); // Setting the MediaBar at bottom
        setStyle("-fx-background-color:#bfc2c7"); // Adding color to the mediabar
        player.play(); // Making the video play
    }
    public void dispose() {
        try {
            if (player != null) {
                player.stop();
                player.dispose();
                player = null;
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

}
