package tn.esprit;

import javafx.application.Application;
import javafx.stage.Stage;
import tn.esprit.shared.SceneManager;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        SceneManager.init(primaryStage);
        primaryStage.setTitle("PI-JAVA");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        SceneManager.switchTo("login");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
