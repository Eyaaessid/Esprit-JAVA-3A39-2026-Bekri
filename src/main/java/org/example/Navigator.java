package org.example;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.community.model.Post;

import java.io.IOException;

public final class Navigator {
    private static final double DEFAULT_WIDTH = 1500;
    private static final double DEFAULT_HEIGHT = 900;

    private static Stage stage;
    private static final AppState appState = new AppState();

    private Navigator() {
    }

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    public static AppState getAppState() {
        return appState;
    }

    public static void showPostsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/org/example/posts-view.fxml"));
        Parent root = loader.load();
        PostsController controller = loader.getController();
        controller.setAppState(appState);
        setScene(root, "Bekri Community - Posts");
    }

    public static void showPostDetailsView(Post post) throws IOException {
        appState.setCurrentPost(post);
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/org/example/post-details-view.fxml"));
        Parent root = loader.load();
        PostDetailsController controller = loader.getController();
        controller.setAppState(appState);
        setScene(root, "Bekri Community - Post Details");
    }

    public static void showCommentsView(Post post) throws IOException {
        appState.setCurrentPost(post);
        FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/org/example/comments-view.fxml"));
        Parent root = loader.load();
        CommentsController controller = loader.getController();
        controller.setAppState(appState);
        setScene(root, "Bekri Community - Comments");
    }

    private static void setScene(Parent root, String title) {
        double width = stage.getScene() == null ? DEFAULT_WIDTH : stage.getScene().getWidth();
        double height = stage.getScene() == null ? DEFAULT_HEIGHT : stage.getScene().getHeight();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.setMinWidth(1200);
        stage.setMinHeight(780);
        stage.show();
    }
}
