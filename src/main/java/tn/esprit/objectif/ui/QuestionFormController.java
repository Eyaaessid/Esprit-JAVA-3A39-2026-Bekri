package tn.esprit.objectif.ui;

import tn.esprit.objectif.model.QuestionEvaluationDto;
import tn.esprit.shared.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.function.Consumer;

public class QuestionFormController {

    @FXML private TextArea         texteField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField        option1Field;
    @FXML private TextField        option2Field;
    @FXML private TextField        option3Field;

    @FXML private Label texteError;
    @FXML private Label categoryError;
    @FXML private Label option1Error;
    @FXML private Label option2Error;
    @FXML private Label option3Error;
    @FXML private Label formTitle;

    private QuestionEvaluationDto existing;
    private Consumer<QuestionEvaluationDto> onSave;

    private static final String BASE    = "field-input";
    private static final String VALID   = "input-valid";
    private static final String INVALID = "input-invalid";

    @FXML
    public void initialize() {
        categoryCombo.getItems().addAll(
                "humeur", "sommeil", "poids", "nutrition", "activite", "hydratation"
        );
        categoryCombo.setConverter(new StringConverter<>() {
            @Override public String toString(String v) {
                if (v == null) return "";
                return switch (v) {
                    case "humeur"      -> "Humeur";
                    case "sommeil"     -> "Sommeil";
                    case "poids"       -> "Poids";
                    case "nutrition"   -> "Nutrition";
                    case "activite"    -> "Activité physique";
                    case "hydratation" -> "Hydratation";
                    default            -> v;
                };
            }
            @Override public String fromString(String s) { return s; }
        });

        texteField.textProperty().addListener((obs, o, n) -> liveTexte());
        option1Field.textProperty().addListener((obs, o, n) -> liveOption(option1Field, option1Error, "L'option 1 est obligatoire."));
        option2Field.textProperty().addListener((obs, o, n) -> liveOption(option2Field, option2Error, "L'option 2 est obligatoire."));
        option3Field.textProperty().addListener((obs, o, n) -> liveOption(option3Field, option3Error, "L'option 3 est obligatoire."));
        categoryCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty()) setValid(categoryCombo, categoryError);
            else reset(categoryCombo, categoryError);
        });
    }

    public void setQuestion(QuestionEvaluationDto dto) {
        this.existing = dto;
        if (dto != null) {
            formTitle.setText("Modifier la question");
            texteField.setText(dto.getTexte());
            categoryCombo.setValue(dto.getCategory());
            option1Field.setText(dto.getOption1());
            option2Field.setText(dto.getOption2());
            option3Field.setText(dto.getOption3());
        } else {
            formTitle.setText("Nouvelle question");
        }
    }

    public void setOnSave(Consumer<QuestionEvaluationDto> onSave) {
        this.onSave = onSave;
    }

    @FXML
    private void handleSave() {
        if (!validateAll()) return;

        QuestionEvaluationDto dto = new QuestionEvaluationDto();
        dto.setTexte(texteField.getText().trim());
        dto.setCategory(categoryCombo.getValue());
        dto.setTypeReponse("choice");
        dto.setOption1(option1Field.getText().trim());
        dto.setOption2(option2Field.getText().trim());
        dto.setOption3(option3Field.getText().trim());
        if (existing != null) {
            dto.setId(existing.getId());
        }

        if (onSave != null) onSave.accept(dto);
        close();
    }

    @FXML private void handleCancel() { close(); }

    @FXML
    private void handleBack() {
        Window w = texteField.getScene() != null ? texteField.getScene().getWindow() : null;
        if (w != null && w != SceneManager.getPrimaryStage()) {
            ((Stage) w).close();
        }
        try {
            SceneManager.switchTo("questions");
        } catch (IOException e) {
            if (w instanceof Stage s) {
                s.close();
            }
        }
    }

    private boolean validateAll() {
        boolean ok = true;

        String texte = str(texteField.getText());
        if (texte.isEmpty())        { setInvalid(texteField, texteError, "Le texte de la question est obligatoire."); ok = false; }
        else if (texte.length() < 5){ setInvalid(texteField, texteError, "La question doit contenir au moins 5 caractères."); ok = false; }
        else if (texte.length()>255){ setInvalid(texteField, texteError, "La question ne peut pas dépasser 255 caractères."); ok = false; }
        else setValid(texteField, texteError);

        if (categoryCombo.getValue() == null || categoryCombo.getValue().isEmpty()) {
            setInvalid(categoryCombo, categoryError, "La catégorie est obligatoire."); ok = false;
        } else setValid(categoryCombo, categoryError);

        if (!validateOption(option1Field, option1Error, "L'option 1 est obligatoire.")) ok = false;
        if (!validateOption(option2Field, option2Error, "L'option 2 est obligatoire.")) ok = false;
        if (!validateOption(option3Field, option3Error, "L'option 3 est obligatoire.")) ok = false;

        return ok;
    }

    private boolean validateOption(TextField f, Label err, String emptyMsg) {
        String v = str(f.getText());
        if (v.isEmpty())      { setInvalid(f, err, emptyMsg); return false; }
        if (v.length() > 100) { setInvalid(f, err, "L'option ne peut pas dépasser 100 caractères."); return false; }
        setValid(f, err); return true;
    }

    private void liveTexte() {
        String t = str(texteField.getText());
        if (t.isEmpty())        reset(texteField, texteError);
        else if (t.length() < 5) setInvalid(texteField, texteError, "La question doit contenir au moins 5 caractères.");
        else if (t.length()>255) setInvalid(texteField, texteError, "La question ne peut pas dépasser 255 caractères.");
        else setValid(texteField, texteError);
    }

    private void liveOption(TextField f, Label err, String emptyMsg) {
        String v = str(f.getText());
        if (v.isEmpty())      reset(f, err);
        else if (v.length()>100) setInvalid(f, err, "L'option ne peut pas dépasser 100 caractères.");
        else setValid(f, err);
    }

    private void setValid(Control c, Label err) {
        c.getStyleClass().removeAll(VALID, INVALID);
        c.getStyleClass().add(VALID);
        err.setText("");
    }
    private void setInvalid(Control c, Label err, String msg) {
        c.getStyleClass().removeAll(VALID, INVALID);
        c.getStyleClass().add(INVALID);
        err.setText(msg);
    }
    private void reset(Control c, Label err) {
        c.getStyleClass().removeAll(VALID, INVALID);
        err.setText("");
    }

    private String str(String s) { return s == null ? "" : s.trim(); }
    private void close() { ((Stage) texteField.getScene().getWindow()).close(); }
}
