package tn.esprit.community.service;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MediaStorageService {
    public Optional<Image> loadImage(String mediaUrl) {
        if (mediaUrl == null || mediaUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            if (mediaUrl.startsWith("file:")) {
                return Optional.of(new Image(mediaUrl, true));
            }

            Path resolved = resolveMediaPath(mediaUrl);
            if (resolved != null && Files.exists(resolved)) {
                return Optional.of(new Image(resolved.toUri().toString(), true));
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    public String storeImage(Path sourceImage) throws IOException {
        Path uploadsDirectory = resolveWritableUploadsDirectory();
        Files.createDirectories(uploadsDirectory);

        String extension = extensionOf(sourceImage.getFileName().toString());
        String fileName = "post-" + UUID.randomUUID() + extension;
        Path destination = uploadsDirectory.resolve(fileName);
        Files.copy(sourceImage, destination, StandardCopyOption.REPLACE_EXISTING);

        Path webPublic = discoverWebPublicDirectory();
        if (webPublic != null && destination.startsWith(webPublic)) {
            return "/uploads/posts/" + fileName;
        }

        return destination.toUri().toString();
    }

    private Path resolveMediaPath(String mediaUrl) {
        if (mediaUrl.startsWith("/")) {
            Path webPublic = discoverWebPublicDirectory();
            if (webPublic != null) {
                return webPublic.resolve(mediaUrl.substring(1).replace("/", File.separator));
            }
            return null;
        }

        Path path = Path.of(mediaUrl);
        return Files.exists(path) ? path : null;
    }

    private Path resolveWritableUploadsDirectory() {
        Path webPublic = discoverWebPublicDirectory();
        if (webPublic != null) {
            return webPublic.resolve("uploads").resolve("posts");
        }

        return Path.of(System.getProperty("user.dir")).resolve("uploads").resolve("posts");
    }

    private Path discoverWebPublicDirectory() {
        String configured = System.getenv("BEKRI_WEB_PUBLIC_DIR");
        if (configured != null && !configured.isBlank()) {
            Path configuredPath = Path.of(configured);
            if (Files.exists(configuredPath)) {
                return configuredPath;
            }
        }

        List<Path> candidates = new ArrayList<>();
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        candidates.add(current.resolve("public"));
        candidates.add(current.resolve("..").resolve("..").resolve("bekri-wellbeing-platform").resolve("public").normalize());
        candidates.add(current.resolve("..").resolve("bekri-wellbeing-platform").resolve("public").normalize());

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return ".png";
        }
        return fileName.substring(dotIndex);
    }
}

