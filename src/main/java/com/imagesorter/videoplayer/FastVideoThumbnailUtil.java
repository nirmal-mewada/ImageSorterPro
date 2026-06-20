package com.imagesorter.videoplayer;

import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;
import javafx.scene.image.Image;

import java.io.*;
import java.nio.file.Files;

public class FastVideoThumbnailUtil {

    /**
     * Extracts a frame from a video file using ffmpeg (must be installed or bundled)
     * Returns JavaFX Image.
     */

    public static Image createVideoThumbnail(File videoFile) {
        if (videoFile == null || !videoFile.exists()) {
            return null;
        }

        // Try extracting a real thumbnail from video
        Image realThumbnail = createVideoThumbnailFromVideo(videoFile);
        if (realThumbnail != null && !realThumbnail.isError()) {
            return realThumbnail;
        }

        // Fallback to static placeholder icon
        try (InputStream fis = FastVideoThumbnailUtil.class.getResourceAsStream("/video.png")) {
            if (fis == null) {
                System.err.println("Warning: /video.png resource not found in classpath.");
                return null;
            }
            int size = ConfigService.getInstance().getConfig().getImageQualityPx();
            return new Image(fis, size, 0, true, false);
        } catch (Exception e) {
            System.err.println("Failed to load fallback video icon: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static Image createVideoThumbnailFromVideo(File videoFile) {
        try {
            File tempPng = Files.createTempFile("thumb_", ".png").toFile();
            String ffmpegPath = "D:\\Downloads\\ffmpeg-8.0-essentials_build\\ffmpeg-8.0-essentials_build\\bin\\ffmpeg.exe";
            if (!new File(ffmpegPath).exists()) {
                // Fallback to global ffmpeg command if the specific folder doesn't exist
                ffmpegPath = "ffmpeg";
            }

            String[] cmd = {
                    ffmpegPath,
                    "-ss", "00:00:01", // capture at 1 second
                    "-i", videoFile.getAbsolutePath(),
                    "-frames:v", "1",
                    "-vf", "scale=320:-1",
                    "-y", tempPng.getAbsolutePath()
            };

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            if (tempPng.exists() && tempPng.length() > 0) {
                Image img = new Image(tempPng.toURI().toString());
                if (!tempPng.delete()) {
                    tempPng.deleteOnExit();
                }
                return img;
            }
        } catch (Exception e) {
            System.err.println("Failed to extract frame using ffmpeg: " + e.getMessage());
        }
        return null;
    }
}
