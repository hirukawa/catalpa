package onl.oss.catalpa.model;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.file.FileTypeDirectory;
import com.drew.metadata.gif.GifImageDirectory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.drew.metadata.png.PngDirectory;
import com.drew.metadata.webp.WebpDirectory;
import onl.oss.catalpa.Util;

import javax.imageio.plugins.tiff.TIFFDirectory;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static onl.oss.catalpa.Logger.ERROR;
import static onl.oss.catalpa.Logger.WARN;

public class ImageSize {

    private static final List<String> SUPPORT_EXTENSIONS = Arrays.asList("webp", "png", "gif", "jpg", "jpeg", "bmp");
    private static final Pattern SCALE = Pattern.compile("@(\\d)x");
    private static final Map<Path, ImageSize> cache = new HashMap<>();


    public static ImageSize get(Path path) {
        ImageSize imageSize1 = cache.get(path);
        if (imageSize1 != null) {
            try {
                FileTime lastModifiedTime = Files.getLastModifiedTime(path);
                if (imageSize1.lastModifiedTime.compareTo(lastModifiedTime) == 0) {
                    return imageSize1;
                }
            } catch (IOException e) {
                ERROR(e.getMessage() + ": " + path, e);
            }
        }

        ImageSize imageSize2 = null;
        if (SUPPORT_EXTENSIONS.contains(Util.getFileExtension(path))) {
            try {
                imageSize2 = new ImageSize(path);
            } catch (Exception e) {
                ERROR(e.getMessage() + ": " + path, e);
            }
        }

        if (imageSize2 != null && imageSize2.width > 0 && imageSize2.height > 0) {
            cache.put(path, imageSize2);
            return imageSize2;
        }

        return null;
    }

    private final Path path;
    private final FileTime lastModifiedTime;
    private int width;
    private int height;

    public ImageSize(Path path) throws IOException, ImageProcessingException {
        this.path = path;
        lastModifiedTime = Files.getLastModifiedTime(path);

        Metadata metadata = null;
        try (InputStream in = Files.newInputStream(path)) {
            metadata = ImageMetadataReader.readMetadata(in);
        }

        if (metadata == null) {
            throw new IOException("metadata not found: " + path);
        }

        /*
        //DEBUG PRINT
        System.out.println("path=" + path);
        for (Directory directory : metadata.getDirectories()) {
            System.out.println("  directory=" + directory);
            for (Tag tag : directory.getTags()) {
                System.out.println("    " + tag);
            }
            System.out.println();
        }
        */

        String fileTypeName = null;
        for (FileTypeDirectory directory : metadata.getDirectoriesOfType(FileTypeDirectory.class)) {
            if (directory.containsTag(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_NAME)) {
                fileTypeName = directory.getString(FileTypeDirectory.TAG_DETECTED_FILE_TYPE_NAME);
            }
        }

        if (fileTypeName == null) {
            throw new IOException("file type not found: " + path);
        }

        Integer width = null;
        Integer height = null;

        if (fileTypeName.equalsIgnoreCase("WebP")) {
            for (WebpDirectory directory : metadata.getDirectoriesOfType(WebpDirectory.class)) {
                if (directory.containsTag(WebpDirectory.TAG_IMAGE_WIDTH)) {
                    width = directory.getInteger(WebpDirectory.TAG_IMAGE_WIDTH);
                }
                if (directory.containsTag(WebpDirectory.TAG_IMAGE_HEIGHT)) {
                    height = directory.getInteger(WebpDirectory.TAG_IMAGE_HEIGHT);
                }
            }
            if (width == null) {
                WARN("width not found: " + path);
            }
            if (height == null) {
                WARN("height not found: " + path);
            }
        } else if (fileTypeName.equalsIgnoreCase("PNG")) {
            for (PngDirectory directory : metadata.getDirectoriesOfType(PngDirectory.class)) {
                if (directory.containsTag(PngDirectory.TAG_IMAGE_WIDTH)) {
                    width = directory.getInteger(PngDirectory.TAG_IMAGE_WIDTH);
                }
                if (directory.containsTag(PngDirectory.TAG_IMAGE_HEIGHT)) {
                    height = directory.getInteger(PngDirectory.TAG_IMAGE_HEIGHT);
                }
            }
            if (width == null) {
                WARN("width not found: " + path);
            }
            if (height == null) {
                WARN("height not found: " + path);
            }
        } else if (fileTypeName.equalsIgnoreCase("JPEG")) {
            for (JpegDirectory directory : metadata.getDirectoriesOfType(JpegDirectory.class)) {
                if (directory.containsTag(JpegDirectory.TAG_IMAGE_WIDTH)) {
                    width = directory.getInteger(JpegDirectory.TAG_IMAGE_WIDTH);
                }
                if (directory.containsTag(JpegDirectory.TAG_IMAGE_HEIGHT)) {
                    height = directory.getInteger(JpegDirectory.TAG_IMAGE_HEIGHT);
                }
            }
            if (width == null) {
                WARN("width not found: " + path);
            }
            if (height == null) {
                WARN("height not found: " + path);
            }
        } else if (fileTypeName.equalsIgnoreCase("GIF")) {
            for (GifImageDirectory directory : metadata.getDirectoriesOfType(GifImageDirectory.class)) {
                if (directory.containsTag(GifImageDirectory.TAG_WIDTH)) {
                    width = directory.getInteger(GifImageDirectory.TAG_WIDTH);
                }
                if (directory.containsTag(GifImageDirectory.TAG_HEIGHT)) {
                    height = directory.getInteger(GifImageDirectory.TAG_HEIGHT);
                }
            }
            if (width == null) {
                WARN("width not found: " + path);
            }
            if (height == null) {
                WARN("height not found: " + path);
            }
        }

        if (width != null && height != null) {
            int scale = 1;
            Matcher m = SCALE.matcher(path.getFileName().toString());
            if (m.find()) {
                try {
                    scale = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {}
            }

            if (scale <= 0) {
                throw new IOException("invalid scale: " + path);
            }

            this.width = BigDecimal.valueOf(width).divide(BigDecimal.valueOf(scale), 1, RoundingMode.HALF_EVEN).intValue();
            this.height = BigDecimal.valueOf(height).divide(BigDecimal.valueOf(scale), 1, RoundingMode.HALF_EVEN).intValue();
        }
    }

    public Path getPath() {
        return path;
    }

    public FileTime getLastModifiedTime() {
        return lastModifiedTime;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
