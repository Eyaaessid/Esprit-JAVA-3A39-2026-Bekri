package tn.esprit.community.ui;

import java.io.IOException;

import javafx.application.Application;
import javafx.stage.Stage;
import tn.esprit.community.api.CommunityApiServer;
import tn.esprit.community.core.CommunityNavigator;

public class CommunityStandaloneApp extends Application {
    private final CommunityApiServer apiServer = new CommunityApiServer();

    @Override
    public void start(Stage stage) throws IOException {
        CommunityNavigator.init(stage);
        CommunityNavigator.showPostsView();
        apiServer.start(CommunityNavigator.getContext(), 8086);
    }

    @Override
    public void stop() {
        apiServer.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}




