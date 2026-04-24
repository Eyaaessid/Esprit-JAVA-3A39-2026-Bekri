package tn.esprit.user.service;

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

public class EmailService {
    private final Properties config = loadConfig();

    public void sendAccountDeactivated(Utilisateur user, String deactivatedBy) {
        String actor = "administrateur".equalsIgnoreCase(deactivatedBy) || "admin".equalsIgnoreCase(deactivatedBy)
                ? "un administrateur"
                : "vous";
        String nextStep = "admin".equalsIgnoreCase(deactivatedBy)
                ? "Si vous souhaitez reactiver votre compte, vous pouvez envoyer une demande depuis l'application."
                : "Un code de reactivation a 6 chiffres vous a ete envoye par email. Vous pourrez le saisir dans l'application pour reactiver votre compte.";
        sendAsync(
                user,
                "Votre compte Bekri est inactif",
                "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                        + "<p>Votre compte Bekri a ete desactive par " + actor + ".</p>"
                        + "<p>" + nextStep + "</p>"
                        + "<p>Si vous avez besoin d'aide, contactez-nous a support@bekri.com.</p>"
        );
    }

    public void sendSelfReactivationCode(Utilisateur user, String code) {
        sendAsync(
                user,
                "Votre code de reactivation Bekri",
                "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                        + "<p>Voici votre code de reactivation a 6 chiffres :</p>"
                        + "<div style=\"font-size:34px;font-weight:bold;letter-spacing:8px;color:#0f4674;margin:16px 0;\">"
                        + escape(code)
                        + "</div>"
                        + "<p>Entrez ce code dans l'application pour reactiver votre compte.</p>"
                        + "<p>Ce code expire dans 24 heures.</p>"
        );
    }

    public void sendAccountBlocked(Utilisateur user) {
        sendAsync(
                user,
                "Suspension permanente de votre compte Bekri",
                "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                        + "<p>Votre compte Bekri a ete suspendu de maniere permanente.</p>"
                        + "<p>Pour toute question, merci de contacter le support a support@bekri.com.</p>"
        );
    }

    public void sendReactivationRequestReceived(Utilisateur user) {
        sendAsync(
                user,
                "Demande de reactivation recue",
                "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                        + "<p>Nous avons bien recu votre demande de reactivation.</p>"
                        + "<p>Un administrateur va l'examiner prochainement. Vous recevrez un nouvel email des qu'une decision sera prise.</p>"
        );
    }

    public void sendReactivationApproved(Utilisateur user) {
        sendAsync(
                user,
                "Votre compte Bekri a ete reactive",
                "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                        + "<p>Bonne nouvelle : votre compte Bekri est de nouveau actif.</p>"
                        + "<p>Vous pouvez maintenant vous connecter a l'application.</p>"
        );
    }

    public void sendReactivationDenied(Utilisateur user, String adminNote) {
        String noteBlock = (adminNote == null || adminNote.isBlank())
                ? ""
                : "<p><strong>Note de l'administrateur :</strong><br/>" + escape(adminNote).replace("\n", "<br/>") + "</p>";
        sendAsync(
                user,
                "Votre demande de reactivation a ete refusee",
                "<p>Bonjour " + escape(fullName(user)) + ",</p>"
                        + "<p>Votre demande de reactivation a ete refusee.</p>"
                        + noteBlock
                        + "<p>Pour toute precision, contactez le support a support@bekri.com.</p>"
        );
    }

    private void sendAsync(Utilisateur user, String subject, String htmlBody) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            System.err.println("[AccountEmailService.sendAsync] utilisateur ou email manquant.");
            return;
        }

        Thread thread = new Thread(() -> {
            try {
                Session session = buildSession();
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(
                        config.getProperty("mail.from.address", "noreply@bekri.com"),
                        config.getProperty("mail.from.name", "Bekri Wellbeing"),
                        StandardCharsets.UTF_8.name()
                ));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail(), false));
                msg.setSubject(subject, StandardCharsets.UTF_8.name());
                msg.setContent(wrapHtml(htmlBody), "text/html; charset=UTF-8");
                Transport.send(msg);
            } catch (Exception e) {
                System.err.println("[AccountEmailService.sendAsync] userId=" + user.getId() + " " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }, "account-email-" + (user.getId() != null ? user.getId() : "unknown"));
        thread.setDaemon(true);
        thread.start();
    }

    private Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getProperty("mail.smtp.host", "sandbox.smtp.mailtrap.io"));
        props.put("mail.smtp.port", config.getProperty("mail.smtp.port", "2525"));
        props.put("mail.smtp.auth", config.getProperty("mail.smtp.auth", "true"));
        props.put("mail.smtp.starttls.enable", config.getProperty("mail.smtp.starttls", "true"));

        String username = config.getProperty("mail.smtp.username", "");
        String password = config.getProperty("mail.smtp.password", "");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private Properties loadConfig() {
        Properties properties = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) {
                properties.load(is);
            }
        } catch (Exception e) {
            System.err.println("[AccountEmailService.loadConfig] " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return properties;
    }

    private String wrapHtml(String body) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:620px;margin:0 auto;line-height:1.6;color:#1f2937;\">"
                + "<h2 style=\"color:#0f4674;\">Bekri Wellbeing</h2>"
                + body
                + "<hr style=\"border:none;border-top:1px solid #d9e1f1;margin:24px 0;\"/>"
                + "<p style=\"font-size:12px;color:#64748b;\">Cet email a ete envoye automatiquement par Bekri.</p>"
                + "</div>";
    }

    private String fullName(Utilisateur user) {
        String fullName = user.getFullName();
        return fullName == null || fullName.isBlank() ? user.getEmail() : fullName.trim();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
