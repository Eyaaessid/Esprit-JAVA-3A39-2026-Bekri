package com.bekri.controllers;

import com.bekri.services.ApiClient;
import com.bekri.utils.FormFieldStyles;
import com.bekri.utils.PasswordUiHelper;
import com.bekri.utils.SceneManager;
import com.bekri.validation.FormValidators;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminAddUserController implements Initializable {

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
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button saveBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label generalFormErrorLabel;

    @FXML private Label nomErrorLabel;
    @FXML private Label prenomErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label telErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmPasswordErrorLabel;
    @FXML private Label roleErrorLabel;

    private final ChangeListener<Object> recomputeListener = (o, a, b) -> {
        updateSubmitEnabled();
        if (saveBtn.isDisabled()
                && generalFormErrorLabel != null
                && generalFormErrorLabel.isVisible()) {
            hideGeneralFormError();
        }
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.setItems(FXCollections.observableArrayList("USER", "COACH", "ADMIN"));
        roleCombo.setValue("USER");

        PasswordUiHelper.wireToggle(passwordField, passwordPlainField, passwordToggleBtn);
        PasswordUiHelper.wireToggle(confirmPasswordField, confirmPasswordPlainField, confirmPasswordToggleBtn);

        nomField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateNom(true);
            }
        });
        prenomField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validatePrenom(true);
            }
        });
        emailField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateEmail(true);
            }
        });
        telField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateTel(true);
            }
        });
        dateNaissancePicker.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateDate(true);
            }
        });
        passwordField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validatePassword(true);
                validateConfirm(true);
            }
        });
        passwordPlainField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validatePassword(true);
                validateConfirm(true);
            }
        });
        confirmPasswordField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateConfirm(true);
            }
        });
        confirmPasswordPlainField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateConfirm(true);
            }
        });
        roleCombo.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateRole(true);
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
        roleCombo.valueProperty().addListener(recomputeListener);

        updateSubmitEnabled();
    }

    private String pwd() {
        return PasswordUiHelper.currentPassword(passwordField, passwordPlainField);
    }

    private String pwdConfirm() {
        return PasswordUiHelper.currentPassword(confirmPasswordField, confirmPasswordPlainField);
    }

    private String apiRoleFromCombo() {
        String v = roleCombo.getValue();
        if (v == null) {
            return null;
        }
        return switch (v.trim().toUpperCase(Locale.ROOT)) {
            case "USER" -> "user";
            case "COACH" -> "coach";
            case "ADMIN" -> "admin";
            default -> null;
        };
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
        } else if (err == null) {
            FormFieldStyles.hideErrorLabel(dateErrorLabel);
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

    private void validateRole(boolean show) {
        String api = apiRoleFromCombo();
        String err = api == null ? FormValidators.ERR_ROLE : FormValidators.validateRoleRequired(api);
        FormFieldStyles.applyComboStyle(roleCombo, err == null, err != null && show);
        if (show && err != null) {
            FormFieldStyles.showErrorLabel(roleErrorLabel, err);
        } else if (err == null) {
            FormFieldStyles.hideErrorLabel(roleErrorLabel);
        } else {
            FormFieldStyles.hideErrorLabel(roleErrorLabel);
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
        String api = apiRoleFromCombo();
        return api != null && FormValidators.validateRoleRequired(api) == null;
    }

    private void updateSubmitEnabled() {
        saveBtn.setDisable(!formValidSilent());
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
    private void handleSave() {
        validateNom(true);
        validatePrenom(true);
        validateEmail(true);
        validateTel(true);
        validateDate(true);
        validatePassword(true);
        validateConfirm(true);
        validateRole(true);

        if (!formValidSilent()) {
            showGeneralFormError();
            return;
        }
        hideGeneralFormError();

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        LocalDate date = dateNaissancePicker.getValue();
        String password = pwd();
        String role = apiRoleFromCombo();
        String dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE);

        saveBtn.setDisable(true);
        saveBtn.setText("Création...");
        hideError();
        hideSuccess();

        new Thread(() -> {
            try {
                ApiClient.createUtilisateur(nom, prenom, email, password,
                        tel.isEmpty() ? null : tel, dateStr, role);
                Platform.runLater(() -> {
                    showSuccess("Utilisateur créé avec succès !");
                    nomField.clear();
                    prenomField.clear();
                    emailField.clear();
                    telField.clear();
                    dateNaissancePicker.setValue(null);
                    PasswordUiHelper.clear(passwordField, passwordPlainField);
                    PasswordUiHelper.clear(confirmPasswordField, confirmPasswordPlainField);
                    roleCombo.setValue("USER");
                    saveBtn.setDisable(false);
                    saveBtn.setText("+ Créer l'utilisateur");
                    updateSubmitEnabled();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    saveBtn.setDisable(false);
                    saveBtn.setText("+ Créer l'utilisateur");
                    updateSubmitEnabled();
                });
            }
        }).start();
    }

    @FXML
    private void goBack() {
        try {
            SceneManager.getInstance().switchTo("admin-dashboard");
        } catch (Exception e) {
            e.printStackTrace();
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

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void hideSuccess() {
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
