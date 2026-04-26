package tn.esprit.user.ui;

import tn.esprit.user.entity.Utilisateur;
import tn.esprit.shared.FormFieldStyles;
import tn.esprit.shared.PasswordUiHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.shared.FormValidators;
import tn.esprit.user.service.UtilisateurService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminEditUserController implements Initializable {

    private static final String PARTIAL_PWD =
            "Saisissez le mot de passe et la confirmation.";

    @FXML private Label avatarLabel;
    @FXML private Label nameCard;
    @FXML private Label emailCard;
    @FXML private Label roleBadge;
    @FXML private Label telCard;
    @FXML private Label dateCard;
    @FXML private Label subtitleLabel;

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordPlainField;
    @FXML private Button passwordToggleBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordPlainField;
    @FXML private Button confirmPasswordToggleBtn;

    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> statutCombo;
    @FXML private Button saveBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label generalFormErrorLabel;

    @FXML private Label nomErrorLabel;
    @FXML private Label prenomErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label telErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmPasswordErrorLabel;
    @FXML private Label roleErrorLabel;
    @FXML private Label statutErrorLabel;

    private Utilisateur currentUser;
    private String loadedDateIso;

    private final ChangeListener<Object> recomputeListener = (o, a, b) -> {
        updateSubmitEnabled();
        if (saveBtn.isDisabled()
                && generalFormErrorLabel != null
                && generalFormErrorLabel.isVisible()) {
            hideGeneralFormError();
        }
    };

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        roleCombo.setItems(FXCollections.observableArrayList("USER", "COACH", "ADMIN"));
        statutCombo.setItems(FXCollections.observableArrayList("ACTIF", "BLOQUE", "INACTIF"));

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
        passwordField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateOptionalPasswords(true);
            }
        });
        passwordPlainField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateOptionalPasswords(true);
            }
        });
        confirmPasswordField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateOptionalPasswords(true);
            }
        });
        confirmPasswordPlainField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateOptionalPasswords(true);
            }
        });
        roleCombo.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateRole(true);
            }
        });
        statutCombo.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateStatut(true);
            }
        });

        nomField.textProperty().addListener(recomputeListener);
        prenomField.textProperty().addListener(recomputeListener);
        emailField.textProperty().addListener(recomputeListener);
        telField.textProperty().addListener(recomputeListener);
        passwordField.textProperty().addListener(recomputeListener);
        passwordPlainField.textProperty().addListener(recomputeListener);
        confirmPasswordField.textProperty().addListener(recomputeListener);
        confirmPasswordPlainField.textProperty().addListener(recomputeListener);
        roleCombo.valueProperty().addListener(recomputeListener);
        statutCombo.valueProperty().addListener(recomputeListener);

        updateSubmitEnabled();
    }

    public void setUser(Utilisateur user) {
        this.currentUser = user;

        avatarLabel.setText(user.getInitials());
        nameCard.setText(user.getFullName());
        emailCard.setText(user.getEmail());
        subtitleLabel.setText(user.getEmail());

        roleBadge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        roleBadge.setText(capitalize(user.getRoleKey()));
        roleBadge.getStyleClass().addAll("role-badge-small", "badge-" + user.getRoleKey());

        telCard.setText(user.getTelephone() != null ? user.getTelephone() : "—");
        dateCard.setText(user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDate().toString() : "—");

        nomField.setText(nvl(user.getNom()));
        prenomField.setText(nvl(user.getPrenom()));
        emailField.setText(nvl(user.getEmail()));
        telField.setText(nvl(user.getTelephone()));

        loadedDateIso = null;
        if (user.getDateNaissance() != null) {
            loadedDateIso = user.getDateNaissance().toString();
        } else {
            loadedDateIso = "2000-01-01";
        }

        roleCombo.setValue(displayRole(user.getRoleKey()));
        statutCombo.setValue(displayStatut(user.getStatutKey()));

        PasswordUiHelper.clear(passwordField, passwordPlainField);
        PasswordUiHelper.clear(confirmPasswordField, confirmPasswordPlainField);
        FormFieldStyles.hideErrorLabel(nomErrorLabel);
        FormFieldStyles.hideErrorLabel(prenomErrorLabel);
        FormFieldStyles.hideErrorLabel(emailErrorLabel);
        FormFieldStyles.hideErrorLabel(telErrorLabel);
        FormFieldStyles.hideErrorLabel(passwordErrorLabel);
        FormFieldStyles.hideErrorLabel(confirmPasswordErrorLabel);
        FormFieldStyles.hideErrorLabel(roleErrorLabel);
        FormFieldStyles.hideErrorLabel(statutErrorLabel);
        hideGeneralFormError();
        updateSubmitEnabled();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
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

    private String apiStatutFromCombo() {
        String v = statutCombo.getValue();
        if (v == null) {
            return null;
        }
        return switch (v.trim().toUpperCase(Locale.ROOT)) {
            case "ACTIF" -> "actif";
            case "BLOQUE" -> "bloque";
            case "INACTIF" -> "inactif";
            default -> null;
        };
    }

    private static String displayRole(String api) {
        if (api == null) {
            return "USER";
        }
        return switch (api.toLowerCase(Locale.ROOT)) {
            case "user" -> "USER";
            case "coach" -> "COACH";
            case "admin" -> "ADMIN";
            default -> api.toUpperCase(Locale.ROOT);
        };
    }

    private static String displayStatut(String api) {
        if (api == null) {
            return "ACTIF";
        }
        return switch (api.toLowerCase(Locale.ROOT)) {
            case "actif" -> "ACTIF";
            case "bloque" -> "BLOQUE";
            case "inactif" -> "INACTIF";
            case "supprime" -> "INACTIF";
            default -> api.toUpperCase(Locale.ROOT);
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

    private void validateStatut(boolean show) {
        String api = apiStatutFromCombo();
        String err = api == null ? FormValidators.ERR_STATUT : FormValidators.validateStatutRequired(api);
        FormFieldStyles.applyComboStyle(statutCombo, err == null, err != null && show);
        if (show && err != null) {
            FormFieldStyles.showErrorLabel(statutErrorLabel, err);
        } else if (err == null) {
            FormFieldStyles.hideErrorLabel(statutErrorLabel);
        } else {
            FormFieldStyles.hideErrorLabel(statutErrorLabel);
        }
    }

    private void validateOptionalPasswords(boolean show) {
        String p = pwd();
        String c = pwdConfirm();
        boolean pe = p == null || p.isEmpty();
        boolean ce = c == null || c.isEmpty();
        if (pe && ce) {
            FormFieldStyles.applyInputStyle(passwordField, false, false);
            FormFieldStyles.applyInputStyle(passwordPlainField, false, false);
            FormFieldStyles.applyInputStyle(confirmPasswordField, false, false);
            FormFieldStyles.applyInputStyle(confirmPasswordPlainField, false, false);
            FormFieldStyles.hideErrorLabel(passwordErrorLabel);
            FormFieldStyles.hideErrorLabel(confirmPasswordErrorLabel);
            return;
        }
        if (pe || ce) {
            applyDual(passwordField, passwordPlainField, passwordErrorLabel, PARTIAL_PWD, show);
            applyDual(confirmPasswordField, confirmPasswordPlainField, confirmPasswordErrorLabel, PARTIAL_PWD, show);
            return;
        }
        String e1 = FormValidators.validatePasswordRequired(p);
        applyDual(passwordField, passwordPlainField, passwordErrorLabel, e1, show);
        String e2 = FormValidators.validatePasswordConfirm(p, c);
        applyDual(confirmPasswordField, confirmPasswordPlainField, confirmPasswordErrorLabel, e2, show);
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

    private boolean optionalPasswordsValid() {
        String p = pwd();
        String c = pwdConfirm();
        boolean pe = p == null || p.isEmpty();
        boolean ce = c == null || c.isEmpty();
        if (pe && ce) {
            return true;
        }
        if (pe || ce) {
            return false;
        }
        return FormValidators.validatePasswordRequired(p) == null
                && FormValidators.validatePasswordConfirm(p, c) == null;
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
        if (!optionalPasswordsValid()) {
            return false;
        }
        String r = apiRoleFromCombo();
        if (r == null || FormValidators.validateRoleRequired(r) != null) {
            return false;
        }
        String s = apiStatutFromCombo();
        return s != null && FormValidators.validateStatutRequired(s) == null;
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
        validateOptionalPasswords(true);
        validateRole(true);
        validateStatut(true);

        if (!formValidSilent()) {
            showGeneralFormError();
            return;
        }
        hideGeneralFormError();

        String newRole = apiRoleFromCombo();
        String newStatut = apiStatutFromCombo();
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        String telOrNull = tel.isEmpty() ? null : tel;
        String pwdOut = pwd();
        boolean changePwd = pwdOut != null && !pwdOut.isEmpty();

        saveBtn.setDisable(true);
        saveBtn.setText("Enregistrement...");
        hideError();
        hideSuccess();

        new Thread(() -> {
            try {
                Utilisateur updated = utilisateurService.updateUtilisateurAdmin(
                        currentUser.getId(),
                        nom,
                        prenom,
                        email,
                        telOrNull,
                        currentUser.getDateNaissance() != null
                                ? currentUser.getDateNaissance()
                                : UtilisateurService.parseDateIso(loadedDateIso),
                        changePwd ? pwdOut : null,
                        newRole,
                        newStatut);
                this.currentUser = updated;

                Platform.runLater(() -> {
                    showSuccess("Modifications enregistrées avec succès !");
                    setUser(updated);
                    roleBadge.getStyleClass().removeIf(c -> c.startsWith("badge-"));
                    roleBadge.setText(capitalize(updated.getRoleKey()));
                    roleBadge.getStyleClass().addAll("role-badge-small", "badge-" + updated.getRoleKey());
                    saveBtn.setDisable(false);
                    saveBtn.setText("✓  Enregistrer les modifications");
                    updateSubmitEnabled();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    saveBtn.setDisable(false);
                    saveBtn.setText("✓  Enregistrer les modifications");
                    updateSubmitEnabled();
                });
            }
        }).start();
    }

    @FXML
    private void goBack() {
        try {
            SceneManager.switchTo("admin-users");
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
