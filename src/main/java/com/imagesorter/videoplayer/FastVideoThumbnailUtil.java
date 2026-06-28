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
        String osName = System.getProperty("os.name").toLowerCase();
        boolean isWindows = osName.contains("win");
        String exeName = isWindows ? "ffmpeg.exe" : "ffmpeg";

        // 1. Check directory of the running code/JAR
        try {
            java.net.URL codeLocation = FastVideoThumbnailUtil.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeLocation != null) {
                File codeFile = new File(codeLocation.toURI());
                File appDir = codeFile.isDirectory() ? codeFile : codeFile.getParentFile();
                if (appDir != null && appDir.exists()) {
                    // Check same directory as code/JAR
                    File localFfmpeg = new File(appDir, exeName);
                    if (localFfmpeg.exists()) {
                        return localFfmpeg.getAbsolutePath();
                    }
                    
                    // Check parent directory (e.g. if code is in target/classes or target/app/...)
                    File parentDir = appDir.getParentFile();
                    if (parentDir != null && parentDir.exists()) {
                        File parentFfmpeg = new File(parentDir, exeName);
                        if (parentFfmpeg.exists()) {
                            return parentFfmpeg.getAbsolutePath();
                        }
                        
                        // Check sibling app/ directory
                        File siblingAppDir = new File(parentDir, "app");
                        if (siblingAppDir.exists() && siblingAppDir.isDirectory()) {
                            File siblingFfmpeg = new File(siblingAppDir, exeName);
                            if (siblingFfmpeg.exists()) {
                                return siblingFfmpeg.getAbsolutePath();
                            }
                        }

                        // Check sibling ext-dist/ directory (e.g. if running maven layout target/)
                        File siblingExtDist = new File(parentDir, "ext-dist");
                        if (siblingExtDist.exists() && siblingExtDist.isDirectory()) {
                            File siblingFfmpeg = new File(siblingExtDist, exeName);
                            if (siblingFfmpeg.exists()) {
                                return siblingFfmpeg.getAbsolutePath();
                            }
                        }

                        // Check grand-parent sibling ext-dist/ directory (e.g. root folder from target/classes)
                        File grandParentDir = parentDir.getParentFile();
                        if (grandParentDir != null && grandParentDir.exists()) {
                            File grandParentExtDist = new File(grandParentDir, "ext-dist");
                            if (grandParentExtDist.exists() && grandParentExtDist.isDirectory()) {
                                File rootFfmpeg = new File(grandParentExtDist, exeName);
                                if (rootFfmpeg.exists()) {
                                    return rootFfmpeg.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not resolve code source path for FFmpeg check: " + e.getMessage());
        }

        // 2. Check current working directory and ext-dist subdirectory
        try {
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                // Check user.dir/ext-dist/
                File extDistFfmpeg = new File(new File(userDir, "ext-dist"), exeName);
                if (extDistFfmpeg.exists()) {
                    return extDistFfmpeg.getAbsolutePath();
                }

                // Check user.dir
                File workingFfmpeg = new File(userDir, exeName);
                if (workingFfmpeg.exists()) {
                    return workingFfmpeg.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // 3. Check common installation directories
        String[] commonPaths = {
            "/opt/homebrew/bin/ffmpeg",              // Apple Silicon Homebrew
            "/usr/local/bin/ffmpeg",                 // Intel Homebrew / standard macOS
            "/usr/bin/ffmpeg",                       // Debian / Ubuntu / Fedora
            "/bin/ffmpeg",
            "C:\\ffmpeg\\bin\\ffmpeg.exe",           // Windows typical install
            "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe"
        };

        for (String path : commonPaths) {
            if (new File(path).exists()) {
                return path;
            }
        }

        // 4. Fall back to standard PATH environment lookup
        return exeName;
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
