package tn.esprit.user.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class UtilisateurService {
    private final UtilisateurDao dao = new UtilisateurDao();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public Utilisateur register(String nom, String prenom, String email, String password) {
        return register(nom, prenom, email, password, null, null);
    }

    public Utilisateur register(String nom, String prenom, String email, String password,
                                String telephone, LocalDate dateNaissance) {
        if (dao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        String hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setMotDePasse(hashed);
        u.setRole(UtilisateurRole.USER);
        u.setStatut(UtilisateurStatut.ACTIF);
        u.setTelephone(telephone);
        u.setDateNaissance(dateNaissance);
        return dao.save(u);
    }

    public Utilisateur login(String email, String password) {
        Utilisateur u = dao.findByEmailWithSubtype(email);
        if (u == null) {
            throw new IllegalArgumentException("Email introuvable");
        }
        var result = BCrypt.verifyer().verify(password.toCharArray(), u.getMotDePasse());
        if (!result.verified) {
            throw new IllegalArgumentException("Mot de passe incorrect");
        }
        return u;
    }

    public void updateLastLogin(int userId) {
        dao.updateLastLogin(userId);
    }

    public void requestPasswordReset(String email) {
        Utilisateur user = dao.findByEmail(email).orElse(null);
        if (user == null) {
            return; // no info leak
        }
        if (!user.isVerified()) {
            return; // prevent overriding verification token (shared reset_token column)
        }

        String token = generateToken6Digits();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        dao.saveResetToken(user.getId(), token, expiresAt);

        try {
            EmailService.getInstance().sendPasswordResetEmail(
                    user.getEmail(),
                    user.getPrenom() + " " + user.getNom(),
                    token
            );
        } catch (Exception e) {
            System.out.println("[UtilisateurService] Failed to send reset email: " + e.getMessage());
        }
    }

    public enum ResetResult { SUCCESS, INVALID_TOKEN, EXPIRED_TOKEN, WEAK_PASSWORD }

    public ResetResult resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return ResetResult.WEAK_PASSWORD;
        }
        if (token == null || token.isBlank()) {
            return ResetResult.INVALID_TOKEN;
        }

        Utilisateur user = dao.findByResetToken(token.trim());
        if (user == null) {
            return ResetResult.INVALID_TOKEN;
        }

        LocalDateTime expiresAt = user.getResetTokenExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            dao.clearResetToken(user.getId());
            return ResetResult.EXPIRED_TOKEN;
        }

        String hashed = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        dao.updatePassword(user.getId(), hashed);
        dao.clearResetToken(user.getId());
        return ResetResult.SUCCESS;
    }

    public void sendVerificationEmail(Utilisateur user) {
        if (user == null || user.getId() == null) {
            return;
        }
        String token = generateToken6Digits();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        dao.saveVerificationToken(user.getId(), token, expiresAt);
        EmailService.getInstance().sendVerificationEmail(
                user.getEmail(),
                user.getPrenom() + " " + user.getNom(),
                token
        );
    }

    public enum VerifyResult { SUCCESS, INVALID_TOKEN, EXPIRED_TOKEN }

    public VerifyResult verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            return VerifyResult.INVALID_TOKEN;
        }

        Utilisateur user = dao.findByVerificationToken(token.trim());
        if (user == null) {
            return VerifyResult.INVALID_TOKEN;
        }

        LocalDateTime expiresAt = user.getResetTokenExpiresAt();
        if (expiresAt == null || !expiresAt.isAfter(LocalDateTime.now())) {
            dao.clearResetToken(user.getId());
            return VerifyResult.EXPIRED_TOKEN;
        }

        dao.setVerified(user.getId());
        return VerifyResult.SUCCESS;
    }

    private static String generateToken6Digits() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    public List<Utilisateur> getAllUsers() {
        return dao.findAll();
    }

    public void deleteById(Integer id) {
        dao.deleteById(id);
    }

    public Utilisateur getUserById(Integer id) {
        return dao.findById(id).orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
    }

    public Utilisateur updateUser(Utilisateur u) {
        return dao.save(u);
    }

    public Utilisateur updateRole(Integer id, UtilisateurRole role) {
        Utilisateur u = getUserById(id);
        u.setRole(role);
        return dao.save(u);
    }

    public Utilisateur updateStatut(Integer id, UtilisateurStatut statut) {
        Utilisateur u = getUserById(id);
        u.setStatut(statut);
        return dao.save(u);
    }

    public void updateRoleAndStatus(Integer id, UtilisateurRole role, UtilisateurStatut statut) {
        dao.updateRoleAndStatus(id, role, statut);
    }

    public List<Utilisateur> findUtilisateursFiltered(String search, String roleFilter, String statutFilter) {
        String s = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String rf = roleFilter == null ? null : roleFilter.trim().toLowerCase(Locale.ROOT);
        String sf = statutFilter == null ? null : statutFilter.trim().toLowerCase(Locale.ROOT);

        return dao.findAllNonAdmin(s, sf == null || sf.isEmpty() ? null : sf.toUpperCase(Locale.ROOT)).stream()
                .filter(u -> rf == null || rf.isEmpty() || u.getRoleKey().equalsIgnoreCase(rf))
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getRoleStats() {
        return dao.fetchRoleStats();
    }

    private boolean matchesSearch(Utilisateur u, String q) {
        String nom = u.getNom() != null ? u.getNom().toLowerCase(Locale.ROOT) : "";
        String prenom = u.getPrenom() != null ? u.getPrenom().toLowerCase(Locale.ROOT) : "";
        String email = u.getEmail() != null ? u.getEmail().toLowerCase(Locale.ROOT) : "";
        return nom.contains(q) || prenom.contains(q) || email.contains(q);
    }

    public Utilisateur createUtilisateurAdmin(String nom, String prenom, String email, String password,
                                                String telephone, LocalDate dateNaissance, String apiRole) {
        if (dao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        UtilisateurRole role = parseRole(apiRole);
        String hashed = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setMotDePasse(hashed);
        u.setRole(role);
        u.setStatut(UtilisateurStatut.ACTIF);
        u.setTelephone(telephone);
        u.setDateNaissance(dateNaissance);
        return dao.save(u);
    }

    public Utilisateur updateUtilisateurAdmin(Integer id, String nom, String prenom, String email,
                                               String telephone, LocalDate dateNaissance,
                                               String newPasswordOrNull,
                                               String apiRole, String apiStatut) {
        Utilisateur u = getUserById(id);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone(telephone);
        u.setDateNaissance(dateNaissance);
        if (newPasswordOrNull != null && !newPasswordOrNull.isBlank()) {
            u.setMotDePasse(BCrypt.withDefaults().hashToString(12, newPasswordOrNull.toCharArray()));
        }
        if (apiRole != null) {
            u.setRole(parseRole(apiRole));
        }
        if (apiStatut != null) {
            u.setStatut(parseStatut(apiStatut));
        }
        return dao.save(u);
    }

    public Utilisateur updateProfile(Integer userId, String nom, String prenom, String email,
                                     String telephone, LocalDate dateNaissance,
                                     String photoProfil, String newPasswordOrNull) {
        Utilisateur u = getUserById(userId);
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setTelephone(telephone);
        u.setDateNaissance(dateNaissance);
        if (photoProfil != null) {
            u.setPhotoProfil(photoProfil);
        }
        if (newPasswordOrNull != null && !newPasswordOrNull.isBlank()) {
            u.setMotDePasse(BCrypt.withDefaults().hashToString(12, newPasswordOrNull.toCharArray()));
        }
        return dao.save(u);
    }

    public static UtilisateurRole parseRole(String api) {
        if (api == null || api.isBlank()) {
            throw new IllegalArgumentException("Rôle invalide");
        }
        return switch (api.trim().toLowerCase(Locale.ROOT)) {
            case "user" -> UtilisateurRole.USER;
            case "coach" -> UtilisateurRole.COACH;
            case "admin" -> UtilisateurRole.ADMIN;
            default -> throw new IllegalArgumentException("Rôle invalide");
        };
    }

    public static UtilisateurStatut parseStatut(String api) {
        if (api == null || api.isBlank()) {
            throw new IllegalArgumentException("Statut invalide");
        }
        return switch (api.trim().toLowerCase(Locale.ROOT)) {
            case "actif" -> UtilisateurStatut.ACTIF;
            case "inactif" -> UtilisateurStatut.INACTIF;
            case "bloque" -> UtilisateurStatut.BLOQUE;
            case "supprime" -> UtilisateurStatut.SUPPRIME;
            default -> throw new IllegalArgumentException("Statut invalide");
        };
    }

    public static LocalDate parseDateIso(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(iso.length() >= 10 ? iso.substring(0, 10) : iso, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
