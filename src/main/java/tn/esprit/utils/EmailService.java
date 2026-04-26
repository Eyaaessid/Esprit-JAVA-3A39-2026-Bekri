package tn.esprit.utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import tn.esprit.user.entity.Utilisateur;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class EmailService {

    private static EmailService instance;
    private final Session session;
    private final String fromAddress;
    private final String fromName;

    private EmailService() {
        Properties config = loadConfig();

        this.fromAddress = config.getProperty("mail.from.address", "noreply@bekri.tn");
        this.fromName    = config.getProperty("mail.from.name",    "Bekri Wellbeing");

        String host     = config.getProperty("mail.smtp.host",     "sandbox.smtp.mailtrap.io");
        String port     = config.getProperty("mail.smtp.port",     "587");
        String username = config.getProperty("mail.smtp.username", "");
        String password = config.getProperty("mail.smtp.password", "");
        String auth     = config.getProperty("mail.smtp.auth",     "true");
        String starttls = config.getProperty("mail.smtp.starttls", "true");

        System.out.println("[EmailService] mail.smtp.host="     + host);
        System.out.println("[EmailService] mail.smtp.port="     + port);
        System.out.println("[EmailService] mail.smtp.username=" + username);
        System.out.println("[EmailService] mail.from.address="  + fromAddress);

        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host",            host);
        mailProps.put("mail.smtp.port",            port);
        mailProps.put("mail.smtp.auth",            auth);
        mailProps.put("mail.smtp.starttls.enable", starttls);

        this.session = Session.getInstance(mailProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    // ------------------------------------------------------------------ //
    //  Password reset & verification (used by UtilisateurService)         //
    // ------------------------------------------------------------------ //

    public void sendPasswordResetEmail(String toEmail, String toName, String token) {
        String html = "<p>Bonjour " + escape(toName) + ",</p>"
                + "<p>Vous avez demandé la réinitialisation de votre mot de passe.</p>"
                + "<p>Votre code de réinitialisation :</p>"
                + "<h1 style=\"letter-spacing:8px;font-size:36px;font-family:monospace;\">" + escape(token) + "</h1>"
                + "<p>Ce code expire dans <strong>1 heure</strong>.</p>"
                + "<p>Besoin d'aide ? Contactez <a href=\"mailto:support@bekri.tn\">support@bekri.tn</a></p>";
        sendSync(toEmail, toName, "Réinitialisation de votre mot de passe", html);
    }

    public void sendVerificationEmail(String toEmail, String toName, String token) {
        String html = "<p>Bonjour " + escape(toName) + ",</p>"
                + "<p>Entrez ce code dans l'application pour vérifier votre email.</p>"
                + "<h1 style=\"letter-spacing:8px;font-size:36px;font-family:monospace;\">" + escape(token) + "</h1>"
                + "<p>Ce code expire dans <strong>24 heures</strong>.</p>"
                + "<p>Besoin d'aide ? Contactez <a href=\"mailto:support@bekri.tn\">support@bekri.tn</a></p>";
        sendSync(toEmail, toName, "Vérifiez votre adresse email", html);
    }

    // ------------------------------------------------------------------ //
    //  Account status emails (used by AccountStatusService)               //
    // ------------------------------------------------------------------ //

    public void sendAccountDeactivated(Utilisateur user, String deactivatedBy) {
        String actor = "admin".equalsIgnoreCase(deactivatedBy)
                ? "un administrateur"
                : "vous";
        String nextStep = "admin".equalsIgnoreCase(deactivatedBy)
                ? "Pour reactiver votre compte, veuillez contacter support@bekri.tn."
                : "Un code de réactivation à 6 chiffres vous a été envoyé. Saisissez-le dans l'application pour réactiver votre compte.";
        String html = "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                + "<p>Votre compte Bekri a été désactivé par " + actor + ".</p>"
                + "<p>" + nextStep + "</p>"
                + "<p>Besoin d'aide ? Contactez <a href=\"mailto:support@bekri.tn\">support@bekri.tn</a></p>";
        // ✅ FIX: send synchronously so the email is not killed by scene switch
        sendSync(user.getEmail(), fullName(user), "Votre compte Bekri est inactif", html);
    }

    public void sendSelfReactivationCode(Utilisateur user, String code) {
        String html = "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                + "<p>Voici votre code de réactivation à 6 chiffres :</p>"
                + "<div style=\"font-size:34px;font-weight:bold;letter-spacing:8px;color:#0f4674;margin:16px 0;\">"
                + escape(code) + "</div>"
                + "<p>Entrez ce code dans l'application pour réactiver votre compte.</p>"
                + "<p>Ce code expire dans <strong>24 heures</strong>.</p>";
        // ✅ FIX: send synchronously — this code is critical, must not be lost
        sendSync(user.getEmail(), fullName(user), "Votre code de réactivation Bekri", html);
    }

    public void sendAccountBlocked(Utilisateur user) {
        String html = "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                + "<p>Votre compte Bekri a été suspendu de manière permanente.</p>"
                + "<p>Pour toute question, contactez <a href=\"mailto:support@bekri.tn\">support@bekri.tn</a></p>";
        sendAsync(user, "Suspension permanente de votre compte Bekri", html);
    }

    public void sendReactivationApproved(Utilisateur user) {
        String html = "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                + "<p>Bonne nouvelle : votre compte Bekri est de nouveau actif.</p>"
                + "<p>Vous pouvez maintenant vous connecter à l'application.</p>";
        sendAsync(user, "Votre compte Bekri a été réactivé", html);
    }

    // ------------------------------------------------------------------ //
    //  Internal send helpers                                               //
    // ------------------------------------------------------------------ //

    /** Synchronous send — used for critical emails (reset, verification, reactivation code). */
    private void sendSync(String toEmail, String toName, String subject, String htmlBody) {
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            msg.setSubject(subject, StandardCharsets.UTF_8.name());
            msg.setContent(wrapHtml(htmlBody), "text/html; charset=UTF-8");
            Transport.send(msg);
            System.out.println("[EmailService] Sent '" + subject + "' to " + toEmail);
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send '" + subject + "' to " + toEmail + ": " + e.getMessage());
            e.printStackTrace(System.err);
            throw new RuntimeException("Erreur envoi email: " + e.getMessage(), e);
        }
    }

    /** Asynchronous send — used for non-critical notifications (fire-and-forget). */
    private void sendAsync(Utilisateur user, String subject, String htmlBody) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            System.err.println("[EmailService] sendAsync: utilisateur ou email manquant.");
            return;
        }
        String toEmail = user.getEmail();
        String toName  = fullName(user);
        Thread thread = new Thread(() -> {
            try {
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
                msg.setSubject(subject, StandardCharsets.UTF_8.name());
                msg.setContent(wrapHtml(htmlBody), "text/html; charset=UTF-8");
                Transport.send(msg);
                System.out.println("[EmailService] Sent '" + subject + "' to " + toEmail);
            } catch (Exception e) {
                System.err.println("[EmailService] Failed to send '" + subject + "' to " + toEmail + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }, "email-" + subject.substring(0, Math.min(subject.length(), 20)));
        thread.setDaemon(false); // ✅ FIX: non-daemon so JVM waits for email to finish
        thread.start();
    }

    private String wrapHtml(String body) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:620px;margin:0 auto;line-height:1.6;color:#1f2937;\">"
                + "<h2 style=\"color:#0f4674;\">Bekri Wellbeing</h2>"
                + body
                + "<hr style=\"border:none;border-top:1px solid #d9e1f1;margin:24px 0;\"/>"
                + "<p style=\"font-size:12px;color:#64748b;\">Cet email a été envoyé automatiquement par Bekri.</p>"
                + "</div>";
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = EmailService.class.getResourceAsStream("/config.properties")) {
            if (is != null) {
                props.load(is);
                System.out.println("[EmailService] config.properties loaded successfully.");
            } else {
                System.err.println("[EmailService] ERROR: config.properties NOT FOUND in classpath!");
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Failed to load config.properties: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return props;
    }

    private static String fullName(Utilisateur user) {
        if (user == null) return "";
        String full = user.getFullName();
        return (full == null || full.isBlank()) ? user.getEmail() : full.trim();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
