package tn.esprit.faceauth.ui;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

public class FaceRegisterController {

    @FXML private ImageView cameraView;
    @FXML private WebView webView;
    @FXML private Button captureBtn;
    @FXML private Button registerBtn;
    @FXML private Button disableBtn;
    @FXML private Label statusLabel;

    private Webcam webcam;
    private volatile boolean webcamRunning = false;
    private volatile BufferedImage lastFrame = null;

    private double[] capturedDescriptor = null;
    private Utilisateur user;
    private JSBridge jsBridge;
    private Thread webcamThread;

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

    public void setUser(Utilisateur user) {
        this.user = user;
        if (user != null && user.isFaceAuthEnabled()) {
            disableBtn.setVisible(true);
            statusLabel.setText("Visage déjà enregistré. Vous pouvez le remplacer ou le désactiver.");
        }
    }

    private void loadWebView() {
        WebEngine engine = webView.getEngine();

        jsBridge = new JSBridge(
                () -> {
                    try {
                        engine.executeScript(ModelLoader.buildLoadModelsScript());
                        statusLabel.setText("Chargement des modèles...");
                        statusLabel.setStyle("-fx-text-fill: #1971c2;");
                    } catch (Exception e) {
                        statusLabel.setText("Erreur init: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    }
                },
                () -> {
                    captureBtn.setDisable(false);
                    statusLabel.setText("Prêt : regardez la caméra puis cliquez sur Capturer.");
                    statusLabel.setStyle("-fx-text-fill: #2d6a4f;");
                },
                descriptor -> {
                    capturedDescriptor = descriptor;
                    registerBtn.setDisable(false);
                    statusLabel.setText("Visage capturé. Cliquez sur Enregistrer pour confirmer.");
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

                    Path tmpDir = FaceAuthResourceExtractor.extractToTemp();
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
                    statusLabel.setText("Erreur init: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    e.printStackTrace();
                }
            });
        });

        try {
            engine.load(FaceAuthResourceExtractor.getHtmlUrl());
        } catch (IOException e) {
            statusLabel.setText("ERREUR: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void startWebcam() {
        webcamThread = new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Aucune webcam détectée.");
                        statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    });
                    return;
                }

                webcam.setViewSize(new Dimension(640, 480));
                webcam.open();

                BufferedImage firstFrame = null;
                for (int attempt = 0; attempt < 50 && firstFrame == null; attempt++) {
                    firstFrame = webcam.getImage();
                    if (firstFrame == null) {
                        Thread.sleep(100);
                    }
                }

                if (firstFrame == null) {
                    Platform.runLater(() -> {
                        statusLabel.setText("Webcam ouverte mais aucune image reçue.");
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
            statusLabel.setText("Webcam pas encore prête. Patientez un instant.");
            statusLabel.setStyle("-fx-text-fill: #c92a2a;");
            return;
        }

        captureBtn.setDisable(true);
        statusLabel.setText("Analyse du visage en cours...");
        statusLabel.setStyle("-fx-text-fill: #1971c2;");

        final BufferedImage snapshot = deepCopy(lastFrame);

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
                    dataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
                } else {
                    dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
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
    private void handleRegister() {
        double[] toSave = capturedDescriptor;
        if (toSave == null) {
            statusLabel.setText("Aucun visage capturé. Cliquez d'abord sur Capturer.");
            statusLabel.setStyle("-fx-text-fill: #c92a2a;");
            return;
        }
        if (user == null || user.getId() == null) {
            statusLabel.setText("Erreur: utilisateur non défini.");
            statusLabel.setStyle("-fx-text-fill: #c92a2a;");
            return;
        }

        registerBtn.setDisable(true);
        statusLabel.setText("Enregistrement en cours...");
        statusLabel.setStyle("-fx-text-fill: #666;");

        new Thread(() -> {
            try {
                faceAuthService.storeFaceDescriptor(user.getId(), toSave);
                user.setFaceAuthEnabled(true);
                user.setFaceRegisteredAt(java.time.LocalDateTime.now());
                Platform.runLater(() -> {
                    SessionManager.getInstance().setCurrentUser(user);
                    statusLabel.setText("Reconnaissance faciale activée avec succès.");
                    statusLabel.setStyle("-fx-text-fill: #2d6a4f;");
                    disableBtn.setVisible(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    registerBtn.setDisable(false);
                    statusLabel.setText("Erreur enregistrement: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                    e.printStackTrace();
                });
            }
        }, "register-thread").start();
    }

    @FXML
    private void handleDisable() {
        if (user == null || user.getId() == null) {
            return;
        }
        new Thread(() -> {
            try {
                faceAuthService.disableFaceAuth(user.getId());
                user.setFaceAuthEnabled(false);
                user.setFaceDescriptor(null);
                user.setFaceRegisteredAt(null);
                Platform.runLater(() -> {
                    SessionManager.getInstance().setCurrentUser(user);
                    disableBtn.setVisible(false);
                    capturedDescriptor = null;
                    registerBtn.setDisable(true);
                    captureBtn.setDisable(true);
                    statusLabel.setText("Reconnaissance faciale désactivée.");
                    statusLabel.setStyle("-fx-text-fill: orange;");
                    loadWebView();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Erreur: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #c92a2a;");
                });
            }
        }, "disable-thread").start();
    }

    @FXML
    private void handleBack() {
        stopWebcam();
        try {
            SceneManager.switchTo("profile");
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

    private static String toJsString(String s) {
        if (s == null) return "null";
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
