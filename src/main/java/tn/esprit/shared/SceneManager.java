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

    private static final Map<String, String> SCENES;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("login", "/fxml/login.fxml");
        m.put("register", "/fxml/register.fxml");
        m.put("forgot-password", "/fxml/forgot_password.fxml");
        m.put("reset-password", "/fxml/reset_password.fxml");
        m.put("email-verification", "/fxml/email_verification.fxml");
        m.put("face-login", "/fxml/face_login.fxml");
        m.put("face-register", "/fxml/face_register.fxml");
        m.put("user-dashboard", "/fxml/user-app-shell.fxml");
        m.put("admin-dashboard", "/fxml/admin-dashboard.fxml");
        m.put("admin-posts", "/fxml/admin-posts.fxml");
        m.put("objectifs", "/fxml/objectifs.fxml");
        m.put("objectif-form", "/fxml/objectif-form.fxml");
        m.put("questions", "/fxml/questions.fxml");
        m.put("question-form", "/fxml/question-form.fxml");
        m.put("profile", "/fxml/profile.fxml");
        m.put("account-settings", "/fxml/account_settings.fxml");
        m.put("edit-profile", "/fxml/edit-profile.fxml");
        m.put("admin-users", "/fxml/user_management.fxml");
        m.put("admin-add-user", "/fxml/admin-add-user.fxml");
        m.put("admin-user-detail", "/fxml/admin-user-detail.fxml");
        m.put("admin-edit-user", "/fxml/admin-edit-user.fxml");
        m.put("admin-reactivation-requests", "/fxml/admin-reactivation-requests.fxml"); // ADDED
        m.put("reactivation-request", "/fxml/reactivation_request.fxml");
        m.put("self-reactivation-code", "/fxml/self_reactivation_code.fxml");
        m.put("test", "/fxml/test.fxml");
        m.put("profil-psychologique", "/fxml/profil-psychologique.fxml");
        m.put("suivi-today", "/fxml/suivi_today.fxml");
        m.put("weekly-insight", "/fxml/weekly-insight.fxml");
        m.put("plan-weekly", "/fxml/plan-weekly.fxml");
        m.put("chat-coach", "/fxml/chat-coach.fxml");
        m.put("two-factor-setup", "/fxml/TwoFactorSetup.fxml");
        m.put("two-factor-disable", "/fxml/TwoFactorDisable.fxml");
        m.put("two-factor-backup-codes", "/fxml/TwoFactorBackupCodes.fxml");
        m.put("two-factor-login", "/fxml/TwoFactorLogin.fxml");
        m.put("evenements-list", "/fxml/evenements-list.fxml");
        m.put("evenement-details", "/fxml/evenement-details.fxml");
        m.put("evenement-form", "/fxml/evenement-form.fxml");
        m.put("mes-participations", "/fxml/mes-participations.fxml");
        m.put("mes-favoris", "/fxml/mes-favoris.fxml");
        m.put("coach-evenements", "/fxml/coach-evenements.fxml");
        m.put("coach-dashboard", "/fxml/coach-dashboard.fxml");
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
        if (stage == null) return;
        stage.setMinWidth(900);
        stage.setMinHeight(600);
    }

    public static void resizePrimaryStage(double width, double height) {
        if (primaryStage == null) return;
        primaryStage.setMaximized(false);
        primaryStage.setWidth(width);
        primaryStage.setHeight(height);
        primaryStage.centerOnScreen();
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

        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        if (appCss != null) scene.getStylesheets().add(appCss.toExternalForm());

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

        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        if (appCss != null) scene.getStylesheets().add(appCss.toExternalForm());

        primaryStage.setScene(scene);
        applyMinWindowSize(primaryStage);
        primaryStage.show();
        return loader.getController();
    }
}
