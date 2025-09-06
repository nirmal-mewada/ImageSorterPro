package com.imagesorter.utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.imagesorter.service.ConfigService;
import com.imagesorter.service.ImageService;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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

    public static void rotateImageAndUpdateExif(File file, int angle) throws IOException, ImageReadException, ImageWriteException {
        BufferedImage image = ImageIO.read(file);
        BufferedImage rotatedImage = rotateImage(image, angle);

        TiffOutputSet outputSet = null;
        ImageMetadata metadata = Imaging.getMetadata(file);
        if (metadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            if (null != jpegMetadata) {
                TiffImageMetadata exif = jpegMetadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();
                }
            }
        }

        if (null == outputSet) {
            outputSet = new TiffOutputSet();
        }

        TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
        exifDirectory.removeField(TiffTagConstants.TIFF_TAG_ORIENTATION);
        exifDirectory.add(TiffTagConstants.TIFF_TAG_ORIENTATION, (short) 1);

        File tempFile = File.createTempFile("image-", ".jpg");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            new ExifRewriter().updateExifMetadataLossless(rotatedImage, fos, outputSet);
        }
    }
}
