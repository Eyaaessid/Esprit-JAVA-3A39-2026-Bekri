package tn.esprit.shared;

import java.io.File;

public final class AvatarUrlHelper {
    private AvatarUrlHelper() {}

    /** Returns a string suitable for {@link javafx.scene.image.Image} constructor. */
    public static String toImageUrl(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        if (pathOrUrl.startsWith("file:")) {
            return pathOrUrl;
        }
        return new File(pathOrUrl).toURI().toString();
    }
}
