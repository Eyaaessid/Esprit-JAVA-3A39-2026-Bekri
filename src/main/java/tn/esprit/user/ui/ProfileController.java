package tn.esprit.user.ui;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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
    @FXML private Button enableTwoFactorBtn;
    @FXML private Button disableTwoFactorBtn;
    @FXML private Button regenerateBackupCodesBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        avatarLabel.setText(user.getInitials());
        fullNameLabel.setText(user.getFullName());
        roleLabel.setText(capitalize(user.getRoleKey()));
        roleLabel.getStyleClass().add("badge-" + user.getRoleKey());

        prenomLabel.setText(nvl(user.getPrenom()));
        nomLabel.setText(nvl(user.getNom()));
        emailLabel.setText(nvl(user.getEmail()));
        telLabel.setText(nvl(user.getTelephone()));
        dateNaissanceLabel.setText(user.getDateNaissance() != null
                ? user.getDateNaissance().toString() : "—");
        statutLabel.setText(user.getStatut() != null ? capitalize(user.getStatutKey()) : "—");
        createdAtLabel.setText(user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDate().toString() : "—");
        refreshTwoFactorSection(user);
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
            SceneManager.switchTo("user-dashboard");
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
    private void goToFaceRegister() {
        try {
            Utilisateur user = SessionManager.getInstance().getCurrentUser();
            if (user == null) return;
            var controller = SceneManager.switchToAndGetController("face-register");
            //noinspection unchecked
            ((tn.esprit.faceauth.ui.FaceRegisterController) controller).setUser(user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTwoFactorSetup() {
        try {
            SceneManager.switchTo("two-factor-setup");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToTwoFactorDisable() {
        try {
            SceneManager.switchTo("two-factor-disable");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToRegenerateBackupCodes() {
        try {
            TwoFactorDisableController controller = SceneManager.switchToAndGetController("two-factor-disable");
            controller.setRegenerateMode(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private void refreshTwoFactorSection(Utilisateur user) {
        boolean enabled = user.isTwoFactorEnabled();
        if (enabled) {
            String since = user.getTwoFactorEnabledAt() != null
                    ? user.getTwoFactorEnabledAt().toLocalDate().toString()
                    : "date inconnue";
            twoFactorStatusLabel.setText("Authentification à deux facteurs : activée depuis " + since);
        } else {
            twoFactorStatusLabel.setText("Authentification à deux facteurs : désactivée");
        }
        enableTwoFactorBtn.setVisible(!enabled);
        enableTwoFactorBtn.setManaged(!enabled);
        disableTwoFactorBtn.setVisible(enabled);
        disableTwoFactorBtn.setManaged(enabled);
        regenerateBackupCodesBtn.setVisible(enabled);
        regenerateBackupCodesBtn.setManaged(enabled);
    }
}
