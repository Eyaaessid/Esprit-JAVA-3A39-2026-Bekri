package tn.esprit.shared;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SceneManager {

    private static Stage primaryStage;

    /**
     * Registered scene keys → classpath FXML path (see {@link #switchTo(String)}).
     */
    private static final Map<String, String> SCENES;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("login", "/fxml/login.fxml");
        m.put("register", "/fxml/register.fxml");
        m.put("user-dashboard", "/fxml/user-dashboard.fxml");
        m.put("admin-dashboard", "/fxml/admin-dashboard.fxml");
        m.put("objectifs", "/fxml/objectifs.fxml");
        m.put("objectif-form", "/fxml/objectif-form.fxml");
        m.put("questions", "/fxml/questions.fxml");
        m.put("question-form", "/fxml/question-form.fxml");
        m.put("profile", "/fxml/profile.fxml");
        m.put("edit-profile", "/fxml/edit-profile.fxml");
        m.put("admin-users", "/fxml/admin-users.fxml");
        m.put("admin-add-user", "/fxml/admin-add-user.fxml");
        m.put("admin-user-detail", "/fxml/admin-user-detail.fxml");
        m.put("admin-edit-user", "/fxml/admin-edit-user.fxml");
        m.put("test", "/fxml/test.fxml");
        m.put("profil-psychologique", "/fxml/profil-psychologique.fxml");
        SCENES = Collections.unmodifiableMap(m);
    }

    private SceneManager() {}

    public static void init(Stage stage) {
        primaryStage = stage;
        applyMinWindowSize(stage);
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    private static void applyMinWindowSize(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.setMinWidth(900);
        stage.setMinHeight(600);
    }

    private static URL resolveFxmlUrl(String viewName) throws IOException {
        String path = SCENES.get(viewName);
        if (path == null) {
            throw new IOException("Scène non enregistrée : \"" + viewName + "\"");
        }
        URL fxmlUrl = SceneManager.class.getResource(path);
        if (fxmlUrl == null) {
            throw new IOException("FXML introuvable : " + path);
        }
        return fxmlUrl;
    }

    public static void switchTo(String viewName) throws IOException {
        URL fxmlUrl = resolveFxmlUrl(viewName);
        URL cssUrl = SceneManager.class.getResource("/css/bekri.css");
        URL appCss = SceneManager.class.getResource("/css/app.css");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        if (appCss != null) {
            scene.getStylesheets().add(appCss.toExternalForm());
        }

        primaryStage.setScene(scene);
        applyMinWindowSize(primaryStage);
        primaryStage.show();
    }

    public static <T> T switchToAndGetController(String viewName) throws IOException {
        URL fxmlUrl = resolveFxmlUrl(viewName);
        URL cssUrl = SceneManager.class.getResource("/css/bekri.css");
        URL appCss = SceneManager.class.getResource("/css/app.css");

        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        Scene scene = new Scene(root);

        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        if (appCss != null) {
            scene.getStylesheets().add(appCss.toExternalForm());
        }

        primaryStage.setScene(scene);
        applyMinWindowSize(primaryStage);
        primaryStage.show();
        return loader.getController();
    }
}
