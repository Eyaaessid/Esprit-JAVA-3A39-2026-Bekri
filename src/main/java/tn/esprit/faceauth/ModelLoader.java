package tn.esprit.faceauth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ModelLoader {

    /**
     * Reads a classpath resource and returns it as a base64 string.
     */
    public static String toBase64(String resourcePath) throws IOException {
        try (InputStream in = ModelLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            byte[] bytes = in.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    /**
     * Reads a classpath resource and returns it as a UTF-8 string.
     */
    public static String toString(String resourcePath) throws IOException {
        try (InputStream in = ModelLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Escapes a string value for safe embedding inside a JS single-quoted string literal.
     */
    public static String escapeForJsString(String s) {
        if (s == null) return "";
        return s
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }

    /**
     * Builds a fetch() override that serves all face-api model files from
     * in-memory base64 data.  Must be injected BEFORE face-api.min.js loads.
     *
     * FIX: The previous version embedded raw base64 strings directly into a JS
     * string literal — backslash-heavy base64 chars could corrupt the literal.
     * Now we assign each shard to a JS variable first, then build the map.
     */
    public static String buildFetchOverrideScript() throws IOException {
        // Load manifests as text
        String ssdManifest       = toString("/faceauth/models/ssd_mobilenetv1_model-weights_manifest.json");
        String landmarkManifest  = toString("/faceauth/models/face_landmark_68_model-weights_manifest.json");
        String recognitionManifest = toString("/faceauth/models/face_recognition_model-weights_manifest.json");
        String tinyManifest      = toString("/faceauth/models/tiny_face_detector_model-weights_manifest.json");

        // Load binary shards as base64
        String ssdShard1         = toBase64("/faceauth/models/ssd_mobilenetv1_model-shard1");
        String ssdShard2         = toBase64("/faceauth/models/ssd_mobilenetv1_model-shard2");
        String landmarkShard1    = toBase64("/faceauth/models/face_landmark_68_model-shard1");
        String recognitionShard1 = toBase64("/faceauth/models/face_recognition_model-shard1");
        String recognitionShard2 = toBase64("/faceauth/models/face_recognition_model-shard2");
        String tinyShard1        = toBase64("/faceauth/models/tiny_face_detector_model-shard1");

        // Escape manifest JSON for embedding in a JS string
        String ssdManifestEsc    = escapeForJsString(ssdManifest);
        String lmManifestEsc     = escapeForJsString(landmarkManifest);
        String recManifestEsc    = escapeForJsString(recognitionManifest);
        String tinyManifestEsc   = escapeForJsString(tinyManifest);

        // FIX: Use JSON.parse on an array literal built with btoa'd values so we
        // never embed a raw base64 string inside a JS string literal (avoids
        // accidental escape sequences).  Manifests are embedded as escaped strings
        // since they are valid JSON text.
        return "(function(){\n"
                // --- manifests (text) ---
                + "  var __mSsdMf   = '" + ssdManifestEsc + "';\n"
                + "  var __mLmMf    = '" + lmManifestEsc  + "';\n"
                + "  var __mRecMf   = '" + recManifestEsc  + "';\n"
                + "  var __mTinyMf  = '" + tinyManifestEsc + "';\n"
                // --- binary shards (base64 strings, safe – no escaping needed) ---
                + "  var __mSsd1    = '" + ssdShard1        + "';\n"
                + "  var __mSsd2    = '" + ssdShard2        + "';\n"
                + "  var __mLm1     = '" + landmarkShard1   + "';\n"
                + "  var __mRec1    = '" + recognitionShard1+ "';\n"
                + "  var __mRec2    = '" + recognitionShard2+ "';\n"
                + "  var __mTiny1   = '" + tinyShard1       + "';\n"

                + "  var __bekriModels = {\n"
                + "    'ssd_mobilenetv1_model-weights_manifest.json':   { text: __mSsdMf,   isBin: false },\n"
                + "    'face_landmark_68_model-weights_manifest.json':  { text: __mLmMf,    isBin: false },\n"
                + "    'face_recognition_model-weights_manifest.json':  { text: __mRecMf,   isBin: false },\n"
                + "    'tiny_face_detector_model-weights_manifest.json':{ text: __mTinyMf,  isBin: false },\n"
                + "    'ssd_mobilenetv1_model-shard1':                  { text: __mSsd1,    isBin: true  },\n"
                + "    'ssd_mobilenetv1_model-shard2':                  { text: __mSsd2,    isBin: true  },\n"
                + "    'face_landmark_68_model-shard1':                 { text: __mLm1,     isBin: true  },\n"
                + "    'face_recognition_model-shard1':                 { text: __mRec1,    isBin: true  },\n"
                + "    'face_recognition_model-shard2':                 { text: __mRec2,    isBin: true  },\n"
                + "    'tiny_face_detector_model-shard1':               { text: __mTiny1,   isBin: true  }\n"
                + "  };\n"

                + "  function __b64ToArrayBuffer(b64) {\n"
                + "    var bin = atob(b64);\n"
                + "    var len = bin.length;\n"
                + "    var bytes = new Uint8Array(len);\n"
                + "    for (var i = 0; i < len; i++) bytes[i] = bin.charCodeAt(i);\n"
                + "    return bytes.buffer;\n"
                + "  }\n"

                + "  function __getName(r) {\n"
                + "    try {\n"
                + "      var s = (r && r.url) ? String(r.url) : String(r);\n"
                + "      s = s.split('?')[0].split('#')[0];\n"
                + "      return s.split('/').pop();\n"
                + "    } catch(e) { return String(r); }\n"
                + "  }\n"

                + "  var __origFetch = window.fetch;\n"
                + "  window.fetch = function(resource, init) {\n"
                + "    var name = __getName(resource);\n"
                + "    console.log('[bekri-fetch] intercepted: ' + name);\n"
                + "    if (__bekriModels.hasOwnProperty(name)) {\n"
                + "      var entry = __bekriModels[name];\n"
                + "      if (!entry.isBin) {\n"
                + "        return Promise.resolve(new Response(entry.text,\n"
                + "          { status: 200, headers: { 'Content-Type': 'application/json' } }));\n"
                + "      }\n"
                + "      return Promise.resolve(new Response(__b64ToArrayBuffer(entry.text), { status: 200 }));\n"
                + "    }\n"
                + "    if (__origFetch) return __origFetch(resource, init);\n"
                + "    return Promise.reject(new Error('fetch not available: ' + name));\n"
                + "  };\n"
                + "  console.log('[bekri] fetch override installed ✓');\n"
                + "})();\n";
    }

    /**
     * Builds the JS snippet that loads all three face-api model networks.
     * Must be executed AFTER face-api.js has been loaded and AFTER the fetch
     * override is installed.
     *
     * FIX: After loading, we wait one extra microtask tick (setTimeout 0) before
     * notifying Java so that face-api's internal state is fully settled.
     */
    public static String buildLoadModelsScript() {
        return "(function(){\n"
                + "  async function __loadAllModels() {\n"
                + "    try {\n"
                + "      var st = document.getElementById('status');\n"
                + "      if (st) { st.textContent = 'Chargement des modèles...'; st.className = 'loading'; }\n"
                + "      console.log('[bekri] loadAllModels: faceapi type = ' + typeof faceapi);\n"

                // Use a fake base URL — the fetch override intercepts by filename only
                + "      await faceapi.nets.tinyFaceDetector.loadFromUri('https://bekri.local/models');\n"
                + "      console.log('[bekri] tinyFaceDetector loaded');\n"

                + "      await faceapi.nets.faceLandmark68Net.loadFromUri('https://bekri.local/models');\n"
                + "      console.log('[bekri] faceLandmark68Net loaded');\n"

                + "      await faceapi.nets.faceRecognitionNet.loadFromUri('https://bekri.local/models');\n"
                + "      console.log('[bekri] faceRecognitionNet loaded');\n"

                + "      window.modelsLoaded = true;\n"
                + "      if (st) { st.textContent = 'Prêt. Cliquez sur Capturer.'; st.className = 'success'; }\n"

                // FIX: defer the Java callback by one tick so all internal state is settled
                + "      setTimeout(function() {\n"
                + "        if (window.javaBridge && window.javaBridge.onModelsLoaded)\n"
                + "          window.javaBridge.onModelsLoaded();\n"
                + "      }, 0);\n"

                + "    } catch(e) {\n"
                + "      var st2 = document.getElementById('status');\n"
                + "      if (st2) { st2.textContent = 'Erreur modèles: ' + e.message; st2.className = 'error'; }\n"
                + "      console.error('[bekri] loadAllModels error: ' + e.message);\n"
                + "      if (window.javaBridge && window.javaBridge.onError)\n"
                + "        window.javaBridge.onError('models failed: ' + e.message);\n"
                + "    }\n"
                + "  }\n"
                + "  __loadAllModels();\n"
                + "})();\n";
    }
}