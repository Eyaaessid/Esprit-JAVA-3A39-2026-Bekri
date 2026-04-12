package com.bekri.utils;

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

/**
 * Boîtes de dialogue modales stylées (remplace {@link javafx.scene.control.Alert}).
 */
public final class DialogHelper {

    private static final double DIALOG_WIDTH = 420;

    private DialogHelper() {}

    public static void showSuccess(String title, String message) {
        runDialog(() -> showDialog(title, message, "✅", "dialog-header-success", false));
    }

    public static void showError(String title, String message) {
        runDialog(() -> showDialog(title, message, "❌", "dialog-header-error", false));
    }

    /**
     * @return {@code true} si l'utilisateur confirme
     */
    public static boolean showConfirm(String title, String message) {
        if (Platform.isFxApplicationThread()) {
            return showDialog(title, message, "⚠️", "dialog-header-confirm", true);
        }
        AtomicBoolean result = new AtomicBoolean(false);
        CountDownLatchCompat latch = new CountDownLatchCompat();
        Platform.runLater(() -> {
            try {
                result.set(showDialog(title, message, "⚠️", "dialog-header-confirm", true));
            } finally {
                latch.countDown();
            }
        });
        latch.awaitQuietly();
        return result.get();
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
                                      boolean confirmMode) {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);

        Stage owner = SceneManager.getInstance().getPrimaryStage();
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

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("dialog-title");
        titleLbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        header.getChildren().addAll(iconLbl, titleLbl);

        Label msgLbl = new Label(message);
        msgLbl.setWrapText(true);
        msgLbl.getStyleClass().add("dialog-message");
        msgLbl.setMaxWidth(DIALOG_WIDTH - 36);
        VBox.setMargin(msgLbl, new Insets(18, 18, 18, 18));

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 18, 18, 18));

        AtomicBoolean confirmed = new AtomicBoolean(false);

        if (confirmMode) {
            Button cancelBtn = new Button("Annuler");
            cancelBtn.getStyleClass().addAll("btn-secondary", "dialog-footer-btn");
            cancelBtn.setCursor(Cursor.HAND);
            cancelBtn.setOnAction(e -> {
                confirmed.set(false);
                stage.close();
            });

            Button okBtn = new Button("Confirmer");
            okBtn.getStyleClass().addAll("btn-primary", "dialog-footer-btn");
            okBtn.setCursor(Cursor.HAND);
            okBtn.setDefaultButton(true);
            okBtn.setOnAction(e -> {
                confirmed.set(true);
                stage.close();
            });

            btnRow.getChildren().addAll(cancelBtn, okBtn);
        } else {
            Button okBtn = new Button("OK");
            okBtn.getStyleClass().addAll("btn-primary", "dialog-footer-btn");
            okBtn.setCursor(Cursor.HAND);
            okBtn.setDefaultButton(true);
            okBtn.setOnAction(e -> stage.close());
            btnRow.getChildren().add(okBtn);
        }

        root.getChildren().addAll(header, msgLbl, btnRow);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        var css = DialogHelper.class.getResource("/css/bekri.css");
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
