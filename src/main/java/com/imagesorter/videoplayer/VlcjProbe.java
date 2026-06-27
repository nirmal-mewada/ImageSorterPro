package com.imagesorter.videoplayer;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

/**
 * Minimal VLC initialization probe executed in a subprocess by VlcjPlayer.isAvailable().
 * If VLC triggers assert()/abort() during plugin loading, only this subprocess dies
 * (exit code != 0), leaving the main JVM unaffected and allowing graceful fallback.
 */
public class VlcjProbe {
    public static void main(String[] args) {
        try {
            MediaPlayerFactory factory = new MediaPlayerFactory("--quiet");
            EmbeddedMediaPlayer player = factory.mediaPlayers().newEmbeddedMediaPlayer();
            player.release();
            factory.release();
            System.exit(0); // success
        } catch (Exception e) {
            System.exit(2); // java-level failure
        }
        // SIGABRT from VLC assert → exit code 134 (signal 6 + 128 on macOS/Linux)
    }
}
