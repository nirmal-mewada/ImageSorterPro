package com.imagesorter.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.imagesorter.model.ImageFile;
import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ImageUtils {


    /**
     * Checks if a file is a supported image format
     */
    public static boolean isImageFile(ImageService imageService, File file) {
        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex >= 0 && dotIndex < name.length() - 1) {
            String extension = name.substring(dotIndex + 1);
            return ConfigService.getInstance().getConfig().getSupportedExtensions().contains(extension);
        }

        return false;
    }


    public static int displayImageWithExifCorrection(File file) {
        try {

            int rotate = 0;
            // Read EXIF metadata from the image file
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);


            // Apply rotation and scaling based on EXIF orientation
            if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);

                switch (orientation) {
                    case 1: // Normal
                        rotate = 0;
                        break;
                    case 3: // Rotate 180 degrees
//                        imageView.setRotate(180);
                        rotate = 180;
                        break;
                    case 6: // Rotate 90 degrees clockwise
//                        imageView.setRotate(90);
                        rotate = 90;
                        break;
                    case 8: // Rotate 270 degrees clockwise
//                        imageView.setRotate(270);
                        rotate = 270;
                        break;
                    case 2: // Flip horizontally
//                        imageView.setScaleX(-1);
                        break;
                    case 4: // Flip vertically
//                        imageView.setScaleY(-1);
                        break;
                    case 5: // Rotate 90 and flip horizontally
//                        imageView.setRotate(90);
                        rotate = 90;
//                        imageView.setScaleX(-1);
                        break;
                    case 7: // Rotate 270 and flip horizontally
//                        imageView.setRotate(270);
                        rotate = 270;
//                        imageView.setScaleX(-1);
                        break;
                }
            }
            return rotate;
        } catch (IOException | ImageProcessingException | MetadataException e) {
            System.err.println("Error processing image or EXIF data: " + e.getMessage());
        }
        return 0;
    }

    public static BufferedImage rotateImage(BufferedImage image, int angle) {
        if (angle == 0) {
            return image;
        }

        double radians = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        int newWidth = (int) Math.floor(originalWidth * cos + originalHeight * sin);
        int newHeight = (int) Math.floor(originalHeight * cos + originalWidth * sin);

        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, image.getType());
        java.awt.Graphics2D g2d = rotatedImage.createGraphics();

        g2d.translate((newWidth - originalWidth) / 2, (newHeight - originalHeight) / 2);
        g2d.rotate(radians, originalWidth / 2, originalHeight / 2);
        g2d.drawRenderedImage(image, null);
        g2d.dispose();

        return rotatedImage;
    }

}
