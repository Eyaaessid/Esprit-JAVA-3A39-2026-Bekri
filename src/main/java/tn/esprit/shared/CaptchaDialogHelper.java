package tn.esprit.shared;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import tn.esprit.utils.CaptchaService;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CaptchaDialogHelper {

    private CaptchaDialogHelper() {
    }

    public static boolean showCaptchaDialog(CaptchaService captchaService) {
        CaptchaService.CaptchaChallenge challenge = captchaService.generate();
        AtomicBoolean verified = new AtomicBoolean(false);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        if (SceneManager.getPrimaryStage() != null) {
            stage.initOwner(SceneManager.getPrimaryStage());
        }
        stage.initModality(Modality.APPLICATION_MODAL);

        Label titleLabel = new Label("Verification de securite");
        titleLabel.getStyleClass().add("dialog-title");

        Label iconLabel = new Label("?");
        iconLabel.getStyleClass().add("dialog-icon");

        HBox header = new HBox(10, iconLabel, titleLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().addAll("dialog-header-base", "dialog-header-confirm");
        header.setPadding(new Insets(14, 18, 14, 18));

        Label helperLabel = new Label("Confirmez que vous etes bien une personne en repondant a cette question.");
        helperLabel.setWrapText(true);
        helperLabel.setStyle("-fx-text-fill: #667892; -fx-font-size: 12px;");

        Label questionLabel = new Label(challenge.question());
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #22324b;");

        TextField answerField = new TextField();
        answerField.setPromptText("Votre reponse");
        answerField.getStyleClass().add("text-field");
        answerField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");

        VBox content = new VBox(12, helperLabel, questionLabel, answerField, errorLabel);
        content.setPadding(new Insets(18));

        Button cancelButton = new Button("Annuler");
        cancelButton.getStyleClass().addAll("btn-secondary", "dialog-footer-btn");
        cancelButton.setOnAction(event -> stage.close());

        Button verifyButton = new Button("Verifier");
        verifyButton.getStyleClass().addAll("btn-primary", "dialog-footer-btn");
        verifyButton.setDefaultButton(true);
        verifyButton.setOnAction(event -> {
            String input = answerField.getText() == null ? "" : answerField.getText().trim();
            try {
                int answer = Integer.parseInt(input);
                if (!captchaService.verify(answer, challenge.answer())) {
                    errorLabel.setText("Reponse incorrecte. Reessayez.");
                    answerField.clear();
                    answerField.requestFocus();
                    return;
                }
                verified.set(true);
                stage.close();
            } catch (NumberFormatException e) {
                errorLabel.setText("Veuillez entrer un nombre.");
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, spacer, cancelButton, verifyButton);
        actions.setPadding(new Insets(0, 18, 18, 18));
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(header, content, actions);
        root.getStyleClass().add("dialog-root");
        root.setMaxWidth(440);

        StackPane wrapper = new StackPane(root);
        wrapper.setPadding(new Insets(12));

        Scene scene = new Scene(wrapper);
        scene.setFill(Color.TRANSPARENT);
        var css = CaptchaDialogHelper.class.getResource("/css/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.showAndWait();
        return verified.get();
    }
}
