package tn.esprit.faceauth;

import com.google.gson.Gson;
import javafx.application.Platform;

import java.util.function.Consumer;

public class JSBridge {
    private final Runnable onFaceApiReady;
    private final Runnable onModelsLoaded;
    private final Consumer<double[]> onDescriptorReady;
    private final Consumer<String> onError;
    private final Gson gson = new Gson();

    public JSBridge(
            Runnable onFaceApiReady,
            Runnable onModelsLoaded,
            Consumer<double[]> onDescriptorReady,
            Consumer<String> onError
    ) {
        this.onFaceApiReady = onFaceApiReady;
        this.onModelsLoaded = onModelsLoaded;
        this.onDescriptorReady = onDescriptorReady;
        this.onError = onError;
    }

    // Called by JS poller when faceapi is available
    public void onFaceApiReady() {
        System.out.println("[FaceAuth] onFaceApiReady called from JS ✓");
        Platform.runLater(onFaceApiReady);
    }

    // Called by JS — must be public, non-static
    public void onModelsLoaded() {
        Platform.runLater(onModelsLoaded);
    }

    public void onDescriptorReady(String json) {
        double[] descriptor = gson.fromJson(json, double[].class);
        Platform.runLater(() -> onDescriptorReady.accept(descriptor));
    }

    public void onError(String message) {
        Platform.runLater(() -> onError.accept(message));
    }
}

