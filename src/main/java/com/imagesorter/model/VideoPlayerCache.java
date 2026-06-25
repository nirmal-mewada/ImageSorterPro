// VideoPlayerCache.java
package com.imagesorter.model;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LRU Cache for pre-created MediaPlayer instances.
 * Pre-loads Media + MediaPlayer objects so that navigating to video files
 * is near-instant instead of waiting for the media pipeline to initialise.
 *
 * <p>Cached players are created in a <em>paused</em> state and muted so they
 * do not consume audio resources while sitting in the cache. When a player
 * is retrieved via {@link #take(String)} it is removed from the cache
 * (single-use) so that the caller owns the lifecycle.</p>
 */
public class VideoPlayerCache {
    private final int maxSize;
    private final Map<String, CacheEntry> cache;

    private static class CacheEntry {
        final Media media;
        final MediaPlayer mediaPlayer;
        final long createTime;

        CacheEntry(Media media, MediaPlayer mediaPlayer) {
            this.media = media;
            this.mediaPlayer = mediaPlayer;
            this.createTime = System.currentTimeMillis();
        }
    }

    public VideoPlayerCache(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
        this.cache = new LinkedHashMap<String, CacheEntry>(maxSize + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                if (size() > VideoPlayerCache.this.maxSize) {
                    disposeEntry(eldest.getValue());
                    return true;
                }
                return false;
            }
        };
    }

    /**
     * Pre-creates a {@link MediaPlayer} for the given video file and stores it
     * in the cache. If an entry already exists for this path it is skipped.
     *
     * <p><strong>Thread safety:</strong> this method is synchronized and may be
     * called from any thread. The {@link Media} constructor is thread-safe and
     * performs the expensive container-parsing work that we want to move off
     * the FX thread.</p>
     *
     * @param filePath absolute path to the video file
     */
    public synchronized void preload(String filePath) {
        if (filePath == null || cache.containsKey(filePath)) {
            return;
        }
        try {
            String url = new File(filePath).toURI().toURL().toExternalForm();
            Media media = new Media(url);
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            // Keep the player muted and paused while it sits in the cache
            mediaPlayer.setMute(true);
            mediaPlayer.setAutoPlay(false);
            cache.put(filePath, new CacheEntry(media, mediaPlayer));
        } catch (MalformedURLException e) {
            System.err.println("VideoPlayerCache: failed to preload " + filePath + " - " + e.getMessage());
        } catch (Exception e) {
            System.err.println("VideoPlayerCache: error preloading " + filePath + " - " + e.getMessage());
        }
    }

    /**
     * Returns {@code true} if a pre-created player exists for the given path.
     */
    public synchronized boolean contains(String filePath) {
        return cache.containsKey(filePath);
    }

    /**
     * Takes (removes) the cached {@link MediaPlayer} for the given path.
     * The caller assumes ownership and is responsible for disposing it.
     *
     * @return the pre-created MediaPlayer, or {@code null} if not cached
     */
    public synchronized MediaPlayer take(String filePath) {
        CacheEntry entry = cache.remove(filePath);
        if (entry != null) {
            // Un-mute before handing to the caller
            entry.mediaPlayer.setMute(false);
            return entry.mediaPlayer;
        }
        return null;
    }

    /**
     * Removes and disposes a single cached entry.
     */
    public synchronized void remove(String filePath) {
        CacheEntry entry = cache.remove(filePath);
        if (entry != null) {
            disposeEntry(entry);
        }
    }

    /**
     * Disposes all cached players and clears the cache.
     */
    public synchronized void clear() {
        List<CacheEntry> entries = new ArrayList<>(cache.values());
        cache.clear();
        for (CacheEntry entry : entries) {
            disposeEntry(entry);
        }
    }

    public synchronized int size() {
        return cache.size();
    }

    public synchronized int getMaxSize() {
        return maxSize;
    }

    private void disposeEntry(CacheEntry entry) {
        try {
            if (entry.mediaPlayer != null) {
                entry.mediaPlayer.stop();
                entry.mediaPlayer.dispose();
            }
        } catch (Exception e) {
            System.err.println("VideoPlayerCache: error disposing player - " + e.getMessage());
        }
    }
}
