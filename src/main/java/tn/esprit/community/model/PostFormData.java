package tn.esprit.community.model;

import java.nio.file.Path;

public record PostFormData(
        String titre,
        String categorie,
        String contenu,
        Path selectedImagePath,
        boolean removeExistingImage
) {
}

