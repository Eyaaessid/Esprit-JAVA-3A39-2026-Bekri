package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import tn.esprit.shared.CaptchaDialogHelper;
import tn.esprit.shared.FormFieldStyles;
import tn.esprit.shared.FormValidators;
import tn.esprit.shared.PasswordUiHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.service.UtilisateurService;
import tn.esprit.utils.CaptchaService;

import java.time.LocalDate;

public class RegisterController {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPlainField;
    @FXML private Button passwordToggleBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordPlainField;
    @FXML private Button confirmPasswordToggleBtn;
    @FXML private CheckBox termsCheckbox;
    @FXML private Button registerBtn;
    @FXML private Label errorLabel;
    @FXML private Label generalFormErrorLabel;
    @FXML private Label nomErrorLabel;
    @FXML private Label prenomErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label telErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmPasswordErrorLabel;
    @FXML private Label termsErrorLabel;

    private final ChangeListener<Object> recomputeListener = (o, a, b) -> {
        updateSubmitEnabled();
        if (registerBtn.isDisabled()
                && generalFormErrorLabel != null
                && generalFormErrorLabel.isVisible()) {
            hideGeneralFormError();
        }
    };

    private final UtilisateurService utilisateurService = new UtilisateurService();
    private final CaptchaService captchaService = new CaptchaService();

    @FXML
    private void initialize() {
        PasswordUiHelper.wireToggle(passwordField, passwordPlainField, passwordToggleBtn);
        PasswordUiHelper.wireToggle(confirmPasswordField, confirmPasswordPlainField, confirmPasswordToggleBtn);

        nomField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validateNom(true);
        });
        prenomField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validatePrenom(true);
        });
        emailField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validateEmail(true);
        });
        telField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validateTel(true);
        });
        dateNaissancePicker.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validateDate(true);
        });
        passwordField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validatePassword(true);
                validateConfirm(true);
            }
        });
        passwordPlainField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validatePassword(true);
                validateConfirm(true);
            }
        });
        confirmPasswordField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validateConfirm(true);
        });
        confirmPasswordPlainField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) validateConfirm(true);
        });

        nomField.textProperty().addListener(recomputeListener);
        prenomField.textProperty().addListener(recomputeListener);
        emailField.textProperty().addListener(recomputeListener);
        telField.textProperty().addListener(recomputeListener);
        dateNaissancePicker.valueProperty().addListener(recomputeListener);
        passwordField.textProperty().addListener(recomputeListener);
        passwordPlainField.textProperty().addListener(recomputeListener);
        confirmPasswordField.textProperty().addListener(recomputeListener);
        confirmPasswordPlainField.textProperty().addListener(recomputeListener);
        termsCheckbox.selectedProperty().addListener((o, a, selected) -> {
            if (Boolean.TRUE.equals(selected)) {
                FormFieldStyles.hideErrorLabel(termsErrorLabel);
            }
            updateSubmitEnabled();
        });

        updateSubmitEnabled();
    }

    private String pwd() {
        return PasswordUiHelper.currentPassword(passwordField, passwordPlainField);
    }

    private String pwdConfirm() {
        return PasswordUiHelper.currentPassword(confirmPasswordField, confirmPasswordPlainField);
    }

    private void validateNom(boolean show) {
        apply(nomField, nomErrorLabel, FormValidators.validateNom(nomField.getText()), show);
    }

    private void validatePrenom(boolean show) {
        apply(prenomField, prenomErrorLabel, FormValidators.validatePrenom(prenomField.getText()), show);
    }

    private void validateEmail(boolean show) {
        apply(emailField, emailErrorLabel, FormValidators.validateEmail(emailField.getText()), show);
    }

    private void validateTel(boolean show) {
        apply(telField, telErrorLabel, FormValidators.validateTelephoneOptional(telField.getText()), show);
    }

    private void validateDate(boolean show) {
        String err = FormValidators.validateDateNaissanceRequired(dateNaissancePicker.getValue());
        FormFieldStyles.applyDatePickerStyle(dateNaissancePicker, err == null, err != null && show);
        if (show && err != null) {
            FormFieldStyles.showErrorLabel(dateErrorLabel, err);
        } else {
            FormFieldStyles.hideErrorLabel(dateErrorLabel);
        }
    }

    private void validatePassword(boolean show) {
        applyDual(passwordField, passwordPlainField, passwordErrorLabel,
                FormValidators.validatePasswordRequired(pwd()), show);
    }

    private void validateConfirm(boolean show) {
        String p1 = pwd();
        String p2 = pwdConfirm();
        if (FormValidators.validatePasswordRequired(p1) != null) {
            FormFieldStyles.applyInputStyle(confirmPasswordField, false, false);
            FormFieldStyles.applyInputStyle(confirmPasswordPlainField, false, false);
            FormFieldStyles.hideErrorLabel(confirmPasswordErrorLabel);
            return;
        }
        applyDual(confirmPasswordField, confirmPasswordPlainField, confirmPasswordErrorLabel,
                FormValidators.validatePasswordConfirm(p1, p2), show);
    }

    private void validateTerms(boolean show) {
        if (!termsCheckbox.isSelected()) {
            if (show) {
                FormFieldStyles.showErrorLabel(termsErrorLabel, FormValidators.ERR_TERMS);
            }
        } else {
            FormFieldStyles.hideErrorLabel(termsErrorLabel);
        }
    }

    private void apply(TextField field, Label errLabel, String err, boolean show) {
        boolean invalid = err != null;
        boolean showInvalid = invalid && show;
        FormFieldStyles.applyInputStyle(field, !invalid, showInvalid);
        if (showInvalid) {
            FormFieldStyles.showErrorLabel(errLabel, err);
        } else {
            FormFieldStyles.hideErrorLabel(errLabel);
        }
    }

    private void applyDual(PasswordField hidden, TextField plain, Label errLabel, String err, boolean show) {
        boolean invalid = err != null;
        boolean showInvalid = invalid && show;
        FormFieldStyles.applyInputStyle(hidden, !invalid, showInvalid);
        FormFieldStyles.applyInputStyle(plain, !invalid, showInvalid);
        if (showInvalid) {
            FormFieldStyles.showErrorLabel(errLabel, err);
        } else {
            FormFieldStyles.hideErrorLabel(errLabel);
        }
    }

    private boolean formValidSilent() {
        if (FormValidators.validateNom(nomField.getText()) != null) return false;
        if (FormValidators.validatePrenom(prenomField.getText()) != null) return false;
        if (FormValidators.validateEmail(emailField.getText()) != null) return false;
        if (FormValidators.validateTelephoneOptional(telField.getText()) != null) return false;
        if (FormValidators.validateDateNaissanceRequired(dateNaissancePicker.getValue()) != null) return false;
        if (FormValidators.validatePasswordRequired(pwd()) != null) return false;
        if (FormValidators.validatePasswordConfirm(pwd(), pwdConfirm()) != null) return false;
        return termsCheckbox.isSelected();
    }

    private void updateSubmitEnabled() {
        registerBtn.setDisable(!formValidSilent());
    }

    private void showGeneralFormError() {
        if (generalFormErrorLabel != null) {
            FormFieldStyles.showErrorLabel(generalFormErrorLabel, FormValidators.GENERAL_CORRECT);
        }
    }

    private void hideGeneralFormError() {
        if (generalFormErrorLabel != null) {
            generalFormErrorLabel.setVisible(false);
            generalFormErrorLabel.setManaged(false);
            generalFormErrorLabel.setText("");
        }
    }

    @FXML
    private void handleRegister() {
        validateNom(true);
        validatePrenom(true);
        validateEmail(true);
        validateTel(true);
        validateDate(true);
        validatePassword(true);
        validateConfirm(true);
        validateTerms(true);

        if (!formValidSilent()) {
            showGeneralFormError();
            return;
        }
        if (!CaptchaDialogHelper.showCaptchaDialog(captchaService)) {
            showInfo("Verification annulee.");
            return;
        }
        hideGeneralFormError();

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String password = pwd();

        registerBtn.setDisable(true);
        registerBtn.setText("Inscription...");
        hideStatus();

        new Thread(() -> {
            try {
                Utilisateur newUser = utilisateurService.register(
                        nom, prenom, email, password, tel.isEmpty() ? null : tel, dateNaissance);
                Platform.runLater(() -> {
                    try {
                        EmailVerificationController controller =
                                SceneManager.switchToAndGetController("email-verification");
                        controller.setUser(newUser);
                    } catch (Exception e) {
                        showError("Erreur de navigation.");
                    } finally {
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
    private void goToLogin() {
        try {
            SceneManager.switchTo("login");
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText("\u26a0 " + msg);
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showInfo(String msg) {
        errorLabel.setText("\u2139 " + msg);
        errorLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-size: 12px;");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideStatus() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void resetBtn() {
        registerBtn.setDisable(false);
        registerBtn.setText("Creer mon compte gratuitement");
        updateSubmitEnabled();
    }
}
