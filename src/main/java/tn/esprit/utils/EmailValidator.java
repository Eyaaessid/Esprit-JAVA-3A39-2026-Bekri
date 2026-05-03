package tn.esprit.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class EmailValidator {

    private static final Pattern BASIC_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public record ValidationResult(boolean valid, String errorMessage, String suggestion) {}

    private static final Map<String, String> TYPO_MAP = Map.ofEntries(
            Map.entry("gmial.com", "gmail.com"),
            Map.entry("gmai.com", "gmail.com"),
            Map.entry("gmal.com", "gmail.com"),
            Map.entry("gmail.co", "gmail.com"),
            Map.entry("gmail.cm", "gmail.com"),

            Map.entry("yaho.com", "yahoo.com"),
            Map.entry("yhoo.com", "yahoo.com"),
            Map.entry("yahoo.co", "yahoo.com"),
            Map.entry("yahoo.cm", "yahoo.com"),

            Map.entry("outlok.com", "outlook.com"),
            Map.entry("outlook.co", "outlook.com"),

            Map.entry("hotmai.com", "hotmail.com"),
            Map.entry("hotmial.com", "hotmail.com"),
            Map.entry("hotmail.co", "hotmail.com")
    );

    private static final Set<String> DISPOSABLE = Set.of(
            "tempmail.com", "guerrillamail.com", "mailinator.com", "10minutemail.com",
            "throwaway.email", "temp-mail.org", "temp-mail.io", "getnada.com",
            "mohmal.com", "yopmail.com", "sharklasers.com", "grr.la", "pokemail.net",
            "spam4.me", "trashmail.com", "maildrop.cc", "fakeinbox.com", "mailsac.com",
            "nada.email", "tmpmail.org", "emailondeck.com", "fakemailgenerator.com"
    );

    private EmailValidator() {}

    public static ValidationResult validate(String email) {
        // a) Null/blank
        if (email == null || email.isBlank()) {
            return new ValidationResult(false, "Veuillez entrer une adresse email.", null);
        }
        String e = email.trim();

        // b) Regex format
        if (!BASIC_EMAIL.matcher(e).matches()) {
            return new ValidationResult(false, "L'adresse email n'est pas valide.", null);
        }

        String domain = extractDomain(e);
        if (domain == null || domain.isBlank()) {
            return new ValidationResult(false, "L'adresse email n'est pas valide.", null);
        }
        String d = domain.toLowerCase(Locale.ROOT);

        // c) Typo map
        if (TYPO_MAP.containsKey(d)) {
            String corrected = TYPO_MAP.get(d);
            String suggestion = buildSuggestion(e, corrected);
            return new ValidationResult(false, "Vouliez-vous dire " + suggestion + " ?", suggestion);
        }

        // d) Disposable blocklist (incl. subdomains)
        for (String blocked : DISPOSABLE) {
            if (d.equals(blocked) || d.endsWith("." + blocked)) {
                return new ValidationResult(false, "Les adresses email jetables ne sont pas autorisées.", null);
            }
        }

        // e) Domain reachability (best-effort)
        try {
            InetAddress.getAllByName(d);
        } catch (UnknownHostException ex) {
            return new ValidationResult(false, "Ce domaine email ne peut pas recevoir d'emails.", null);
        } catch (Exception ignored) {
            // skip check (do not fail closed)
        }

        return new ValidationResult(true, null, null);
    }

    private static String extractDomain(String email) {
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return null;
        }
        return email.substring(at + 1);
    }

    private static String buildSuggestion(String email, String correctedDomain) {
        int at = email.lastIndexOf('@');
        String local = at > 0 ? email.substring(0, at) : email;
        return local + "@" + correctedDomain;
    }
}

