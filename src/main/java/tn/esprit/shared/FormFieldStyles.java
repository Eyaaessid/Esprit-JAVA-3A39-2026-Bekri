package tn.esprit.shared;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.util.Duration;

/**
 * Bordures valide / invalide et libellés d'erreur sous champ.
 */
public final class FormFieldStyles {

    public static final String FIELD_VALID = "field-valid";
    /** Bordure champ invalide (voir bekri.css .field-error). */
    public static final String FIELD_ERROR = "field-error";

    private FormFieldStyles() {}

    public static void applyInputStyle(TextInputControl field, boolean valid, boolean invalid) {
        field.getStyleClass().removeAll(FIELD_VALID, FIELD_ERROR);
        if (invalid) {
            field.getStyleClass().add(FIELD_ERROR);
        } else if (valid) {
            field.getStyleClass().add(FIELD_VALID);
        }
    }

    public static void applyDatePickerStyle(DatePicker picker, boolean valid, boolean invalid) {
        TextField editor = picker.getEditor();
        editor.getStyleClass().removeAll(FIELD_VALID, FIELD_ERROR);
        if (invalid) {
            editor.getStyleClass().add(FIELD_ERROR);
        } else if (valid) {
            editor.getStyleClass().add(FIELD_VALID);
        }
    }

    public static void applyComboStyle(ComboBox<?> combo, boolean valid, boolean invalid) {
        Node target = combo.lookup(".text-input");
        if (target instanceof TextInputControl tic) {
            tic.getStyleClass().removeAll(FIELD_VALID, FIELD_ERROR);
            if (invalid) {
                tic.getStyleClass().add(FIELD_ERROR);
            } else if (valid) {
                tic.getStyleClass().add(FIELD_VALID);
            }
        } else {
            combo.getStyleClass().removeAll(FIELD_VALID, FIELD_ERROR);
            if (invalid) {
                combo.getStyleClass().add(FIELD_ERROR);
            } else if (valid) {
                combo.getStyleClass().add(FIELD_VALID);
            }
        }
    }

    public static void showErrorLabel(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(220), errorLabel);
        errorLabel.setOpacity(0);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    public static void hideErrorLabel(Label errorLabel) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setText("");
    }
}
