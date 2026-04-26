package tn.esprit.user.service;

import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.exception.ReactivationException;
import tn.esprit.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;

public class AccountStatusService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UtilisateurDao utilisateurDao;
    private final EmailService emailService;

    public AccountStatusService(UtilisateurDao utilisateurDao, EmailService emailService) {
        this.utilisateurDao = utilisateurDao;
        this.emailService = emailService;
    }

    public void deactivateAccount(Utilisateur target, String deactivatedBy) {
        validateNotAdmin(target, "desactiver");
        String actor = normalizeDeactivatedBy(deactivatedBy);
        utilisateurDao.updateStatut(target.getId(), "INACTIF", actor);
        target.setStatut(UtilisateurStatut.INACTIF);
        target.setDeactivatedBy(actor);

        if ("user".equals(actor)) {
            sendSelfReactivationCode(target);
            // ✅ FIX: wait 1.5s between emails to avoid Mailtrap free-plan rate limit (1 email/sec)
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }

        // ✅ FIX: wrapped in try/catch so a failed notification email
        // does NOT roll back the deactivation that already happened in DB
        try {
            emailService.sendAccountDeactivated(target, actor);
        } catch (Exception e) {
            System.err.println("[AccountStatusService] Warning: deactivation email failed (account is still deactivated): " + e.getMessage());
        }
    }

    public void blockAccount(Utilisateur target) {
        validateNotAdmin(target, "bloquer");
        if (target.getStatut() == UtilisateurStatut.BLOQUE) {
            throw new IllegalArgumentException("Ce compte est deja bloque.");
        }
        utilisateurDao.updateStatut(target.getId(), "BLOQUE", "admin");
        target.setStatut(UtilisateurStatut.BLOQUE);
        target.setDeactivatedBy("admin");
        try {
            emailService.sendAccountBlocked(target);
        } catch (Exception e) {
            System.err.println("[AccountStatusService] Warning: block email failed: " + e.getMessage());
        }
    }

    public void reactivateAccount(Utilisateur target) {
        utilisateurDao.reactivate(target.getId());
        utilisateurDao.clearReactivationToken(target.getId());
        target.setStatut(UtilisateurStatut.ACTIF);
        target.setDeactivatedBy(null);
        target.setDeactivatedAt(null);
        try {
            emailService.sendReactivationApproved(target);
        } catch (Exception e) {
            System.err.println("[AccountStatusService] Warning: reactivation approved email failed: " + e.getMessage());
        }
    }

    public void sendSelfReactivationCode(String email) throws ReactivationException {
        Utilisateur user = utilisateurDao.findByEmail(email.trim())
                .orElseThrow(() -> new ReactivationException("Aucun compte ne correspond a cette adresse email."));
        sendSelfReactivationCode(user);
    }

    public void reactivateWithCode(String email, String code) throws ReactivationException {
        if (email == null || email.isBlank()) {
            throw new ReactivationException("Veuillez saisir votre adresse email.");
        }
        if (code == null || !code.matches("\\d{6}")) {
            throw new ReactivationException("Veuillez saisir un code a 6 chiffres.");
        }

        Utilisateur user = utilisateurDao.findByEmailAndReactivationToken(email.trim(), code.trim())
                .orElseThrow(() -> new ReactivationException("Code invalide ou deja utilise."));

        if (user.getStatut() != UtilisateurStatut.INACTIF || !"user".equalsIgnoreCase(user.getDeactivatedBy())) {
            throw new ReactivationException("Ce compte ne peut pas etre reactive avec un code.");
        }
        if (user.getReactivationTokenExpiresAt() == null || !user.getReactivationTokenExpiresAt().isAfter(LocalDateTime.now())) {
            utilisateurDao.clearReactivationToken(user.getId());
            throw new ReactivationException("Ce code a expire. Veuillez en demander un nouveau.");
        }

        utilisateurDao.reactivate(user.getId());
        utilisateurDao.clearReactivationToken(user.getId());
    }

    private void validateNotAdmin(Utilisateur target, String action) {
        if (target == null || target.getId() == null) {
            throw new IllegalArgumentException("Utilisateur cible introuvable.");
        }
        if (target.getRole() == UtilisateurRole.ADMIN) {
            throw new IllegalArgumentException("Impossible de " + action + " un administrateur.");
        }
    }

    private String normalizeDeactivatedBy(String deactivatedBy) {
        if (deactivatedBy == null || deactivatedBy.isBlank()) {
            throw new IllegalArgumentException("Le type de desactivation est requis.");
        }

        String actor = deactivatedBy.trim().toLowerCase();
        if (!"admin".equals(actor) && !"user".equals(actor)) {
            throw new IllegalArgumentException("Le type de desactivation doit etre 'admin' ou 'user'.");
        }
        return actor;
    }

    private void sendSelfReactivationCode(Utilisateur user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        utilisateurDao.saveReactivationToken(user.getId(), code, LocalDateTime.now().plusHours(24));
        emailService.sendSelfReactivationCode(user, code);
    }
}
