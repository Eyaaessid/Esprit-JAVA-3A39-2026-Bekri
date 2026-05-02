package tn.esprit.shared;

public class SceneData {
    private static final SceneData INSTANCE = new SceneData();
    private Object data;

    private SceneData() {
    }

    public static SceneData getInstance() {
        return INSTANCE;
    }

    public void set(Object data) {
        this.data = data;
    }

    public <T> T get(Class<T> type) {
        if (type.isInstance(data)) {
            return type.cast(data);
        }
        return null;
    }

    public void clear() {
        this.data = null;
    }
}
