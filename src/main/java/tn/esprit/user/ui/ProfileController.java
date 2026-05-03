package tn.esprit.user.ui;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tn.esprit.faceauth.FaceAuthService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.shell.UserShellNavigator;
import tn.esprit.user.shell.UserShellRoute;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.utils.EmailService;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private Label fullNameLabel;
    @FXML private Label roleLabel;
    @FXML private Label prenomLabel;
    @FXML private Label nomLabel;
    @FXML private Label emailLabel;
    @FXML private Label telLabel;
    @FXML private Label dateNaissanceLabel;
    @FXML private Label statutLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label twoFactorStatusLabel;
    @FXML private Label faceAuthStatusLabel;
    @FXML private Button enableTwoFactorBtn;
    @FXML private Button disableTwoFactorBtn;
    @FXML private Button regenerateBackupCodesBtn;
    @FXML private Button enableFaceAuthBtn;
    @FXML private Button disableFaceAuthBtn;
    @FXML private Button deactivateAccountBtn;

    private final FaceAuthService faceAuthService = new FaceAuthService();
    private final AccountStatusService accountStatusService =
            new AccountStatusService(new UtilisateurDao(), EmailService.getInstance());

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        avatarLabel.setText(user.getInitials());
        fullNameLabel.setText(user.getFullName());
        roleLabel.setText(capitalize(user.getRoleKey()));
        roleLabel.getStyleClass().add("badge-" + user.getRoleKey());

        prenomLabel.setText(nvl(user.getPrenom()));
        nomLabel.setText(nvl(user.getNom()));
        emailLabel.setText(nvl(user.getEmail()));
        telLabel.setText(nvl(user.getTelephone()));
        dateNaissanceLabel.setText(user.getDateNaissance() != null
                ? user.getDateNaissance().toString() : "-");
        statutLabel.setText(user.getStatut() != null ? capitalize(user.getStatutKey()) : "-");
        createdAtLabel.setText(user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDate().toString() : "-");
        boolean canDeactivate = user.getRole() != UtilisateurRole.ADMIN;
        deactivateAccountBtn.setVisible(canDeactivate);
        deactivateAccountBtn.setManaged(canDeactivate);
        refreshSecuritySection(user);
    }

    @FXML
    private void goToEditProfile() {
        try {
            SceneManager.switchTo("edit-profile");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goBack() {
        try {
            if (SessionManager.getInstance().isAdmin()) {
                SceneManager.switchTo("admin-dashboard");
            } else {
                UserShellNavigator.navigate(UserShellRoute.DASHBOARD);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().clear();
        try {
            SceneManager.switchTo("login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeactivateAccount() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            return;
        }
        if (!DialogHelper.showConfirm(
                "Desactiver mon compte",
                "Votre compte sera desactive et vous serez deconnecte immediatement. Un code de reactivation a 6 chiffres vous sera envoye par email.",
                "Desactiver",
                "Annuler")) {
            return;
        }

        new Thread(() -> {
            try {
                accountStatusService.deactivateAccount(user, "user");
                SessionManager.getInstance().logout();
                javafx.application.Platform.runLater(() -> {
                    DialogHelper.showInfo("Compte desactive",
                            "Votre compte a ete desactive. Verifiez votre email pour recuperer votre code de reactivation.");
                    try {
                        SceneManager.switchTo("login");
                    } catch (Exception e) {
                        DialogHelper.showError("Navigation", "Impossible de revenir a la connexion.");
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        DialogHelper.showError("Desactivation", "Impossible de desactiver votre compte."));
            }
        }).start();
    }

    @FXML
    private void goToFaceRegister() {
        try {
            Utilisateur user = SessionManager.getInstance().getCurrentUser();
            if (user == null || !SessionManager.getInstance().canUseSecurityFeatures()) {
                return;
            }
            var controller = SceneManager.switchToAndGetController("face-register");
            ((tn.esprit.faceauth.ui.FaceRegisterController) controller).setUser(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDisableFaceAuth() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null || !SessionManager.getInstance().canUseSecurityFeatures()) {
            return;
        }
        try {
            faceAuthService.disableFaceAuth(user.getId());
            user.setFaceAuthEnabled(false);
            user.setFaceDescriptor(null);
            user.setFaceRegisteredAt(null);
            SessionManager.getInstance().setCurrentUser(user);
            refreshSecuritySection(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTwoFactorSetup() {
        try {
            if (!SessionManager.getInstance().canUseSecurityFeatures()) {
                return;
            }
            SceneManager.switchTo("two-factor-setup");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTwoFactorDisable() {
        try {
            if (!SessionManager.getInstance().canUseSecurityFeatures()) {
                return;
            }
            SceneManager.switchTo("two-factor-disable");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegenerateBackupCodes() {
        try {
            if (!SessionManager.getInstance().canUseSecurityFeatures()) {
                return;
            }
            TwoFactorDisableController controller = SceneManager.switchToAndGetController("two-factor-disable");
            controller.setMode("regenerate");
            controller.setUser(SessionManager.getInstance().getCurrentUser());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String nvl(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private void refreshSecuritySection(Utilisateur user) {
        boolean twoFactorEnabled = user.isTwoFactorEnabled();
        if (twoFactorEnabled) {
            String since = user.getTwoFactorEnabledAt() != null
                    ? user.getTwoFactorEnabledAt().toLocalDate().toString()
                    : "date inconnue";
            twoFactorStatusLabel.setText("Authentification a deux facteurs : activee depuis " + since);
        } else {
            twoFactorStatusLabel.setText("Authentification a deux facteurs : desactivee");
        }
        enableTwoFactorBtn.setVisible(!twoFactorEnabled);
        enableTwoFactorBtn.setManaged(!twoFactorEnabled);
        disableTwoFactorBtn.setVisible(twoFactorEnabled);
        disableTwoFactorBtn.setManaged(twoFactorEnabled);
        regenerateBackupCodesBtn.setVisible(twoFactorEnabled);
        regenerateBackupCodesBtn.setManaged(twoFactorEnabled);

        boolean faceEnabled = user.isFaceAuthEnabled();
        if (faceEnabled) {
            String since = user.getFaceRegisteredAt() != null
                    ? user.getFaceRegisteredAt().toLocalDate().toString()
                    : "date inconnue";
            faceAuthStatusLabel.setText("Reconnaissance faciale : activee depuis " + since);
        } else {
            faceAuthStatusLabel.setText("Reconnaissance faciale : desactivee");
        }
        enableFaceAuthBtn.setVisible(!faceEnabled);
        enableFaceAuthBtn.setManaged(!faceEnabled);
        disableFaceAuthBtn.setVisible(faceEnabled);
        disableFaceAuthBtn.setManaged(faceEnabled);
    }
}
