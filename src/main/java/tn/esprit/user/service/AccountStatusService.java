package tn.esprit.user.service;

import tn.esprit.user.dao.ReactivationRequestDao;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.ReactivationRequest;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.exception.ReactivationException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

public class AccountStatusService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UtilisateurDao utilisateurDao;
    private final ReactivationRequestDao reactivationRequestDao;
    private final EmailService emailService;

    public AccountStatusService(UtilisateurDao utilisateurDao,
                                ReactivationRequestDao reactivationRequestDao,
                                EmailService emailService) {
        this.utilisateurDao = utilisateurDao;
        this.reactivationRequestDao = reactivationRequestDao;
        this.emailService = emailService;
    }

    public void deactivateAccount(Utilisateur target, String deactivatedBy) {
        validateNotAdmin(target, "desactiver");
        utilisateurDao.updateStatut(target.getId(), "INACTIF", deactivatedBy);
        target.setStatut(UtilisateurStatut.INACTIF);
        target.setDeactivatedBy(deactivatedBy);
        if ("user".equalsIgnoreCase(deactivatedBy)) {
            sendSelfReactivationCode(target);
        }
        emailService.sendAccountDeactivated(target, deactivatedBy);
    }

    public void blockAccount(Utilisateur target) {
        validateNotAdmin(target, "bloquer");
        if (target.getStatut() == UtilisateurStatut.BLOQUE) {
            throw new IllegalArgumentException("Ce compte est deja bloque.");
        }
        utilisateurDao.updateStatut(target.getId(), "BLOQUE", "admin");
        target.setStatut(UtilisateurStatut.BLOQUE);
        target.setDeactivatedBy("admin");
        emailService.sendAccountBlocked(target);
    }

    public void reactivateAccount(Utilisateur target) {
        utilisateurDao.reactivate(target.getId());
        utilisateurDao.clearReactivationToken(target.getId());
        target.setStatut(UtilisateurStatut.ACTIF);
        target.setDeactivatedBy(null);
        target.setDeactivatedAt(null);
        emailService.sendReactivationApproved(target);
    }

    public void submitReactivationRequest(String email, String reason) throws ReactivationException {
        Utilisateur user = utilisateurDao.findByEmail(email.trim())
                .orElseThrow(() -> new ReactivationException("Aucun compte ne correspond a cette adresse email."));

        if (user.getStatut() != UtilisateurStatut.INACTIF) {
            throw new ReactivationException("Seuls les comptes inactifs peuvent demander une reactivation.");
        }
        if (!"admin".equalsIgnoreCase(user.getDeactivatedBy())) {
            throw new ReactivationException("Ce compte ne peut pas etre reactive depuis l'application. Utilisez votre code de reactivation recu par email.");
        }
        if (reactivationRequestDao.findPendingByUserId(user.getId()).isPresent()) {
            throw new ReactivationException("Une demande de reactivation est deja en attente pour ce compte.");
        }

        ReactivationRequest request = new ReactivationRequest();
        request.setUtilisateurId(user.getId());
        request.setReason(reason);
        reactivationRequestDao.save(request);
        emailService.sendReactivationRequestReceived(user);
    }

    public void approveRequest(int requestId) {
        ReactivationRequest request = getPendingRequest(requestId);
        Utilisateur user = loadRequestUser(request);
        reactivationRequestDao.approve(requestId);
        utilisateurDao.reactivate(user.getId());
        utilisateurDao.clearReactivationToken(user.getId());
        user.setStatut(UtilisateurStatut.ACTIF);
        user.setDeactivatedBy(null);
        emailService.sendReactivationApproved(user);
    }

    public void denyRequest(int requestId, String adminNote) {
        ReactivationRequest request = getPendingRequest(requestId);
        Utilisateur user = loadRequestUser(request);
        reactivationRequestDao.deny(requestId, adminNote);
        emailService.sendReactivationDenied(user, adminNote);
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

    private ReactivationRequest getPendingRequest(int requestId) {
        ReactivationRequest request = reactivationRequestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Demande de reactivation introuvable."));
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalArgumentException("Cette demande a deja ete traitee.");
        }
        return request;
    }

    private Utilisateur loadRequestUser(ReactivationRequest request) {
        if (request.getUtilisateur() != null) {
            Optional<Utilisateur> refreshed = utilisateurDao.findById(request.getUtilisateurId());
            return refreshed.orElse(request.getUtilisateur());
        }
        return utilisateurDao.findById(request.getUtilisateurId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable pour cette demande."));
    }

    private void validateNotAdmin(Utilisateur target, String action) {
        if (target == null || target.getId() == null) {
            throw new IllegalArgumentException("Utilisateur cible introuvable.");
        }
        if (target.getRole() == UtilisateurRole.ADMIN) {
            throw new IllegalArgumentException("Impossible de " + action + " un administrateur.");
        }
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
