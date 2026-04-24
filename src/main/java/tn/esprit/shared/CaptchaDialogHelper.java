package tn.esprit.shared;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import tn.esprit.utils.CaptchaService;

import java.util.Optional;

public final class CaptchaDialogHelper {

    private CaptchaDialogHelper() {
    }

    public static boolean showCaptchaDialog(CaptchaService captchaService) {
        CaptchaService.CaptchaChallenge challenge = captchaService.generate();

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Verification de securite");
        dialog.setHeaderText(null);

        VBox content = new VBox(16);
        content.setStyle("-fx-padding: 24 32 8 32; -fx-background-color: white;");
        content.setAlignment(Pos.CENTER);

        Label icon = new Label("?");
        icon.setStyle("-fx-font-size: 36px; -fx-text-fill: #1e3a5f;");

        Label title = new Label("Verification requise");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e3a5f;");

        Label question = new Label(challenge.question());
        question.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");
        question.setMaxWidth(280);
        question.setWrapText(true);
        question.setTextAlignment(TextAlignment.CENTER);

        TextField answerField = new TextField();
        answerField.setPromptText("Votre reponse...");
        answerField.setStyle("-fx-background-color: #f4f6f9; -fx-background-radius: 8; " +
                "-fx-border-color: #3498db; -fx-border-radius: 8; -fx-border-width: 1.5; " +
                "-fx-padding: 10 14; -fx-font-size: 14px;");
        answerField.setMaxWidth(200);
        answerField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");

        content.getChildren().addAll(icon, title, question, answerField, errorLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 12;");

        ButtonType verifyType = new ButtonType("Verifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(verifyType, cancelType);

        Button verifyBtn = (Button) dialog.getDialogPane().lookupButton(verifyType);
        String primaryStyle = "-fx-background-color: #1e3a5f; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 24; -fx-font-weight: bold;";
        verifyBtn.setStyle(primaryStyle);
        wirePrimaryHover(verifyBtn, primaryStyle);

        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelType);
        cancelBtn.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #555; " +
                "-fx-background-radius: 8; -fx-padding: 8 24;");

        verifyBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String input = answerField.getText() == null ? "" : answerField.getText().trim();
            try {
                int answer = Integer.parseInt(input);
                if (!captchaService.verify(answer, challenge.answer())) {
                    errorLabel.setText("Reponse incorrecte. Reessayez.");
                    answerField.clear();
                    answerField.requestFocus();
                    event.consume();
                }
            } catch (NumberFormatException e) {
                errorLabel.setText("Veuillez entrer un nombre.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> btn == verifyType ? "ok" : null);
        Optional<String> result = dialog.showAndWait();
        return result.isPresent();
    }

    private static void wirePrimaryHover(Button button, String baseStyle) {
        String hoverStyle = baseStyle.replace("#1e3a5f", "#2c5282");
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }
}
