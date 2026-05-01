package tn.esprit;

import javafx.application.Application;
import javafx.stage.Stage;
import tn.esprit.views.MainView;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainView mainView = new MainView();
        mainView.show(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
