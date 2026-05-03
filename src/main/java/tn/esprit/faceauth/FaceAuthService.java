package tn.esprit.faceauth;

import com.google.gson.Gson;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.utils.MyDataBase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;

public class FaceAuthService {

    private static final double MATCH_THRESHOLD = 0.3; // must match Symfony
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final Gson gson = new Gson();
    private final UtilisateurDao utilisateurDao = new UtilisateurDao();

    private Utilisateur lastAuthenticatedUser;

    public enum FaceAuthResult {
        SUCCESS,
        USER_NOT_FOUND,
        FACE_NOT_ENABLED,
        ACCOUNT_NOT_ACTIVE,
        EMAIL_NOT_VERIFIED,
        LOCKED_OUT,
        FACE_MISMATCH
    }

    public Utilisateur getLastAuthenticatedUser() {
        return lastAuthenticatedUser;
    }

    public void storeFaceDescriptor(int userId, double[] descriptor) {
        String json = gson.toJson(descriptor);
        String sql = "UPDATE utilisateur SET "
                + "face_descriptor = ?, "
                + "face_auth_enabled = 1, "
                + "face_registered_at = NOW(), "
                + "face_auth_failed_attempts = 0, "
                + "updated_at = NOW() "
                + "WHERE id = ?";
        try (PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            ps.setString(1, json);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void disableFaceAuth(int userId) {
        String sql = "UPDATE utilisateur SET "
                + "face_descriptor = NULL, "
                + "face_auth_enabled = 0, "
                + "face_registered_at = NULL, "
                + "updated_at = NOW() "
                + "WHERE id = ?";
        try (PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FaceAuthResult authenticate(String email, double[] providedDescriptor) {
        lastAuthenticatedUser = null;

        Utilisateur user = utilisateurDao.findByEmail(email).orElse(null);
        if (user == null) {
            return FaceAuthResult.USER_NOT_FOUND;
        }

        if (!user.isFaceAuthEnabled() || user.getFaceDescriptor() == null || user.getFaceDescriptor().isBlank()) {
            return FaceAuthResult.FACE_NOT_ENABLED;
        }

        lastAuthenticatedUser = user;

        if (user.getStatut() != UtilisateurStatut.ACTIF) {
            return FaceAuthResult.ACCOUNT_NOT_ACTIVE;
        }

        if (!user.isVerified()) {
            return FaceAuthResult.EMAIL_NOT_VERIFIED;
        }

        if (isLockedOut(user)) {
            // If lockout window expired, reset attempts and continue
            if (lockoutExpired(user)) {
                resetFailedAttempts(user.getId());
                user.setFaceAuthFailedAttempts(0);
            } else {
                return FaceAuthResult.LOCKED_OUT;
            }
        }

        double[] stored = gson.fromJson(user.getFaceDescriptor(), double[].class);
        if (stored == null || stored.length != 128 || providedDescriptor == null || providedDescriptor.length != 128) {
            incrementFailedAttempt(user.getId());
            return FaceAuthResult.FACE_MISMATCH;
        }

        double distance = euclideanDistance128(stored, providedDescriptor);
        if (distance < MATCH_THRESHOLD) {
            markSuccess(user.getId());
            lastAuthenticatedUser = utilisateurDao.findById(user.getId()).orElse(user);
            return FaceAuthResult.SUCCESS;
        }

        incrementFailedAttempt(user.getId());
        return FaceAuthResult.FACE_MISMATCH;
    }

    public int getRemainingLockoutMinutes(int userId) {
        String sql = "SELECT face_auth_failed_attempts, last_face_auth_attempt_at FROM utilisateur WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                int attempts = rs.getInt("face_auth_failed_attempts");
                Timestamp ts = rs.getTimestamp("last_face_auth_attempt_at");
                LocalDateTime last = ts != null ? ts.toLocalDateTime() : null;
                if (attempts < MAX_FAILED_ATTEMPTS || last == null) {
                    return 0;
                }
                Duration since = Duration.between(last, LocalDateTime.now());
                if (since.compareTo(LOCKOUT_DURATION) >= 0) {
                    return 0;
                }
                long remainingSeconds = LOCKOUT_DURATION.minus(since).getSeconds();
                long remainingMinutes = (long) Math.ceil(remainingSeconds / 60.0);
                return (int) Math.max(1, remainingMinutes);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isLockedOut(Utilisateur u) {
        return u.getFaceAuthFailedAttempts() >= MAX_FAILED_ATTEMPTS && u.getLastFaceAuthAttemptAt() != null;
    }

    private boolean lockoutExpired(Utilisateur u) {
        if (u.getLastFaceAuthAttemptAt() == null) return true;
        Duration since = Duration.between(u.getLastFaceAuthAttemptAt(), LocalDateTime.now());
        return since.compareTo(LOCKOUT_DURATION) >= 0;
    }

    private void resetFailedAttempts(int userId) {
        String sql = "UPDATE utilisateur SET face_auth_failed_attempts = 0 WHERE id = ?";
        try (PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void markSuccess(int userId) {
        String sql = "UPDATE utilisateur SET "
                + "face_auth_failed_attempts = 0, "
                + "last_login_at = NOW(), "
                + "last_face_auth_attempt_at = NOW() "
                + "WHERE id = ?";
        try (PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void incrementFailedAttempt(int userId) {
        String sql = "UPDATE utilisateur SET "
                + "face_auth_failed_attempts = face_auth_failed_attempts + 1, "
                + "last_face_auth_attempt_at = NOW() "
                + "WHERE id = ?";
        try (PreparedStatement ps = MyDataBase.getInstance().getCnx().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private static double euclideanDistance128(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < 128; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
