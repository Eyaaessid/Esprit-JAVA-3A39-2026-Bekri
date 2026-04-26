package org.example;

import java.io.IOException;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.api.CommunityApiServer;

public class MainFX extends Application {
    private final CommunityApiServer apiServer = new CommunityApiServer();

    @Override
    public void start(Stage stage) throws IOException {
        Navigator.init(stage);
        Navigator.showPostsView();
        apiServer.start(Navigator.getAppState(), 8086);
    }

    @Override
    public void stop() {
        apiServer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
