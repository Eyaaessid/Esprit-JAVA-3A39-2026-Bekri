package com.bekri;

import com.bekri.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Enable JavaFX CSS error logging — shows EXACTLY which CSS lines fail
        System.setProperty("javafx.css.warn", "true");

        primaryStage.setTitle("Bekri — Bien-être");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setWidth(1100);
        primaryStage.setHeight(700);

        SceneManager.getInstance().setPrimaryStage(primaryStage);
        SceneManager.getInstance().switchTo("login");
    }

    public static void main(String[] args) {
        // Also enable CSS logging via JVM args
        System.setProperty("javafx.css.warn", "true");
        launch(args);
    }
}