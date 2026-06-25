package com.imagesorter.videoplayer;

import com.imagesorter.service.ConfigService;
import javafx.scene.image.Image;
import javafx.embed.swing.SwingFXUtils;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;

import java.io.*;
import java.nio.file.Files;
import java.awt.image.BufferedImage;

public class FastVideoThumbnailUtil {

    /**
     * Extracts a frame from a video file using ffmpeg or JCodec fallback
     * Returns JavaFX Image.
     */
    public static Image createVideoThumbnail(File videoFile) {
        if (videoFile == null || !videoFile.exists()) {
            return null;
        }

        // 1. Try extracting a real thumbnail from video using ffmpeg
        Image realThumbnail = createVideoThumbnailFromVideo(videoFile);
        if (realThumbnail != null && !realThumbnail.isError()) {
            return realThumbnail;
        }

        // 2. Try extracting a real thumbnail using JCodec as fallback
        System.out.println("Ffmpeg thumbnail extraction failed or executable not found. Trying JCodec fallback...");
        Image jcodecThumbnail = createVideoThumbnailWithJCodec(videoFile);
        if (jcodecThumbnail != null && !jcodecThumbnail.isError()) {
            return jcodecThumbnail;
        }

        // 3. Fallback to static placeholder icon
        System.out.println("JCodec fallback also failed. Using static video.png placeholder icon...");
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

    private static String getFfmpegExecutablePath() {
        // 1. Check custom path if configured (or windows hardcoded if it exists)
        String windowsPath = "D:\\Downloads\\ffmpeg-8.0-essentials_build\\ffmpeg-8.0-essentials_build\\bin\\ffmpeg.exe";
        if (new File(windowsPath).exists()) {
            return windowsPath;
        }

        // 2. Check common installation directories for macOS/Linux/Unix
        String[] commonPaths = {
            "/opt/homebrew/bin/ffmpeg",  // Apple Silicon Homebrew
            "/usr/local/bin/ffmpeg",     // Intel Homebrew / Standard mac install
            "/usr/bin/ffmpeg",           // Debian/Ubuntu/Fedora standard path
            "/bin/ffmpeg"
        };

        for (String path : commonPaths) {
            if (new File(path).exists()) {
                return path;
            }
        }

        // 3. Fall back to standard PATH environment lookup
        return "ffmpeg";
    }

    private static Image createVideoThumbnailFromVideo(File videoFile) {
        try {
            File tempPng = Files.createTempFile("thumb_", ".png").toFile();
            String ffmpegPath = getFfmpegExecutablePath();

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
            // Keep error message descriptive but concise
            System.err.println("Failed to extract frame using ffmpeg: " + e.getMessage());
        }
        return null;
    }

    private static Image createVideoThumbnailWithJCodec(File videoFile) {
        try {
            // Get frame at second 1 (assuming ~30 fps, frame index 30 is roughly 1 second)
            Picture picture = FrameGrab.getFrameFromFile(videoFile, 30);
            if (picture == null) {
                picture = FrameGrab.getFrameFromFile(videoFile, 1);
            }
            if (picture != null) {
                BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
                if (bufferedImage != null) {
                    return SwingFXUtils.toFXImage(bufferedImage, null);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract frame using JCodec: " + e.getMessage());
        }
        return null;
    }
}
