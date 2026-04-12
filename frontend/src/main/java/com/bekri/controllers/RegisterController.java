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
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    @FXML
    private void initialize() {
        PasswordUiHelper.wireToggle(passwordField, passwordPlainField, passwordToggleBtn);
        PasswordUiHelper.wireToggle(confirmPasswordField, confirmPasswordPlainField, confirmPasswordToggleBtn);

        nomField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validateNom(true);
            }
        });
        prenomField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validatePrenom(true);
            }
        });
        emailField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validateEmail(true);
            }
        });
        telField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validateTel(true);
            }
        });
        dateNaissancePicker.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validateDate(true);
            }
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
            if (!focused) {
                validateConfirm(true);
            }
        });
        confirmPasswordPlainField.focusedProperty().addListener((obs, oldV, focused) -> {
            if (!focused) {
                validateConfirm(true);
            }
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
        String err = FormValidators.validateNom(nomField.getText());
        apply(nomField, nomErrorLabel, err, show);
    }

    private void validatePrenom(boolean show) {
        String err = FormValidators.validatePrenom(prenomField.getText());
        apply(prenomField, prenomErrorLabel, err, show);
    }

    private void validateEmail(boolean show) {
        String err = FormValidators.validateEmail(emailField.getText());
        apply(emailField, emailErrorLabel, err, show);
    }

    private void validateTel(boolean show) {
        String err = FormValidators.validateTelephoneOptional(telField.getText());
        apply(telField, telErrorLabel, err, show);
    }

    private void validateDate(boolean show) {
        String err = FormValidators.validateDateNaissanceRequired(dateNaissancePicker.getValue());
        FormFieldStyles.applyDatePickerStyle(dateNaissancePicker, err == null, err != null && show);
        if (show && err != null) {
            FormFieldStyles.showErrorLabel(dateErrorLabel, err);
        } else if (err == null) {
            FormFieldStyles.hideErrorLabel(dateErrorLabel);
        } else {
            FormFieldStyles.hideErrorLabel(dateErrorLabel);
        }
    }

    private void validatePassword(boolean show) {
        String err = FormValidators.validatePasswordRequired(pwd());
        applyDual(passwordField, passwordPlainField, passwordErrorLabel, err, show);
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
        String err = FormValidators.validatePasswordConfirm(p1, p2);
        applyDual(confirmPasswordField, confirmPasswordPlainField, confirmPasswordErrorLabel, err, show);
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
        } else if (!invalid) {
            FormFieldStyles.hideErrorLabel(errLabel);
        } else {
            FormFieldStyles.hideErrorLabel(errLabel);
        }
    }

    private void applyDual(PasswordField h, TextField p, Label errLabel, String err, boolean show) {
        boolean invalid = err != null;
        boolean showInvalid = invalid && show;
        FormFieldStyles.applyInputStyle(h, !invalid, showInvalid);
        FormFieldStyles.applyInputStyle(p, !invalid, showInvalid);
        if (showInvalid) {
            FormFieldStyles.showErrorLabel(errLabel, err);
        } else if (!invalid) {
            FormFieldStyles.hideErrorLabel(errLabel);
        } else {
            FormFieldStyles.hideErrorLabel(errLabel);
        }
    }

    private boolean formValidSilent() {
        if (FormValidators.validateNom(nomField.getText()) != null) {
            return false;
        }
        if (FormValidators.validatePrenom(prenomField.getText()) != null) {
            return false;
        }
        if (FormValidators.validateEmail(emailField.getText()) != null) {
            return false;
        }
        if (FormValidators.validateTelephoneOptional(telField.getText()) != null) {
            return false;
        }
        if (FormValidators.validateDateNaissanceRequired(dateNaissancePicker.getValue()) != null) {
            return false;
        }
        if (FormValidators.validatePasswordRequired(pwd()) != null) {
            return false;
        }
        if (FormValidators.validatePasswordConfirm(pwd(), pwdConfirm()) != null) {
            return false;
        }
        return termsCheckbox.isSelected();
    }

    private void updateSubmitEnabled() {
        registerBtn.setDisable(!formValidSilent());
    }

    private void showGeneralFormError() {
        if (generalFormErrorLabel == null) {
            return;
        }
        FormFieldStyles.showErrorLabel(generalFormErrorLabel, FormValidators.GENERAL_CORRECT);
    }

    private void hideGeneralFormError() {
        if (generalFormErrorLabel == null) {
            return;
        }
        generalFormErrorLabel.setVisible(false);
        generalFormErrorLabel.setManaged(false);
        generalFormErrorLabel.setText("");
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
        hideGeneralFormError();

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        String password = pwd();
        String dateStr = dateNaissance.format(DateTimeFormatter.ISO_LOCAL_DATE);

        registerBtn.setDisable(true);
        registerBtn.setText("Inscription...");
        hideError();

        new Thread(() -> {
            try {
                AuthResponse response = ApiClient.register(nom, prenom, email, password,
                        tel.isEmpty() ? null : tel, dateStr);

                SessionManager.getInstance().setToken(response.getToken());
                SessionManager.getInstance().setCurrentUser(response.getUtilisateur());

                Platform.runLater(() -> {
                    try {
                        SceneManager.getInstance().switchTo("test");
                    } catch (Exception e) {
                        showError("Erreur de navigation.");
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
            SceneManager.getInstance().switchTo("login");
        } catch (Exception e) {
            showError("Erreur de navigation.");
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void resetBtn() {
        registerBtn.setDisable(false);
        registerBtn.setText("Créer mon compte gratuitement");
        updateSubmitEnabled();
    }
}
