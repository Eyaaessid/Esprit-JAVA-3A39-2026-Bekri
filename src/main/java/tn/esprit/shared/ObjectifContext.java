package tn.esprit.shared;

public class ObjectifContext {
    private static String pendingType;
    private static String pendingLabel;

    public static String getPendingType() {
        return pendingType;
    }

    public static String getPendingLabel() {
        return pendingLabel;
    }

    public static void setPendingType(String t) {
        pendingType = t;
    }

    public static void setPendingLabel(String l) {
        pendingLabel = l;
    }

    public static void clear() {
        pendingType = null;
        pendingLabel = null;
    }
}
