package com.imagesorter.videoplayer;

import com.imagesorter.model.ImageFile;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.File;
import java.nio.ByteBuffer;

public class VlcjPlayer extends AbstractVideoPlayer {

    // --- Static VLC detection ------------------------------------------------

    private static volatile Boolean vlcAvailable = null;
    private static volatile String vlcLibPath = null;

    public static boolean isAvailable() {
        if (vlcAvailable == null) {
            synchronized (VlcjPlayer.class) {
                if (vlcAvailable == null) {
                    vlcAvailable = detectVlc();
                }
            }
        }
        return vlcAvailable;
    }

    private static boolean detectVlc() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String libName = os.contains("win") ? "libvlc.dll"
                       : os.contains("mac") ? "libvlc.dylib"
                       : "libvlc.so";

        String[] candidates = {
            "/Applications/VLC.app/Contents/MacOS/lib",
            System.getProperty("user.home") + "/Applications/VLC.app/Contents/MacOS/lib",
            "/usr/lib/x86_64-linux-gnu",
            "/usr/lib64",
            "/usr/local/lib",
        };

        for (String path : candidates) {
            if (new File(path, libName).exists()) {
                // Set jna.library.path so JNA can find libvlc before any vlcj class is loaded
                String existing = System.getProperty("jna.library.path", "");
                if (!existing.contains(path)) {
                    System.setProperty("jna.library.path",
                        existing.isEmpty() ? path : existing + File.pathSeparator + path);
                }
                vlcLibPath = path;
                return true;
            }
        }
        return false;
    }

    // --- Instance -------------------------------------------------------------

    private MediaPlayerFactory factory;
    private EmbeddedMediaPlayer vlcPlayer;
    private ImageView videoView;
    private WritableImage writableImage;
    private VlcjMediaBar mediaBar;
    private AnimationTimer renderTimer;

    private final Object frameLock = new Object();
    private byte[] frameBytes;
    private boolean frameReady = false;
    private volatile int videoWidth;
    private volatile int videoHeight;

    private ObservableDoubleValue containerWidth;
    private ObservableDoubleValue containerHeight;
    private int displayRotation = 0;

    public VlcjPlayer(ImageFile imageFile) {
        videoView = new ImageView();
        videoView.setPreserveRatio(true);
        videoView.setSmooth(true);

        StackPane videoPane = new StackPane(videoView);
        videoPane.setAlignment(Pos.CENTER);
        videoPane.setStyle("-fx-background-color: black;");
        setCenter(videoPane);
        setStyle("-fx-background-color: black;");

        // VLC auto-discovers its plugins directory via its own RPATH from the lib location;
        // passing --plugin-path with a long path triggers an snprintf overflow assertion
        // in VLC's dispatch.c when any plugin fails to load.
        factory = new MediaPlayerFactory("--quiet");

        vlcPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();

        setupVideoSurface();

        mediaBar = new VlcjMediaBar(vlcPlayer, this);
        setBottom(mediaBar);

        startRenderTimer();

        // Begin playback — synchronous start on VLC's side, frame callbacks arrive async
        vlcPlayer.media().play(imageFile.getFile().getAbsolutePath());
    }

    // ---- Video surface (vmem callback) --------------------------------------

    private void setupVideoSurface() {
        BufferFormatCallback bufferFormatCallback = new BufferFormatCallback() {
            @Override
            public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
                videoWidth = sourceWidth;
                videoHeight = sourceHeight;
                synchronized (frameLock) {
                    frameBytes = new byte[sourceWidth * sourceHeight * 4];
                }
                Platform.runLater(() -> {
                    writableImage = new WritableImage(sourceWidth, sourceHeight);
                    videoView.setImage(writableImage);
                });
                return new RV32BufferFormat(sourceWidth, sourceHeight);
            }

            @Override
            public void allocatedBuffers(ByteBuffer[] buffers) {}
        };

        RenderCallback renderCallback = (mp, nativeBuffers, bufferFormat) -> {
            ByteBuffer src = nativeBuffers[0];
            src.rewind();
            synchronized (frameLock) {
                if (frameBytes != null && src.remaining() == frameBytes.length) {
                    src.get(frameBytes);
                    frameReady = true;
                }
            }
        };

        vlcPlayer.videoSurface().set(new CallbackVideoSurface(
            bufferFormatCallback,
            renderCallback,
            true,
            VideoSurfaceAdapters.getVideoSurfaceAdapter()
        ));
    }

    // ---- Render timer (FX thread) -------------------------------------------

    private void startRenderTimer() {
        renderTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                synchronized (frameLock) {
                    if (!frameReady || writableImage == null || frameBytes == null) return;
                    frameReady = false;
                    try {
                        writableImage.getPixelWriter().setPixels(
                            0, 0, videoWidth, videoHeight,
                            PixelFormat.getByteBgraInstance(),
                            frameBytes, 0, videoWidth * 4
                        );
                    } catch (Exception ignored) {
                        // Size mismatch during resolution change — next frame will succeed
                    }
                }
            }
        };
        renderTimer.start();
    }

    // ---- Fit bindings -------------------------------------------------------

    private void applyFitBindings() {
        videoView.fitWidthProperty().unbind();
        videoView.fitHeightProperty().unbind();
        if (containerWidth == null || containerHeight == null || mediaBar == null) return;

        DoubleBinding videoH = Bindings.createDoubleBinding(
            () -> Math.max(0, containerHeight.get() - mediaBar.getHeight()),
            containerHeight, mediaBar.heightProperty()
        );

        if (displayRotation == 90 || displayRotation == 270) {
            videoView.fitWidthProperty().bind(videoH);
            videoView.fitHeightProperty().bind(containerWidth);
        } else {
            videoView.fitWidthProperty().bind(containerWidth);
            videoView.fitHeightProperty().bind(videoH);
        }
    }

    // ---- AbstractVideoPlayer interface --------------------------------------

    @Override
    public void bindToContainer(ObservableDoubleValue w, ObservableDoubleValue h) {
        containerWidth = w;
        containerHeight = h;
        applyFitBindings();
    }

    @Override
    public void dispose() {
        if (renderTimer != null) {
            renderTimer.stop();
            renderTimer = null;
        }
        if (mediaBar != null) {
            mediaBar.dispose();
            mediaBar = null;
        }
        if (vlcPlayer != null) {
            vlcPlayer.controls().stop();
            final EmbeddedMediaPlayer p = vlcPlayer;
            vlcPlayer = null;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { p.release(); } catch (Exception ignored) {}
            });
        }
        if (factory != null) {
            final MediaPlayerFactory f = factory;
            factory = null;
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try { f.release(); } catch (Exception ignored) {}
            });
        }
        videoView = null;
        writableImage = null;
        synchronized (frameLock) {
            frameBytes = null;
        }
    }

    @Override
    public void pauseOrPlay() {
        if (vlcPlayer == null) return;
        if (vlcPlayer.status().isPlaying()) {
            vlcPlayer.controls().pause();
        } else {
            vlcPlayer.controls().play();
        }
    }

    @Override
    public boolean isPlaying() {
        return vlcPlayer != null && vlcPlayer.status().isPlaying();
    }

    @Override
    public void rotate90() {
        displayRotation = (displayRotation + 90) % 360;
        switch (displayRotation) {
            case 90  -> videoView.setRotate(270);
            case 270 -> videoView.setRotate(90);
            case 180 -> videoView.setRotate(180);
            default  -> videoView.setRotate(0);
        }
        applyFitBindings();
    }

    @Override
    public int getVideoWidth() { return videoWidth; }

    @Override
    public int getVideoHeight() { return videoHeight; }

    @Override
    public double getDurationSeconds() {
        if (vlcPlayer == null) return 0;
        long ms = vlcPlayer.status().length();
        return ms > 0 ? ms / 1000.0 : 0;
    }

    @Override
    public void addReadyListener(Runnable onReady) {
        if (vlcPlayer == null) return;
        vlcPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(uk.co.caprica.vlcj.player.base.MediaPlayer mp) {
                Platform.runLater(onReady);
                mp.events().removeMediaPlayerEventListener(this);
            }
        });
    }
}
