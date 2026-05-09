package tn.esprit.user.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.CaptchaDialogHelper;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.FormFieldStyles;
import tn.esprit.shared.FormValidators;
import tn.esprit.shared.PasswordUiHelper;
import tn.esprit.shared.PsychologicalProfileNavigation;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurStatut;
import tn.esprit.user.service.UtilisateurService;
import tn.esprit.utils.CaptchaService;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPlainField;
    @FXML private Button passwordToggleBtn;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;
    @FXML private Label generalFormErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Hyperlink resendVerificationLink;

    private final ChangeListener<String> recompute = (o, a, b) -> updateSubmitEnabled();

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final CaptchaService captchaService = new CaptchaService();
    private Utilisateur lastUnverifiedUser;
    private int loginAttempts;
    private boolean cooldownActive;
    private static final int MAX_ATTEMPTS = 5;
    private static final int COOLDOWN_SECONDS = 30;

    @FXML
    private void initialize() {
        PasswordUiHelper.wireToggle(passwordField, passwordPlainField, passwordToggleBtn);

        emailField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validateEmail(true);
            }
        });
        passwordField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validatePassword(true);
            }
        });
        passwordPlainField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validatePassword(true);
            }
        });

        emailField.textProperty().addListener(recompute);
        passwordField.textProperty().addListener(recompute);
        passwordPlainField.textProperty().addListener(recompute);

        updateSubmitEnabled();

        if (resendVerificationLink != null) {
            resendVerificationLink.setVisible(false);
            resendVerificationLink.setManaged(false);
        }
    }

    private String pwd() {
        return PasswordUiHelper.currentPassword(passwordField, passwordPlainField);
    }

    private void validateEmail(boolean show) {
        String err = FormValidators.validateEmail(emailField.getText());
        boolean invalid = err != null;
        boolean showInvalid = invalid && show;
        FormFieldStyles.applyInputStyle(emailField, !invalid, showInvalid);
        if (showInvalid) {
            FormFieldStyles.showErrorLabel(emailErrorLabel, err);
        } else {
            FormFieldStyles.hideErrorLabel(emailErrorLabel);
        }
    }

    private void validatePassword(boolean show) {
        String raw = pwd();
        String err = null;
        if (raw == null || raw.isEmpty()) {
            err = FormValidators.ERR_PASSWORD_LOGIN_REQ;
        }
        boolean invalid = err != null;
        boolean showInvalid = invalid && show;
        FormFieldStyles.applyInputStyle(passwordField, !invalid, showInvalid);
        FormFieldStyles.applyInputStyle(passwordPlainField, !invalid, showInvalid);
        if (showInvalid) {
            FormFieldStyles.showErrorLabel(passwordErrorLabel, err);
        } else {
            FormFieldStyles.hideErrorLabel(passwordErrorLabel);
        }
    }

    private boolean formValidSilent() {
        return FormValidators.validateEmail(emailField.getText()) == null
                && pwd() != null && !pwd().isEmpty();
    }

    private void updateSubmitEnabled() {
        loginBtn.setDisable(cooldownActive || !formValidSilent());
    }

    @FXML
    private void handleLogin() {
        validateEmail(true);
        validatePassword(true);
        if (!formValidSilent()) {
            if (generalFormErrorLabel != null) {
                FormFieldStyles.showErrorLabel(generalFormErrorLabel, FormValidators.GENERAL_CORRECT);
            }
            return;
        }
        if (generalFormErrorLabel != null) {
            generalFormErrorLabel.setVisible(false);
            generalFormErrorLabel.setManaged(false);
        }

        String email = emailField.getText().trim();
        String password = pwd();

        if (loginAttempts >= 2 && !CaptchaDialogHelper.showCaptchaDialog(captchaService)) {
            showInfo("Verification annulee.");
            return;
        }

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion...");
        hideStatus();

        new Thread(() -> {
            try {
                Utilisateur user = utilisateurService.login(email, password);
                if (!user.isVerified()) {
                    lastUnverifiedUser = user;
                    Platform.runLater(() -> {
                        showInfo("Veuillez verifier votre email avant de vous connecter.");
                        if (resendVerificationLink != null) {
                            resendVerificationLink.setVisible(true);
                            resendVerificationLink.setManaged(true);
                        }
                        resetBtn();
                    });
                    return;
                }

                Platform.runLater(() -> {
                    try {
                        if (isSuspended(user)) {
                            DialogHelper.showError("Compte suspendu",
                                    "Votre compte a ete suspendu definitivement. Veuillez contacter le support.");
                            resetBtn();
                            return;
                        }
                        if (user.getStatut() == UtilisateurStatut.INACTIF) {
                            handleInactiveUser(user);
                            resetBtn();
                            return;
                        }
                        if (user.isTwoFactorEnabled()) {
                            TwoFactorLoginController twoFactorController = SceneManager.switchToAndGetController("two-factor-login");
                            twoFactorController.setUser(user);
                        } else {
                            try {
                                utilisateurService.updateLastLogin(user.getId());
                            } catch (Exception e) {
                                System.out.println("[Login] Failed to update last_login_at: " + e.getMessage());
                            }
                            SessionManager.getInstance().setCurrentUser(user);
                            loginAttempts = 0;
                            navigateToDashboard();
                        }
                    } catch (IOException e) {
                        showError("Erreur de navigation : " + e.getMessage());
                        resetBtn();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loginAttempts++;
                    showError(e.getMessage());
                    if (resendVerificationLink != null) {
                        resendVerificationLink.setVisible(false);
                        resendVerificationLink.setManaged(false);
                    }
                    triggerCooldownIfNeeded();
                    resetBtn();
                });
            }
        }).start();
    }

    @FXML
    private void goToForgotPassword() {
        try {
            SceneManager.switchTo("forgot-password");
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    @FXML
    private void goToFaceLogin() {
        try {
            SceneManager.switchTo("face-login");
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    @FXML
    private void handleResendVerification() {
        if (lastUnverifiedUser == null) {
            return;
        }
        try {
            EmailVerificationController controller = SceneManager.switchToAndGetController("email-verification");
            controller.setUser(lastUnverifiedUser);
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    @FXML
    private void handleOpenReactivationRequest() {
        String email = emailField == null ? null : emailField.getText();
        String prefEmail = email == null || email.isBlank() ? null : email.trim();
        InactiveAccountFlowHelper.openSupportScreen(prefEmail, this::showError);
    }

    @FXML
    private void goToRegister() {
        try {
            SceneManager.switchTo("register");
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    private void showError(String message) {
        errorLabel.setText("! " + message);
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showInfo(String message) {
        errorLabel.setText("i " + message);
        errorLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideStatus() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void resetBtn() {
        loginBtn.setDisable(cooldownActive);
        loginBtn.setText("Se connecter");
        updateSubmitEnabled();
    }

    private void triggerCooldownIfNeeded() {
        if (loginAttempts < MAX_ATTEMPTS || cooldownActive) {
            return;
        }
        cooldownActive = true;
        loginBtn.setDisable(true);
        showError("Trop de tentatives. Reessayez dans " + COOLDOWN_SECONDS + " secondes.");
        PauseTransition pause = new PauseTransition(Duration.seconds(COOLDOWN_SECONDS));
        pause.setOnFinished(event -> {
            loginAttempts = 0;
            cooldownActive = false;
            hideStatus();
            updateSubmitEnabled();
        });
        pause.play();
    }

    private void navigateToDashboard() throws IOException {
        PsychologicalProfileNavigation.openPostLoginDestination();
    }

    private boolean isSuspended(Utilisateur user) {
        return user.getStatut() == UtilisateurStatut.BLOQUE
                || user.getStatut() == UtilisateurStatut.SUPPRIME;
    }

    private void handleInactiveUser(Utilisateur user) {
        InactiveAccountFlowHelper.handleInactiveUser(user, this::showError);
    }
}
