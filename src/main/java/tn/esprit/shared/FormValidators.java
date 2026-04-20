package tn.esprit.shared;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Règles alignées sur les formulaires Symfony du projet Bekri.
 */
public final class FormValidators {

    public static final String ERR_NOM_REQ = "Le nom est obligatoire.";
    public static final String ERR_NOM_MIN = "Le nom doit contenir au moins 2 caractères.";
    public static final String ERR_NOM_MAX = "Le nom ne peut pas dépasser 100 caractères.";

    public static final String ERR_PRENOM_REQ = "Le prénom est obligatoire.";
    public static final String ERR_PRENOM_MIN = "Le prénom doit contenir au moins 2 caractères.";
    public static final String ERR_PRENOM_MAX = "Le prénom ne peut pas dépasser 100 caractères.";

    public static final String ERR_EMAIL_REQ = "L'email est obligatoire.";
    public static final String ERR_EMAIL_FMT = "L'adresse email n'est pas valide.";

    public static final String ERR_PHONE_FMT = "Le numéro de téléphone n'est pas valide.";

    public static final String ERR_DOB_REQ = "La date de naissance est obligatoire.";
    public static final String ERR_DOB_AGE = "Vous devez avoir au moins 13 ans.";

    public static final String ERR_PASSWORD_POLICY =
            "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial (@$!%*?&-_).";
    public static final String ERR_PASSWORD_MIN_ONLY =
            "Le mot de passe doit contenir au moins 8 caractères.";
    public static final String ERR_PASSWORD_MATCH = "Les mots de passe doivent correspondre.";

    public static final String ERR_PASSWORD_LOGIN_REQ = "Le mot de passe est obligatoire.";

    public static final String ERR_ROLE = "Veuillez sélectionner un rôle.";
    public static final String ERR_STATUT = "Veuillez sélectionner un statut.";

    public static final String ERR_TERMS = "Vous devez accepter les conditions d'utilisation.";

    public static final String GENERAL_CORRECT =
            "Veuillez corriger les erreurs avant de continuer.";

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9\\s\\-()]+$");
    private static final Pattern PASSWORD = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&\\-_])[A-Za-z\\d@$!%*?&\\-_]+$");

    private FormValidators() {}

    public static String validateNom(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERR_NOM_REQ;
        }
        String t = raw.trim();
        if (t.length() < 2) {
            return ERR_NOM_MIN;
        }
        if (t.length() > 100) {
            return ERR_NOM_MAX;
        }
        return null;
    }

    public static String validatePrenom(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERR_PRENOM_REQ;
        }
        String t = raw.trim();
        if (t.length() < 2) {
            return ERR_PRENOM_MIN;
        }
        if (t.length() > 100) {
            return ERR_PRENOM_MAX;
        }
        return null;
    }

    public static String validateEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERR_EMAIL_REQ;
        }
        String t = raw.trim();
        if (t.contains(" ") || !EMAIL.matcher(t).matches()) {
            return ERR_EMAIL_FMT;
        }
        return null;
    }

    /** Téléphone vide = valide (optionnel). */
    public static String validateTelephoneOptional(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.length() < 8 || t.length() > 20) {
            return ERR_PHONE_FMT;
        }
        if (!PHONE.matcher(t).matches()) {
            return ERR_PHONE_FMT;
        }
        return null;
    }

    /** Date obligatoire + âge minimum 13 ans. */
    public static String validateDateNaissanceRequired(LocalDate date) {
        if (date == null) {
            return ERR_DOB_REQ;
        }
        return validateDateNaissanceMinAge(date);
    }

    /** Date optionnelle : null = ok ; sinon même règle d'âge si renseignée. */
    public static String validateDateNaissanceOptional(LocalDate date) {
        if (date == null) {
            return null;
        }
        return validateDateNaissanceMinAge(date);
    }

    private static String validateDateNaissanceMinAge(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate oldestAllowedBirth = today.minusYears(13);
        if (date.isAfter(oldestAllowedBirth)) {
            return ERR_DOB_AGE;
        }
        return null;
    }

    public static String validatePasswordRequired(String password) {
        if (password == null || password.isEmpty()) {
            return ERR_PASSWORD_POLICY;
        }
        if (password.length() < 8 || !PASSWORD.matcher(password).matches()) {
            return ERR_PASSWORD_POLICY;
        }
        return null;
    }

    /** Mot de passe optionnel : vide = ok ; sinon politique complète. */
    public static String validatePasswordOptional(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }
        if (password.length() < 8) {
            return ERR_PASSWORD_MIN_ONLY;
        }
        if (!PASSWORD.matcher(password).matches()) {
            return ERR_PASSWORD_POLICY;
        }
        return null;
    }

    public static String validatePasswordConfirm(String password, String confirm) {
        if (password == null) {
            password = "";
        }
        if (confirm == null) {
            confirm = "";
        }
        if (!password.equals(confirm)) {
            return ERR_PASSWORD_MATCH;
        }
        return null;
    }

    public static boolean isNomValid(String raw) {
        return validateNom(raw) == null;
    }

    public static boolean isPrenomValid(String raw) {
        return validatePrenom(raw) == null;
    }

    public static boolean isEmailValid(String raw) {
        return validateEmail(raw) == null;
    }

    public static boolean isTelephoneOptionalValid(String raw) {
        return validateTelephoneOptional(raw) == null;
    }

    public static boolean isDateNaissanceRequiredValid(LocalDate d) {
        return validateDateNaissanceRequired(d) == null;
    }

    public static boolean isDateNaissanceOptionalValid(LocalDate d) {
        return validateDateNaissanceOptional(d) == null;
    }

    public static boolean isPasswordPolicyValid(String p) {
        return validatePasswordRequired(p) == null;
    }

    public static boolean isPasswordOptionalPolicyValid(String p) {
        return validatePasswordOptional(p) == null;
    }

    /** Rôle API : user | coach | admin (insensible à la casse). */
    public static String validateRoleRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERR_ROLE;
        }
        String r = raw.trim().toLowerCase(Locale.ROOT);
        if (!"user".equals(r) && !"coach".equals(r) && !"admin".equals(r)) {
            return ERR_ROLE;
        }
        return null;
    }

    /** Statut : actif | bloque | inactif. */
    public static String validateStatutRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            return ERR_STATUT;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!"actif".equals(s) && !"bloque".equals(s) && !"inactif".equals(s)) {
            return ERR_STATUT;
        }
        return null;
    }
}
