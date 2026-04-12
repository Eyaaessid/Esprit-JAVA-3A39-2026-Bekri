package org.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.stage.Stage;

public class MainFX extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Navigator.init(stage);
        Navigator.showPostsView();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
