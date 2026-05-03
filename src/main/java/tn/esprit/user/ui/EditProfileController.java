package tn.esprit.user.ui;

import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.FormFieldStyles;
import tn.esprit.shared.PasswordUiHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.shared.AvatarUrlHelper;
import tn.esprit.shared.FormValidators;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.service.UtilisateurService;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

public class EditProfileController implements Initializable {

    private static final List<String> IMAGE_EXT =
            List.of("*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp");
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    @FXML private ImageView avatarImageView;
    @FXML private StackPane avatarStack;
    @FXML private Label avatarInitialsLabel;
    @FXML private Label changePhotoHintLabel;
    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private Button togglePasswordSectionBtn;
    @FXML private VBox passwordSection;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordPlainField;
    @FXML private Button newPasswordToggleBtn;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private TextField confirmNewPasswordPlainField;
    @FXML private Button confirmNewPasswordToggleBtn;
    @FXML private Button saveBtn;
    @FXML private Button resetBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Label generalFormErrorLabel;
    @FXML private Label prenomErrorLabel;
    @FXML private Label nomErrorLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label telErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label newPasswordErrorLabel;
    @FXML private Label confirmNewPasswordErrorLabel;

    private String origNom;
    private String origPrenom;
    private String origEmail;
    private String origTel;
    private String origDateStr;
    private String origAvatar;
    private Path pendingAvatarFile;
    private Image pendingPreviewImage;

    private final ChangeListener<Object> recompute = (o, a, b) -> updateSubmitEnabled();

    private final UtilisateurService utilisateurService = new UtilisateurService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        PasswordUiHelper.wireToggle(newPasswordField, newPasswordPlainField, newPasswordToggleBtn);
        PasswordUiHelper.wireToggle(confirmNewPasswordField, confirmNewPasswordPlainField, confirmNewPasswordToggleBtn);

        if (avatarImageView != null) {
            double r = 48;
            Circle clip = new Circle(r, r, r);
            avatarImageView.setClip(clip);
            avatarImageView.setFitWidth(96);
            avatarImageView.setFitHeight(96);
            avatarImageView.setPreserveRatio(true);
            avatarImageView.setSmooth(true);
            avatarImageView.setPickOnBounds(true);
            avatarImageView.setVisible(false);
            avatarImageView.setManaged(false);
        }

        Platform.runLater(() -> {
            Utilisateur cached = SessionManager.getInstance().getCurrentUser();
            if (cached != null) {
                applyUserToForm(cached);
            }
        });

        prenomField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validatePrenom(true);
            }
        });
        nomField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused) {
                validateNom(true);
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
        newPasswordField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused && passwordSection.isVisible()) {
                validateNewPassword(true);
                validateConfirmNew(true);
            }
        });
        newPasswordPlainField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused && passwordSection.isVisible()) {
                validateNewPassword(true);
                validateConfirmNew(true);
            }
        });
        confirmNewPasswordField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused && passwordSection.isVisible()) {
                validateConfirmNew(true);
            }
        });
        confirmNewPasswordPlainField.focusedProperty().addListener((obs, ov, focused) -> {
            if (!focused && passwordSection.isVisible()) {
                validateConfirmNew(true);
            }
        });

        prenomField.textProperty().addListener(recompute);
        nomField.textProperty().addListener(recompute);
        emailField.textProperty().addListener(recompute);
        telField.textProperty().addListener(recompute);
        dateNaissancePicker.valueProperty().addListener(recompute);
        newPasswordField.textProperty().addListener(recompute);
        newPasswordPlainField.textProperty().addListener(recompute);
        confirmNewPasswordField.textProperty().addListener(recompute);
        confirmNewPasswordPlainField.textProperty().addListener(recompute);

        if (passwordSection != null) {
            passwordSection.setVisible(false);
            passwordSection.setManaged(false);
        }

        new Thread(() -> {
            try {
                Utilisateur sessionUser = SessionManager.getInstance().getCurrentUser();
                if (sessionUser == null || sessionUser.getId() == null) {
                    return;
                }
                Utilisateur fresh = utilisateurService.getUserById(sessionUser.getId());
                Platform.runLater(() -> applyUserToForm(fresh));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Utilisateur user = SessionManager.getInstance().getCurrentUser();
                    if (user != null) {
                        applyUserToForm(user);
                    }
                });
            }
        }).start();
    }

    private void applyUserToForm(Utilisateur user) {
        SessionManager.getInstance().setCurrentUser(user);
        origNom = nvl(user.getNom());
        origPrenom = nvl(user.getPrenom());
        origEmail = nvl(user.getEmail());
        origTel = nvl(user.getTelephone());
        origDateStr = user.getDateNaissance() != null
                ? user.getDateNaissance().toString()
                : null;
        origAvatar = user.getPhotoProfil();

        prenomField.setText(origPrenom);
        nomField.setText(origNom);
        emailField.setText(origEmail);
        telField.setText(origTel);
        if (user.getDateNaissance() != null) {
            dateNaissancePicker.setValue(user.getDateNaissance());
        } else {
            dateNaissancePicker.setValue(null);
        }
        refreshAvatarDisplay(user);
        clearPasswordFields();
        updateSubmitEnabled();
    }

    private void refreshAvatarDisplay(Utilisateur user) {
        if (avatarImageView == null) {
            return;
        }
        String urlStr = AvatarUrlHelper.toImageUrl(user.getPhotoProfil());
        if (urlStr == null || urlStr.isBlank()) {
            showInitialsFallback(user);
            return;
        }
        Image img = new Image(urlStr, 96, 96, true, true, true);
        if (img.isError()) {
            showInitialsFallback(user);
            return;
        }
        img.errorProperty().addListener((obs, old, err) -> {
            if (Boolean.TRUE.equals(err)) {
                Platform.runLater(() -> showInitialsFallback(user));
            }
        });
        img.progressProperty().addListener((obs, old, p) -> {
            if (p != null && p.doubleValue() >= 1.0) {
                Platform.runLater(() -> {
                    if (img.isError()) {
                        showInitialsFallback(user);
                    } else {
                        applyAvatarImage(img);
                    }
                });
            }
        });
        if (!img.isBackgroundLoading() && !img.isError()) {
            applyAvatarImage(img);
        } else if (img.isError()) {
            showInitialsFallback(user);
        }
    }

    private void applyAvatarImage(Image img) {
        if (avatarImageView == null) {
            return;
        }
        avatarImageView.setImage(img);
        avatarImageView.setVisible(true);
        avatarImageView.setManaged(true);
        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setVisible(false);
            avatarInitialsLabel.setManaged(false);
        }
    }

    private void showInitialsFallback(Utilisateur user) {
        if (avatarImageView != null) {
            avatarImageView.setImage(null);
            avatarImageView.setVisible(false);
            avatarImageView.setManaged(false);
        }
        if (avatarInitialsLabel != null) {
            avatarInitialsLabel.setText(user.getInitials());
            avatarInitialsLabel.setVisible(true);
            avatarInitialsLabel.setManaged(true);
        }
    }

    @FXML
    private void onAvatarClicked(MouseEvent e) {
        FileChooser ch = new FileChooser();
        ch.setTitle("Choisir une photo de profil");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", IMAGE_EXT));
        File f = ch.showOpenDialog(avatarStack.getScene().getWindow());
        if (f == null) {
            return;
        }
        try {
            long len = Files.size(f.toPath());
            if (len > MAX_BYTES) {
                FormFieldStyles.showErrorLabel(errorLabel, "L'image ne doit pas dépasser 2 Mo.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            String probed = Files.probeContentType(f.toPath());
            if (probed == null) {
                probed = "";
            }
            String low = probed.toLowerCase(Locale.ROOT);
            if (!low.equals("image/jpeg") && !low.equals("image/png")
                    && !low.equals("image/gif") && !low.equals("image/webp")) {
                FormFieldStyles.showErrorLabel(errorLabel,
                        "Format d'image non supporté (JPEG, PNG, GIF, WEBP uniquement).");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }
            pendingAvatarFile = f.toPath();
            pendingPreviewImage = new Image(f.toURI().toString(), 192, 192, true, true);
            avatarImageView.setImage(pendingPreviewImage);
            avatarImageView.setVisible(true);
            avatarImageView.setManaged(true);
            avatarInitialsLabel.setVisible(false);
            avatarInitialsLabel.setManaged(false);
            hideError();
            hideSuccess();
        } catch (Exception ex) {
            FormFieldStyles.showErrorLabel(errorLabel, ex.getMessage());
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
        updateSubmitEnabled();
    }

    @FXML
    private void togglePasswordSection() {
        boolean show = !passwordSection.isVisible();
        passwordSection.setVisible(show);
        passwordSection.setManaged(show);
        togglePasswordSectionBtn.setText(show ? "Masquer le changement de mot de passe" : "Changer le mot de passe");
        if (!show) {
            clearPasswordFields();
            FormFieldStyles.hideErrorLabel(newPasswordErrorLabel);
            FormFieldStyles.hideErrorLabel(confirmNewPasswordErrorLabel);
        }
        updateSubmitEnabled();
    }

    private void clearPasswordFields() {
        PasswordUiHelper.clear(newPasswordField, newPasswordPlainField);
        PasswordUiHelper.clear(confirmNewPasswordField, confirmNewPasswordPlainField);
    }

    private String newPwd() {
        return PasswordUiHelper.currentPassword(newPasswordField, newPasswordPlainField);
    }

    private String newPwdConfirm() {
        return PasswordUiHelper.currentPassword(confirmNewPasswordField, confirmNewPasswordPlainField);
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
        LocalDate d = dateNaissancePicker.getValue();
        String err = FormValidators.validateDateNaissanceOptional(d);
        FormFieldStyles.applyDatePickerStyle(dateNaissancePicker, err == null, err != null && show);
        if (show && err != null) {
            FormFieldStyles.showErrorLabel(dateErrorLabel, err);
        } else if (err == null) {
            FormFieldStyles.hideErrorLabel(dateErrorLabel);
        } else {
            FormFieldStyles.hideErrorLabel(dateErrorLabel);
        }
    }

    private void validateNewPassword(boolean show) {
        if (!passwordSection.isVisible()) {
            FormFieldStyles.hideErrorLabel(newPasswordErrorLabel);
            return;
        }
        String err = FormValidators.validatePasswordOptional(newPwd());
        applyDual(newPasswordField, newPasswordPlainField, newPasswordErrorLabel, err, show);
    }

    private void validateConfirmNew(boolean show) {
        if (!passwordSection.isVisible()) {
            FormFieldStyles.hideErrorLabel(confirmNewPasswordErrorLabel);
            return;
        }
        String p1 = newPwd();
        String p2 = newPwdConfirm();
        if (p1.isEmpty() && p2.isEmpty()) {
            FormFieldStyles.applyInputStyle(confirmNewPasswordField, true, false);
            FormFieldStyles.applyInputStyle(confirmNewPasswordPlainField, true, false);
            FormFieldStyles.hideErrorLabel(confirmNewPasswordErrorLabel);
            return;
        }
        String errOpt = FormValidators.validatePasswordOptional(p1);
        if (errOpt != null) {
            applyDual(confirmNewPasswordField, confirmNewPasswordPlainField, confirmNewPasswordErrorLabel,
                    FormValidators.ERR_PASSWORD_MATCH, show);
            return;
        }
        String err = FormValidators.validatePasswordConfirm(p1, p2);
        applyDual(confirmNewPasswordField, confirmNewPasswordPlainField, confirmNewPasswordErrorLabel, err, show);
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
        if (FormValidators.validateDateNaissanceOptional(dateNaissancePicker.getValue()) != null) {
            return false;
        }
        if (!passwordSection.isVisible()) {
            return true;
        }
        String p1 = newPwd();
        String p2 = newPwdConfirm();
        if (p1.isEmpty() && p2.isEmpty()) {
            return true;
        }
        if (FormValidators.validatePasswordOptional(p1) != null) {
            return false;
        }
        return FormValidators.validatePasswordConfirm(p1, p2) == null;
    }

    private void updateSubmitEnabled() {
        saveBtn.setDisable(!formValidSilent());
    }

    @FXML
    private void handleSave() {
        validateNom(true);
        validatePrenom(true);
        validateEmail(true);
        validateTel(true);
        validateDate(true);
        if (passwordSection.isVisible()) {
            validateNewPassword(true);
            validateConfirmNew(true);
        }
        if (!formValidSilent()) {
            FormFieldStyles.showErrorLabel(generalFormErrorLabel, FormValidators.GENERAL_CORRECT);
            return;
        }
        FormFieldStyles.hideErrorLabel(generalFormErrorLabel);

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        LocalDate dateVal = dateNaissancePicker.getValue();

        final String pwdChange =
                passwordSection.isVisible() && !newPwd().isEmpty() ? newPwd() : null;

        saveBtn.setDisable(true);
        saveBtn.setText("Enregistrement...");
        hideError();
        hideSuccess();

        new Thread(() -> {
            try {
                Utilisateur session = SessionManager.getInstance().getCurrentUser();
                if (session == null || session.getId() == null) {
                    throw new IllegalStateException("Session invalide");
                }
                String avatarForPut = origAvatar;
                if (pendingAvatarFile != null) {
                    Path dir = Path.of(System.getProperty("user.home"), ".pi-java-avatars");
                    Files.createDirectories(dir);
                    String ext = getExtension(pendingAvatarFile.getFileName().toString());
                    Path dest = dir.resolve(UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext));
                    Files.copy(pendingAvatarFile, dest);
                    avatarForPut = dest.toAbsolutePath().toString();
                    pendingAvatarFile = null;
                }
                Utilisateur updated = utilisateurService.updateProfile(
                        session.getId(),
                        nom,
                        prenom,
                        email,
                        tel.isEmpty() ? null : tel,
                        dateVal,
                        avatarForPut,
                        pwdChange);
                SessionManager.getInstance().setCurrentUser(updated);
                Platform.runLater(() -> {
                    applyUserToForm(updated);
                    showSuccess("Profil mis à jour avec succès !");
                    saveBtn.setDisable(false);
                    saveBtn.setText("✓  Enregistrer les modifications");
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

    private static String getExtension(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : "";
    }

    @FXML
    private void handleReset() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            applyUserToForm(user);
        }
        pendingAvatarFile = null;
        pendingPreviewImage = null;
        hideError();
        hideSuccess();
        FormFieldStyles.hideErrorLabel(generalFormErrorLabel);
        if (passwordSection != null) {
            passwordSection.setVisible(false);
            passwordSection.setManaged(false);
            togglePasswordSectionBtn.setText("Changer le mot de passe");
        }
        updateSubmitEnabled();
    }

    @FXML
    private void goBack() {
        try {
            if (SessionManager.getInstance().isAdmin()) {
                SceneManager.switchTo("admin-dashboard");
            } else {
                SceneManager.switchTo("user-dashboard");
            }
        } catch (Exception e) {
            DialogHelper.showError("Navigation", e.getMessage());
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

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
