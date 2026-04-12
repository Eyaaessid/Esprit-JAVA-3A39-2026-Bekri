package com.bekri.utils;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Bascule affichage mot de passe (PasswordField ↔ TextField).
 */
public final class PasswordUiHelper {

    private PasswordUiHelper() {}

    public static void wireToggle(PasswordField hidden, TextField plain, Button toggleBtn) {
        plain.setManaged(false);
        plain.setVisible(false);
        toggleBtn.setFocusTraversable(false);
        toggleBtn.setText("👁");
        toggleBtn.getStyleClass().addAll("eye-button", "password-toggle-btn");

        toggleBtn.setOnAction(e -> {
            if (plain.isVisible()) {
                hidden.setText(plain.getText());
                plain.setVisible(false);
                plain.setManaged(false);
                hidden.setVisible(true);
                hidden.setManaged(true);
                toggleBtn.setText("👁");
            } else {
                plain.setText(hidden.getText());
                hidden.setVisible(false);
                hidden.setManaged(false);
                plain.setVisible(true);
                plain.setManaged(true);
                toggleBtn.setText("🙈");
            }
        });
    }

    public static String currentPassword(PasswordField hidden, TextField plain) {
        return plain.isVisible() ? plain.getText() : hidden.getText();
    }

    public static void clear(PasswordField hidden, TextField plain) {
        hidden.clear();
        plain.clear();
        if (plain.isVisible()) {
            hidden.setText("");
            plain.setVisible(false);
            plain.setManaged(false);
            hidden.setVisible(true);
            hidden.setManaged(true);
        }
    }
}
