package tn.esprit.utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class EmailService {

    private static final String SMTP_HOST = "sandbox.smtp.mailtrap.io";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USERNAME = "1c467244828e21";
    private static final String SMTP_PASSWORD = "24ad72273ea86c";

    private static final String FROM_EMAIL = "noreply@bekri.com";
    private static final String FROM_NAME = "Bekri Wellbeing";

    private static EmailService instance;

    private final Session session;

    private EmailService() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(SMTP_PORT));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
    }

    public static synchronized EmailService getInstance() {
        if (instance == null) {
            instance = new EmailService();
        }
        return instance;
    }

    public void sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, StandardCharsets.UTF_8.name()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            msg.setSubject("Réinitialisation de votre mot de passe", StandardCharsets.UTF_8.name());

            String safeName = escapeHtml(toName);
            String safeToken = escapeHtml(resetLink);
            String html = ""
                    + "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;line-height:1.5;color:#0f172a;\">"
                    + "  <h2 style=\"margin:0 0 12px 0;\">Bonjour " + safeName + ",</h2>"
                    + "  <p style=\"margin:0 0 16px 0;\">Vous avez demandé la réinitialisation de votre mot de passe.</p>"
                    + "  <p style=\"margin:0 0 10px 0;\">Votre code de réinitialisation :</p>"
                    + "  <h1 style=\"margin:0 0 12px 0;letter-spacing:8px;font-size:36px;"
                    + "            font-family:ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;\">"
                    +        safeToken
                    + "  </h1>"
                    + "  <p style=\"margin:0;color:#475569;\">Ce code expire dans <strong>1 heure</strong>.</p>"
                    + "  <hr style=\"border:none;border-top:1px solid #e2e8f0;margin:20px 0;\"/>"
                    + "  <p style=\"margin:0;color:#64748b;font-size:12px;\">Besoin d'aide ? Contactez <a href=\"mailto:support@bekri.com\">support@bekri.com</a></p>"
                    + "</div>";

            msg.setContent(html, "text/html; charset=UTF-8");
            Transport.send(msg);
            System.out.println("[EmailService] Password reset email sent to " + toEmail);
        } catch (Exception e) {
            System.out.println("[EmailService] Failed to send password reset email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void sendVerificationEmail(String toEmail, String toName, String verificationToken) {
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME, StandardCharsets.UTF_8.name()));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            msg.setSubject("Vérifiez votre adresse email", StandardCharsets.UTF_8.name());

            String safeName = escapeHtml(toName);
            String safeToken = escapeHtml(verificationToken);
            String html = ""
                    + "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:0 auto;line-height:1.5;color:#0f172a;\">"
                    + "  <h2 style=\"margin:0 0 12px 0;\">Bonjour " + safeName + ",</h2>"
                    + "  <p style=\"margin:0 0 16px 0;\">Entrez ce code dans l'application pour vérifier votre email.</p>"
                    + "  <h1 style=\"margin:0 0 12px 0;letter-spacing:8px;font-size:36px;"
                    + "            font-family:ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;\">"
                    +        safeToken
                    + "  </h1>"
                    + "  <p style=\"margin:16px 0 0 0;color:#475569;\">Ce code expire dans 24 heures.</p>"
                    + "  <hr style=\"border:none;border-top:1px solid #e2e8f0;margin:20px 0;\"/>"
                    + "  <p style=\"margin:0;color:#64748b;font-size:12px;\">Besoin d'aide ? Contactez <a href=\"mailto:support@bekri.com\">support@bekri.com</a></p>"
                    + "</div>";

            msg.setContent(html, "text/html; charset=UTF-8");
            Transport.send(msg);
            System.out.println("[EmailService] Verification email sent to " + toEmail);
        } catch (Exception e) {
            System.out.println("[EmailService] Failed to send verification email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

