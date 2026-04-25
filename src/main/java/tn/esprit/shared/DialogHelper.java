package tn.esprit.shared;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DialogHelper {

    private static final double DIALOG_WIDTH = 430;

    private DialogHelper() {}

    public static void showSuccess(String title, String message) {
        runDialog(() -> showDialog(title, message, "OK", "dialog-header-success", false, "OK", "Annuler"));
    }

    public static void showError(String title, String message) {
        runDialog(() -> showDialog(title, message, "!", "dialog-header-error", false, "OK", "Annuler"));
    }

    public static void showInfo(String title, String message) {
        runDialog(() -> showDialog(title, message, "i", "dialog-header-info", false, "OK", "Annuler"));
    }

    public static boolean showConfirm(String title, String message) {
        return showConfirm(title, message, "Confirmer", "Annuler");
    }

    public static boolean showConfirm(String title, String message, String confirmLabel, String cancelLabel) {
        if (Platform.isFxApplicationThread()) {
            return showDialog(title, message, "?", "dialog-header-confirm", true, confirmLabel, cancelLabel);
        }
        AtomicBoolean result = new AtomicBoolean(false);
        CountDownLatchCompat latch = new CountDownLatchCompat();
        Platform.runLater(() -> {
            try {
                result.set(showDialog(title, message, "?", "dialog-header-confirm", true, confirmLabel, cancelLabel));
            } finally {
                latch.countDown();
            }
        });
        latch.awaitQuietly();
        return result.get();
    }

    public static boolean showChoice(String title, String message, String primaryLabel, String secondaryLabel) {
        return showConfirm(title, message, primaryLabel, secondaryLabel);
    }

    private static void runDialog(Runnable showOnFx) {
        if (Platform.isFxApplicationThread()) {
            showOnFx.run();
            return;
        }
        CountDownLatchCompat latch = new CountDownLatchCompat();
        Platform.runLater(() -> {
            try {
                showOnFx.run();
            } finally {
                latch.countDown();
            }
        });
        latch.awaitQuietly();
    }

    private static boolean showDialog(String title, String message, String icon, String headerStyle,
                                      boolean confirmMode, String confirmLabel, String cancelLabel) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);

        Stage owner = SceneManager.getPrimaryStage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox();
        root.getStyleClass().add("dialog-root");
        root.setMaxWidth(DIALOG_WIDTH);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("dialog-header-base");
        header.getStyleClass().add(headerStyle);
        header.setPadding(new Insets(14, 18, 14, 18));

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("dialog-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        header.getChildren().addAll(iconLabel, titleLabel);

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("dialog-message");
        messageLabel.setMaxWidth(DIALOG_WIDTH - 36);
        VBox.setMargin(messageLabel, new Insets(18, 18, 18, 18));

        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);
        buttonRow.setPadding(new Insets(0, 18, 18, 18));

        AtomicBoolean confirmed = new AtomicBoolean(false);

        if (confirmMode) {
            Button cancelButton = new Button(cancelLabel);
            cancelButton.getStyleClass().addAll("btn-secondary", "dialog-footer-btn");
            cancelButton.setCursor(Cursor.HAND);
            cancelButton.setOnAction(e -> {
                confirmed.set(false);
                stage.close();
            });

            Button confirmButton = new Button(confirmLabel);
            confirmButton.getStyleClass().addAll("btn-primary", "dialog-footer-btn");
            confirmButton.setCursor(Cursor.HAND);
            confirmButton.setDefaultButton(true);
            confirmButton.setOnAction(e -> {
                confirmed.set(true);
                stage.close();
            });

            buttonRow.getChildren().addAll(cancelButton, confirmButton);
        } else {
            Button okButton = new Button(confirmLabel);
            okButton.getStyleClass().addAll("btn-primary", "dialog-footer-btn");
            okButton.setCursor(Cursor.HAND);
            okButton.setDefaultButton(true);
            okButton.setOnAction(e -> stage.close());
            buttonRow.getChildren().add(okButton);
        }

        root.getChildren().addAll(header, messageLabel, buttonRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        var css = DialogHelper.class.getResource("/css/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setScene(scene);
        stage.setResizable(false);
        stage.showAndWait();
        return confirmed.get();
    }

    private static final class CountDownLatchCompat {
        private final Object lock = new Object();
        private boolean done;

        void countDown() {
            synchronized (lock) {
                done = true;
                lock.notifyAll();
            }
        }

        void awaitQuietly() {
            synchronized (lock) {
                while (!done) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
}
