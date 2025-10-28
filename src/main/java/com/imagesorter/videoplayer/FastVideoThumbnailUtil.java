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
        try (InputStream fis = ImageService.class.getClassLoader().getResourceAsStream("video.png")) {
            int size = ConfigService.getInstance().getConfig().getImageQualityPx();
            return new Image(fis, size, 0, true, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static Image createVideoThumbnail1(File videoFile) {
        if (videoFile == null || !videoFile.exists()) return null;

        try {
            File tempPng = Files.createTempFile("thumb_", ".png").toFile();
            String[] cmd = {
                    "D:\\Downloads\\ffmpeg-8.0-essentials_build\\ffmpeg-8.0-essentials_build\\bin\\ffmpeg.exe",
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

            Image img = new Image(tempPng.toURI().toString());
            tempPng.deleteOnExit();
            return img;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
