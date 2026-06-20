package com.imagesorter.videoplayer;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

public class MediaBar extends HBox { // MediaBar extends Horizontal Box

    // introducing Sliders
    Button rotate = new Button("⟳"); // For rotating the video

    Slider time = new Slider(); // Slider for time
    Slider vol = new Slider(); // Slider for volume
    Button PlayButton = new Button("||"); // For pausing the player
    Label volume = new Label("Volume: ");
    MediaPlayer mediaPlayer;
    private final Label playTime = new Label();

    Player player;

    public MediaBar(MediaPlayer mediaPlayer, Player player)
    { // Default constructor taking
        // the MediaPlayer object
        this.mediaPlayer = mediaPlayer;
        this.player = player;

        rotate.setFocusTraversable(false);
        PlayButton.setFocusTraversable(false);
        time.setFocusTraversable(false);
        vol.setFocusTraversable(false);

        rotate.setStyle("-fx-background-color: #8cb6f5; ");

        setAlignment(Pos.CENTER); // setting the HBox to center
        setPadding(new Insets(5, 10, 5, 10));
        // Settih the preference for volume bar
        vol.setPrefWidth(70);
        vol.setMinWidth(30);
        vol.setValue(100);
        HBox.setHgrow(time, Priority.ALWAYS);
        PlayButton.setPrefWidth(30);

        // Adding the components to the bottom

        getChildren().add(rotate);
        getChildren().add(PlayButton); // Playbutton
        getChildren().add(time);
        getChildren().add(playTime);
        getChildren().add(volume); // volume slider
        getChildren().add(vol);

        // Adding Functionality
        // to play the media player
        PlayButton.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e)
            {
                Status status = MediaBar.this.mediaPlayer.getStatus(); // To get the status of Player
                if (status == status.PLAYING) {

                    // If the status is Video playing
                    if (MediaBar.this.mediaPlayer.getCurrentTime().greaterThanOrEqualTo(MediaBar.this.mediaPlayer.getTotalDuration())) {

                        // If the player is at the end of video
                        MediaBar.this.mediaPlayer.seek(MediaBar.this.mediaPlayer.getStartTime()); // Restart the video
                        MediaBar.this.mediaPlayer.play();
                    }
                    else {
                        // Pausing the player
                        MediaBar.this.mediaPlayer.pause();

                        PlayButton.setText(">");
                    }
                } // If the video is stopped, halted or paused
                if (status == Status.HALTED || status == Status.STOPPED || status == Status.PAUSED) {
                    MediaBar.this.mediaPlayer.play(); // Start the video
                    PlayButton.setText("||");
                }
            }
        });

        // Providing functionality to time slider
        this.mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov)
            {
                updatesValues();
            }
        });

        // Inorder to jump to the certain part of video
        time.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov)
            {
                if (time.isPressed()) { // It would set the time
                    // as specified by user by pressing
                    MediaBar.this.mediaPlayer.seek(MediaBar.this.mediaPlayer.getMedia().getDuration().multiply(time.getValue() / 100));
                }
            }
        });

        // providing functionality to volume slider
        vol.valueProperty().addListener(new InvalidationListener() {
            public void invalidated(Observable ov)
            {
                if (vol.isPressed()) {
                    MediaBar.this.mediaPlayer.setVolume(vol.getValue() / 100); // It would set the volume
                    // as specified by user by pressing
                }
            }
        });
        rotate.setOnAction((e)->{
            player.rotate90();
        });
    }

    // Outside the constructor
    protected void updatesValues()
    {
        Platform.runLater(new Runnable() {
            public void run()
            {
                // Updating to the new time value
                // This will move the slider while running your video
                time.setValue(mediaPlayer.getCurrentTime().toMillis()/
                        mediaPlayer.getTotalDuration()
                                .toMillis()
                        * 100);
                updateTime();
            }
        });
    }
    private void updateTime() {
        Duration current = mediaPlayer.getCurrentTime();
        Duration total = mediaPlayer.getTotalDuration();
        String currentText = formatTime(current);
        String totalText = total != null && total.greaterThan(Duration.ZERO) ? formatTime(total) : "--:--";
        playTime.setText(currentText + " / " + totalText);
    }

    private String formatTime(Duration duration) {
        int seconds = (int) Math.floor(duration.toSeconds());
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d  ", minutes, seconds);
    }
}