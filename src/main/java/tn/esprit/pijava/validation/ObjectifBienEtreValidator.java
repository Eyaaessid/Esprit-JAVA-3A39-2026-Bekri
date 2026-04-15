package tn.esprit.pijava.validation;

import tn.esprit.pijava.entity.ObjectifBienEtre;

public class ObjectifBienEtreValidator {
    public void validate(ObjectifBienEtre o) {
        if (o.getTitre() == null || o.getTitre().isBlank()) {
            throw new ValidationException("Le titre est obligatoire.");
        }
        int titreLength = o.getTitre().trim().length();
        if (titreLength < 3 || titreLength > 255) {
            throw new ValidationException("Le titre doit contenir entre 3 et 255 caractères.");
        }
        if (o.getType() == null || o.getType().isBlank()) {
            throw new ValidationException("Le type est obligatoire.");
        }
        if (o.getValeurCible() == null) {
            throw new ValidationException("La valeur cible est obligatoire.");
        }
        if (o.getValeurCible() <= 0) {
            throw new ValidationException("La valeur cible doit être un nombre positif.");
        }
        if (o.getDateDebut() == null) {
            throw new ValidationException("La date de début est obligatoire.");
        }
        if (o.getDateFin() == null) {
            throw new ValidationException("La date de fin est obligatoire.");
        }
        if (o.getStatut() == null || o.getStatut().isBlank()) {
            throw new ValidationException("Le statut est obligatoire.");
        }
    }
}
