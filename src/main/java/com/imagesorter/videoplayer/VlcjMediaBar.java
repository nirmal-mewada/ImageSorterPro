package com.imagesorter.videoplayer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public class VlcjMediaBar extends HBox {

    private final EmbeddedMediaPlayer player;
    private final VlcjPlayer vlcjPlayer;
    private final Button playButton = new Button("||");
    private final Slider timeSlider = new Slider(0, 100, 0);
    private final Label timeLabel = new Label("00:00 / 00:00");
    private final Slider volSlider = new Slider(0, 200, 100);
    private boolean seeking = false;

    private final MediaPlayerEventAdapter eventListener = new MediaPlayerEventAdapter() {
        @Override
        public void playing(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
            Platform.runLater(() -> playButton.setText("||"));
        }

        @Override
        public void paused(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
            Platform.runLater(() -> playButton.setText("▶"));
        }

        @Override
        public void stopped(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
            Platform.runLater(() -> playButton.setText("▶"));
        }

        @Override
        public void timeChanged(uk.co.caprica.vlcj.player.base.MediaPlayer mp, long newTime) {
            Platform.runLater(() -> {
                if (!seeking) {
                    long total = player.status().length();
                    if (total > 0) {
                        timeSlider.setValue((double) newTime / total * 100.0);
                    }
                    timeLabel.setText(formatTime(newTime) + " / " + formatTime(total));
                }
            });
        }
    };

    public VlcjMediaBar(EmbeddedMediaPlayer player, VlcjPlayer vlcjPlayer) {
        this.player = player;
        this.vlcjPlayer = vlcjPlayer;

        playButton.setMinWidth(40);
        playButton.setOnAction(e -> vlcjPlayer.pauseOrPlay());

        timeSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(timeSlider, Priority.ALWAYS);
        timeSlider.setOnMousePressed(e -> seeking = true);
        timeSlider.setOnMouseReleased(e -> {
            seeking = false;
            float pos = (float) (timeSlider.getValue() / 100.0);
            player.controls().setPosition(pos);
        });

        volSlider.setPrefWidth(80);
        volSlider.valueProperty().addListener((obs, oldVal, newVal) ->
            player.audio().setVolume(newVal.intValue())
        );

        Button rotateButton = new Button("⟳");
        rotateButton.setOnAction(e -> vlcjPlayer.rotate90());

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        setPadding(new Insets(4, 8, 4, 8));
        setStyle("-fx-background-color: #1a1a1a;");
        getChildren().addAll(playButton, timeSlider, timeLabel, volSlider, rotateButton);

        player.events().addMediaPlayerEventListener(eventListener);
    }

    private String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    public void dispose() {
        player.events().removeMediaPlayerEventListener(eventListener);
    }
}
