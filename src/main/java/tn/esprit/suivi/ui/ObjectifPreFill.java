package tn.esprit.suivi.ui;

/**
 * Lightweight static transfer object.
 * WeeklyInsightController writes the suggested objectif title here,
 * then navigates to /fxml/objectifs.fxml.
 * The objectifs controller reads it in initialize() to pre-fill the form.
 *
 * Usage in your objectifs controller:
 *
 *   @Override
 *   public void initialize(URL, ResourceBundle rb) {
 *       String pre = ObjectifPreFill.consumeSuggestedTitle();
 *       if (pre != null && titreField != null) {
 *           titreField.setText(pre);
 *       }
 *       // ... rest of your init
 *   }
 */
public final class ObjectifPreFill {

    private static String suggestedTitle = null;

    private ObjectifPreFill() {}

    /** Called by WeeklyInsightController before navigating to objectifs page. */
    public static void setSuggestedTitle(String title) {
        suggestedTitle = title;
    }

    /**
     * Called once by the objectifs controller in initialize().
     * Returns the title and clears the stored value so it is not reused.
     */
    public static String consumeSuggestedTitle() {
        String val = suggestedTitle;
        suggestedTitle = null;
        return val;
    }
}