package tn.esprit.pijavafx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class FxApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        URL fxml = getClass().getResource("/ui/main_layout.fxml");
        if (fxml == null) {
            throw new IllegalStateException("Cannot find /ui/main_layout.fxml. Check src/main/resources/ui/");
        }

        FXMLLoader loader = new FXMLLoader(fxml);
        Scene scene = new Scene(loader.load(), 1300, 780);

        URL css = getClass().getResource("/ui/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("PI-JAVA (JavaFX)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}