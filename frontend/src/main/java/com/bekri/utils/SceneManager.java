package com.bekri.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class SceneManager {

    private static SceneManager instance;
    private Stage primaryStage;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) instance = new SceneManager();
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void switchTo(String viewName) throws IOException {
        // ── DEBUG ──────────────────────────────────────────────
        URL cssUrl  = getClass().getResource("/css/bekri.css");
        URL fxmlUrl = getClass().getResource("/com/bekri/views/" + viewName + ".fxml");
        System.out.println("=== BEKRI DEBUG ===");
        System.out.println("CSS  URL : " + cssUrl);
        System.out.println("FXML URL : " + fxmlUrl);
        System.out.println("Class location: " + getClass().getProtectionDomain().getCodeSource().getLocation());
        // ── END DEBUG ──────────────────────────────────────────

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("✅ CSS loaded: " + cssUrl.toExternalForm());
        } else {
            System.out.println("❌ CSS NOT FOUND — getResource returned null");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public <T> T switchToAndGetController(String viewName) throws IOException {
        URL cssUrl  = getClass().getResource("/css/bekri.css");
        URL fxmlUrl = getClass().getResource("/com/bekri/views/" + viewName + ".fxml");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("❌ CSS NOT FOUND in switchToAndGetController");
        }

        primaryStage.setScene(scene);
        primaryStage.show();
        return loader.getController();
    }
}