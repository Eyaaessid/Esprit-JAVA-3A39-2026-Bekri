package tn.esprit.faceauth.ui;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import tn.esprit.faceauth.FaceAuthResourceExtractor;
import tn.esprit.faceauth.FaceAuthService;
import tn.esprit.faceauth.JSBridge;
import tn.esprit.faceauth.ModelLoader;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;
import tn.esprit.user.enums.UtilisateurStatut;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class FaceLoginController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;
    @FXML private ImageView cameraView;
    @FXML private Button captureBtn;
    @FXML private Button loginBtn;
    @FXML private WebView webView;

    private Webcam webcam;
    private volatile boolean webcamRunning = false;
    private volatile BufferedImage lastFrame = null;

    private Thread webcamThread;
    private double[] capturedDescriptor = null;
    private JSBridge jsBridge;
    private final FaceAuthService faceAuthService = new FaceAuthService();

    @FXML
    public void initialize() {
        loadWebView();
        startWebcam();

        Platform.runLater(() -> {
            if (cameraView.getScene() != null && cameraView.getScene().getWindow() != null) {
                cameraView.getScene().getWindow().setOnCloseRequest(e -> stopWebcam());
            }
        });
    }

    private void loadWebView() {
        WebEngine engine = webView.getEngine();

        jsBridge = new JSBridge(
                () -> {
                    try {
                        engine.executeScript(ModelLoader.buildLoadModelsScript());
                        statusLabel.setText("Chargement des modeles...");
                        statusLabel.setStyle("-fx-text-fill: #1971c2;");
                    } catch (Exception e) {
                        statusLabel.setText("Erreur init: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    }
                },
                () -> {
                    captureBtn.setDisable(false);
                    statusLabel.setText("Pret - regardez la camera et cliquez Capturer.");
                    statusLabel.setStyle("-fx-text-fill: #2d6a4f;");
                },
                descriptor -> {
                    capturedDescriptor = descriptor;
                    loginBtn.setDisable(false);
                    statusLabel.setText("Visage capture ! Cliquez Se connecter.");
                    statusLabel.setStyle("-fx-text-fill: #2d6a4f;");
                },
                error -> {
                    captureBtn.setDisable(false);
                    statusLabel.setText("Erreur: " + error);
                    statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                }
        );

        engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            Platform.runLater(() -> {
                try {
                    JSObject win = (JSObject) engine.executeScript("window");
                    win.setMember("javaBridge", jsBridge);

                    engine.executeScript(
                            "window.console={" +
                                    "log:function(){}, " +
                                    "error:function(){}, " +
                                    "warn:function(){}" +
                                    "};"
                    );

                    engine.executeScript(ModelLoader.buildFetchOverrideScript());

                    java.nio.file.Path tmpDir = FaceAuthResourceExtractor.extractToTemp();
                    String uri = tmpDir.resolve("face-api.min.js").toUri().toString();
                    if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
                        uri = "file:///" + uri.substring(6);
                    }
                    engine.executeScript(
                            "var __s=document.createElement('script');" +
                                    "__s.src=" + toJsString(uri) + ";" +
                                    "document.head.appendChild(__s);"
                    );

                    engine.executeScript(
                            "var __wfa=setInterval(function(){" +
                                    "  if(typeof faceapi!=='undefined'){" +
                                    "    clearInterval(__wfa);" +
                                    "    if(window.javaBridge&&window.javaBridge.onFaceApiReady)" +
                                    "      window.javaBridge.onFaceApiReady();" +
                                    "  }" +
                                    "},100);"
                    );
                } catch (Exception e) {
                    showError("Erreur init: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        try {
            engine.load(FaceAuthResourceExtractor.getHtmlUrl());
        } catch (IOException e) {
            showError("ERREUR: " + e.getMessage());
        }
    }

    private void startWebcam() {
        webcamThread = new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Aucune webcam detectee.");
                        statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    });
                    return;
                }

                webcam.setViewSize(new Dimension(640, 480));
                webcam.open();

                BufferedImage firstFrame = null;
                for (int i = 0; i < 50 && firstFrame == null; i++) {
                    firstFrame = webcam.getImage();
                    if (firstFrame == null) {
                        Thread.sleep(100);
                    }
                }

                if (firstFrame == null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Webcam ouverte mais aucune image recue.");
                        statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    });
                    return;
                }

                lastFrame = firstFrame;
                webcamRunning = true;

                while (webcamRunning) {
                    BufferedImage frame = webcam.getImage();
                    if (frame != null) {
                        lastFrame = frame;
                        var fxImg = SwingFXUtils.toFXImage(frame, null);
                        Platform.runLater(() -> {
                            cameraView.setImage(fxImg);
                            cameraView.setPreserveRatio(true);
                        });
                    }
                    Thread.sleep(33);
                }

            } catch (InterruptedException ignored) {
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur webcam: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    e.printStackTrace();
                });
            }
        }, "webcam-thread");
        webcamThread.setDaemon(true);
        webcamThread.start();
    }

    @FXML
    private void handleCapture() {
        if (!webcamRunning || lastFrame == null) {
            statusLabel.setText("Webcam pas encore prete - patientez un instant.");
            statusLabel.setStyle("-fx-text-fill: #c92a2a;");
            return;
        }

        captureBtn.setDisable(true);
        statusLabel.setText("Analyse du visage en cours...");
        statusLabel.setStyle("-fx-text-fill: #1971c2;");

        BufferedImage snapshot = deepCopy(lastFrame);

        new Thread(() -> {
            try {
                BufferedImage resized = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resized.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(snapshot, 0, 0, 320, 240, null);
                g2d.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                boolean wrote = ImageIO.write(resized, "jpg", baos);

                String dataUrl;
                if (!wrote || baos.size() < 2000) {
                    baos.reset();
                    ImageIO.write(resized, "png", baos);
                    dataUrl = "data:image/png;base64,"
                            + Base64.getEncoder().encodeToString(baos.toByteArray());
                } else {
                    dataUrl = "data:image/jpeg;base64,"
                            + Base64.getEncoder().encodeToString(baos.toByteArray());
                }
                dispatchToJs(dataUrl);

            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur capture: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    captureBtn.setDisable(false);
                    e.printStackTrace();
                });
            }
        }, "capture-thread").start();
    }

    private void dispatchToJs(String dataUrl) {
        Platform.runLater(() -> {
            try {
                JSObject win = (JSObject) webView.getEngine().executeScript("window");
                win.setMember("__pendingImage", dataUrl);
                webView.getEngine().executeScript("processImage(window.__pendingImage);");
                captureBtn.setDisable(false);
            } catch (Exception e) {
                statusLabel.setText("Erreur JS dispatch: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                captureBtn.setDisable(false);
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Veuillez entrer votre email.");
            return;
        }

        double[] desc = capturedDescriptor;
        if (desc == null) {
            showError("Aucun visage capture.");
            return;
        }

        loginBtn.setDisable(true);
        statusLabel.setText("Verification...");
        statusLabel.setStyle("-fx-text-fill: #666;");

        new Thread(() -> {
            FaceAuthService.FaceAuthResult result = faceAuthService.authenticate(email, desc);
            Utilisateur user = faceAuthService.getLastAuthenticatedUser();

            Platform.runLater(() -> {
                loginBtn.setDisable(false);
                switch (result) {
                    case SUCCESS -> {
                        if (user == null) {
                            showError("Erreur de session. Reessayez.");
                            return;
                        }
                        if (user.getStatut() == UtilisateurStatut.BLOQUE || user.getStatut() == UtilisateurStatut.SUPPRIME) {
                            showError("Votre compte a ete suspendu definitivement. Veuillez contacter le support.");
                            return;
                        }
                        if (user.getStatut() == UtilisateurStatut.INACTIF) {
                            showError("Votre compte est inactif. Veuillez utiliser l'ecran de connexion pour demander une reactivation.");
                            return;
                        }
                        stopWebcam();
                        SessionManager.getInstance().setCurrentUser(user);
                        try {
                            SceneManager.switchTo(
                                    user.getRole() == UtilisateurRole.ADMIN
                                            ? "admin-dashboard" : "user-dashboard");
                        } catch (Exception e) {
                            showError("Erreur de navigation.");
                        }
                    }
                    case FACE_MISMATCH -> {
                        capturedDescriptor = null;
                        loginBtn.setDisable(true);
                        captureBtn.setDisable(false);
                        showError("Visage non reconnu. Reessayez.");
                    }
                    case LOCKED_OUT -> {
                        int mins = user != null
                                ? faceAuthService.getRemainingLockoutMinutes(user.getId()) : 15;
                        showError("Compte verrouille. Reessayez dans " + mins + " min.");
                    }
                    case FACE_NOT_ENABLED ->
                            showError("La reconnaissance faciale n'est pas activee pour ce compte.");
                    case EMAIL_NOT_VERIFIED ->
                            showError("Veuillez verifier votre email avant de vous connecter.");
                    case ACCOUNT_NOT_ACTIVE ->
                            showError("Ce compte n'est pas actif.");
                    case USER_NOT_FOUND ->
                            showError("Aucun compte trouve pour cet email.");
                }
            });
        }, "face-login-thread").start();
    }

    @FXML
    private void handleBack() {
        stopWebcam();
        try {
            SceneManager.switchTo("login");
        } catch (Exception ignored) {
        }
    }

    public void stopWebcam() {
        webcamRunning = false;
        if (webcamThread != null) {
            try {
                webcamThread.interrupt();
            } catch (Exception ignored) {
            }
        }
        if (webcam != null && webcam.isOpen()) {
            try {
                webcam.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-text-fill: #c92a2a;");
    }

    private static String toJsString(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(s.length() + 2).append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('\'').toString();
    }
}
