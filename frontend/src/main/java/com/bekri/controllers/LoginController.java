package com.bekri.controllers;

import com.bekri.models.AuthResponse;
import com.bekri.services.ApiClient;
import com.bekri.utils.FormFieldStyles;
import com.bekri.utils.PasswordUiHelper;
import com.bekri.utils.SceneManager;
import com.bekri.utils.SessionManager;
import com.bekri.validation.FormValidators;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

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

    private final ChangeListener<String> recompute = (o, a, b) -> updateSubmitEnabled();

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
        } else if (!invalid) {
            FormFieldStyles.hideErrorLabel(emailErrorLabel);
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
        } else if (!invalid) {
            FormFieldStyles.hideErrorLabel(passwordErrorLabel);
        } else {
            FormFieldStyles.hideErrorLabel(passwordErrorLabel);
        }
    }

    private boolean formValidSilent() {
        return FormValidators.validateEmail(emailField.getText()) == null
                && pwd() != null && !pwd().isEmpty();
    }

    private void updateSubmitEnabled() {
        loginBtn.setDisable(!formValidSilent());
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

        loginBtn.setDisable(true);
        loginBtn.setText("Connexion...");
        hideError();

        new Thread(() -> {
            try {
                AuthResponse response = ApiClient.login(email, password);

                SessionManager.getInstance().setToken(response.getToken());
                SessionManager.getInstance().setCurrentUser(response.getUtilisateur());

                String role = response.getUtilisateur().getRole();

                Platform.runLater(() -> {
                    try {
                        if ("admin".equalsIgnoreCase(role)) {
                            SceneManager.getInstance().switchTo("admin-dashboard");
                        } else {
                            boolean hasProfil = ApiClient.hasProfilPsychologique();
                            if (!hasProfil) {
                                SceneManager.getInstance().switchTo("test");
                            } else {
                                SceneManager.getInstance().switchTo("user-dashboard");
                            }
                        }
                    } catch (Exception e) {
                        showError("Erreur de navigation : " + e.getMessage());
                        resetBtn();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    resetBtn();
                });
            }
        }).start();
    }

    @FXML
    private void goToRegister() {
        try {
            SceneManager.getInstance().switchTo("register");
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void resetBtn() {
        loginBtn.setDisable(false);
        loginBtn.setText("Se connecter");
        updateSubmitEnabled();
    }
}
