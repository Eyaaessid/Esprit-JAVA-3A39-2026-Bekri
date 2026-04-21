package tn.esprit.faceauth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FaceAuthResourceExtractor {

    private static Path tempDir = null;

    /**
     * Extracts all faceauth resources to a temp directory.
     * Returns the Path to the temp faceauth folder.
     * Safe to call multiple times — reuses existing temp dir.
     */
    public static synchronized Path extractToTemp() throws IOException {
        if (tempDir != null && tempDir.toFile().exists()) {
            return tempDir;
        }

        tempDir = Files.createTempDirectory("bekri-faceauth-");
        tempDir.toFile().deleteOnExit();

        String[] files = {
                "face_capture.html",
                "face-api.min.js",
                "models/face_landmark_68_model-shard1",
                "models/face_landmark_68_model-weights_manifest.json",
                "models/face_recognition_model-shard1",
                "models/face_recognition_model-shard2",
                "models/face_recognition_model-weights_manifest.json",
                "models/ssd_mobilenetv1_model-shard1",
                "models/ssd_mobilenetv1_model-shard2",
                "models/ssd_mobilenetv1_model-weights_manifest.json",
                "models/tiny_face_detector_model-shard1",
                "models/tiny_face_detector_model-weights_manifest.json"
        };

        Files.createDirectories(tempDir.resolve("models"));

        for (String file : files) {
            String resourcePath = "/faceauth/" + file;
            try (InputStream in = FaceAuthResourceExtractor.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    throw new IOException("Missing classpath resource: " + resourcePath);
                }
                Path dest = tempDir.resolve(file);
                Files.createDirectories(dest.getParent());
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        System.out.println("[FaceAuth] Resources extracted to: " + tempDir);
        return tempDir;
    }

    /**
     * Returns the file:// URL string for face_capture.html in the temp dir.
     */
    public static String getHtmlUrl() throws IOException {
        Path dir = extractToTemp();
        return dir.resolve("face_capture.html").toUri().toString();
    }
}

