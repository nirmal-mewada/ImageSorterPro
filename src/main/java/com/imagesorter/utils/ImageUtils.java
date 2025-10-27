package com.imagesorter.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.mp4.media.Mp4VideoDirectory;
import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

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
    public static int getVideoRotation(File file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            for (Directory directory : metadata.getDirectories()) {
                if (directory.containsTag(Mp4VideoDirectory.TAG_ROTATION)) {
                    return directory.getInt(Mp4VideoDirectory.TAG_ROTATION);
                }
            }
        } catch (Exception e) {
            System.err.println("Unable to read rotation metadata: " + e.getMessage());
        }
        return 0; // default: no rotation
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

    public static void rotateExifOrientation(File jpegFile, boolean rotateRight)
            throws IOException, ImageReadException, ImageWriteException {

        // Read metadata
        ImageMetadata metadata = Imaging.getMetadata(jpegFile);

        TiffOutputSet outputSet = null;
        int currentOrientation = 1; // default normal

        if (metadata instanceof JpegImageMetadata) {
            TiffImageMetadata exif = ((JpegImageMetadata)metadata).getExif();
            if (exif != null) {
                outputSet = exif.getOutputSet();

                TiffField orientationField =
                        exif.findField(TiffTagConstants.TIFF_TAG_ORIENTATION, true);
                if (orientationField != null) {
                    currentOrientation = orientationField.getIntValue();
                }
            }
        }

        if (outputSet == null) {
            outputSet = new TiffOutputSet();
        }

        // Compute new orientation
        int newOrientation;
        if (rotateRight) {
            // Right (CW): 1→6, 6→3, 3→8, 8→1
            switch (currentOrientation) {
                case 6 -> newOrientation = 3;
                case 3 -> newOrientation = 8;
                case 8 -> newOrientation = 1;
                default -> newOrientation = 6;
            }
        } else {
            // Left (CCW): 1→8, 8→3, 3→6, 6→1
            switch (currentOrientation) {
                case 8 -> newOrientation = 3;
                case 3 -> newOrientation = 6;
                case 6 -> newOrientation = 1;
                default -> newOrientation = 8;
            }
        }

        // Update EXIF tag
        TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
        exifDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
        exifDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short) newOrientation);

        System.out.println("Rotating: "+currentOrientation+" -> "+newOrientation);

        // Write back losslessly
        File tempFile = new File("C:\\Users\\Nirmal\\Desktop\\"+jpegFile.getName());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            new ExifRewriter().updateExifMetadataLossless(jpegFile, fos, outputSet);
        }

        Files.copy(tempFile.toPath(), jpegFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }


    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }

}
