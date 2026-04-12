package tn.esprit.pijavafx.ui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import tn.esprit.pijavafx.model.ObjectifBienEtreDto;

import java.util.Map;

public class ObjectifFormController {

    @FXML private TextField        titreField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> statutCombo;
    @FXML private TextArea         descriptionArea;
    @FXML private TextField        valeurCibleField;
    @FXML private TextField        valeurActuelleField;
    @FXML private DatePicker       dateDebutPicker;
    @FXML private DatePicker       dateFinPicker;
    @FXML private TextField        slugField;

    @FXML private Label titreError;
    @FXML private Label typeError;
    @FXML private Label statutError;
    @FXML private Label valeurCibleError;
    @FXML private Label valeurActuelleError;
    @FXML private Label dateDebutError;
    @FXML private Label dateFinError;

    private ObjectifBienEtreDto result = null;

    private static final String VALID   = "input-valid";
    private static final String INVALID = "input-invalid";

    @FXML
    public void initialize() {
        typeCombo.getItems().addAll("humeur","sommeil","poids","nutrition","activite","hydratation");
        typeCombo.setConverter(new StringConverter<>() {
            private final Map<String,String> L = Map.of(
                    "humeur","Humeur","sommeil","Sommeil","poids","Poids",
                    "nutrition","Nutrition","activite","Activité physique","hydratation","Hydratation");
            @Override public String toString(String s)   { return s==null?"":L.getOrDefault(s,s); }
            @Override public String fromString(String s) { return s; }
        });

        statutCombo.getItems().addAll("en_cours","atteint","abandonne");
        statutCombo.setConverter(new StringConverter<>() {
            private final Map<String,String> L = Map.of(
                    "en_cours","En cours","atteint","Atteint","abandonne","Abandonné");
            @Override public String toString(String s)   { return s==null?"":L.getOrDefault(s,s); }
            @Override public String fromString(String s) { return s; }
        });

        // Live listeners
        titreField.textProperty().addListener((o,a,b)          -> liveTitre());
        valeurCibleField.textProperty().addListener((o,a,b)    -> liveCible());
        valeurActuelleField.textProperty().addListener((o,a,b) -> liveActuelle());
        typeCombo.valueProperty().addListener((o,a,b)   -> { if(b!=null) setValid(typeCombo,typeError);     else reset(typeCombo,typeError); });
        statutCombo.valueProperty().addListener((o,a,b) -> { if(b!=null) setValid(statutCombo,statutError); else reset(statutCombo,statutError); });
        dateDebutPicker.valueProperty().addListener((o,a,b) -> {
            if(b!=null) setValid(dateDebutPicker,dateDebutError); else reset(dateDebutPicker,dateDebutError);
            if(dateFinPicker.getValue()!=null) liveDateFin();
        });
        dateFinPicker.valueProperty().addListener((o,a,b) -> liveDateFin());
    }

    public void setObjectif(ObjectifBienEtreDto o) {
        titreField.setText(ns(o.getTitre()));
        typeCombo.setValue(o.getType());
        statutCombo.setValue(o.getStatut());
        descriptionArea.setText(ns(o.getDescription()));
        valeurCibleField.setText(o.getValeurCible()==null?"":String.valueOf(o.getValeurCible()));
        valeurActuelleField.setText(o.getValeurActuelle()==null?"":String.valueOf(o.getValeurActuelle()));
        dateDebutPicker.setValue(o.getDateDebut());
        dateFinPicker.setValue(o.getDateFin());
        slugField.setText(ns(o.getSlug()));
    }

    @FXML
    private void onEnregistrer() {
        if (!validateAll()) return;

        Double cible    = Double.parseDouble(valeurCibleField.getText().trim());
        Double actuelle = valeurActuelleField.getText().isBlank() ? null
                : Double.parseDouble(valeurActuelleField.getText().trim());

        result = new ObjectifBienEtreDto();
        result.setTitre(titreField.getText().trim());
        result.setType(typeCombo.getValue());
        result.setStatut(statutCombo.getValue());
        result.setDescription(descriptionArea.getText().trim());
        result.setValeurCible(cible);
        result.setValeurActuelle(actuelle);
        result.setDateDebut(dateDebutPicker.getValue());
        result.setDateFin(dateFinPicker.getValue());
        result.setSlug(slugField.getText().isBlank()?null:slugField.getText().trim());
        result.setUtilisateurId(1);
        close();
    }

    @FXML private void onAnnuler() { result=null; close(); }
    public ObjectifBienEtreDto getResult() { return result; }

    // ── Full validate ──────────────────────────────────────────────────────────
    private boolean validateAll() {
        boolean ok = true;

        // Titre
        String t = str(titreField.getText());
        if (t.isEmpty())        { setInvalid(titreField,titreError,"Le titre est obligatoire."); ok=false; }
        else if(t.length()<3)   { setInvalid(titreField,titreError,"Le titre doit contenir au moins 3 caractères."); ok=false; }
        else if(t.length()>150) { setInvalid(titreField,titreError,"Le titre ne peut pas dépasser 150 caractères."); ok=false; }
        else setValid(titreField,titreError);

        // Type
        if(typeCombo.getValue()==null){ setInvalid(typeCombo,typeError,"Le type est obligatoire."); ok=false; }
        else setValid(typeCombo,typeError);

        // Statut
        if(statutCombo.getValue()==null){ setInvalid(statutCombo,statutError,"Le statut est obligatoire."); ok=false; }
        else setValid(statutCombo,statutError);

        // Valeur cible
        String vc = str(valeurCibleField.getText());
        if(vc.isEmpty()){ setInvalid(valeurCibleField,valeurCibleError,"La valeur cible est obligatoire."); ok=false; }
        else { try {
            double d=Double.parseDouble(vc);
            if(d<=0)   { setInvalid(valeurCibleField,valeurCibleError,"La valeur cible doit être un nombre positif."); ok=false; }
            else if(d>9999){ setInvalid(valeurCibleField,valeurCibleError,"La valeur cible ne peut pas dépasser 9999."); ok=false; }
            else setValid(valeurCibleField,valeurCibleError);
        } catch(NumberFormatException e){ setInvalid(valeurCibleField,valeurCibleError,"Nombre valide requis."); ok=false; } }

        // Valeur actuelle (optional)
        String va = str(valeurActuelleField.getText());
        if(!va.isEmpty()){ try {
            double d=Double.parseDouble(va);
            if(d<0)    { setInvalid(valeurActuelleField,valeurActuelleError,"Ne peut pas être négative."); ok=false; }
            else if(d>9999){ setInvalid(valeurActuelleField,valeurActuelleError,"Ne peut pas dépasser 9999."); ok=false; }
            else setValid(valeurActuelleField,valeurActuelleError);
        } catch(NumberFormatException e){ setInvalid(valeurActuelleField,valeurActuelleError,"Nombre valide requis."); ok=false; }
        } else reset(valeurActuelleField,valeurActuelleError);

        // Date début
        if(dateDebutPicker.getValue()==null){ setInvalid(dateDebutPicker,dateDebutError,"La date de début est obligatoire."); ok=false; }
        else setValid(dateDebutPicker,dateDebutError);

        // Date fin
        if(dateFinPicker.getValue()==null){ setInvalid(dateFinPicker,dateFinError,"La date de fin est obligatoire."); ok=false; }
        else if(dateDebutPicker.getValue()!=null && !dateFinPicker.getValue().isAfter(dateDebutPicker.getValue())){
            setInvalid(dateFinPicker,dateFinError,"Doit être postérieure à la date de début."); ok=false;
        } else setValid(dateFinPicker,dateFinError);

        return ok;
    }

    // ── Live ──────────────────────────────────────────────────────────────────
    private void liveTitre() {
        String t=str(titreField.getText());
        if(t.isEmpty())       reset(titreField,titreError);
        else if(t.length()<3)  setInvalid(titreField,titreError,"Le titre doit contenir au moins 3 caractères.");
        else if(t.length()>150)setInvalid(titreField,titreError,"Le titre ne peut pas dépasser 150 caractères.");
        else setValid(titreField,titreError);
    }
    private void liveCible() {
        String s=str(valeurCibleField.getText());
        if(s.isEmpty()){reset(valeurCibleField,valeurCibleError);return;}
        try{ double d=Double.parseDouble(s);
            if(d<=0)  setInvalid(valeurCibleField,valeurCibleError,"Doit être un nombre positif.");
            else if(d>9999)setInvalid(valeurCibleField,valeurCibleError,"Ne peut pas dépasser 9999.");
            else setValid(valeurCibleField,valeurCibleError);
        }catch(NumberFormatException e){setInvalid(valeurCibleField,valeurCibleError,"Nombre valide requis.");}
    }
    private void liveActuelle() {
        String s=str(valeurActuelleField.getText());
        if(s.isEmpty()){reset(valeurActuelleField,valeurActuelleError);return;}
        try{ double d=Double.parseDouble(s);
            if(d<0)  setInvalid(valeurActuelleField,valeurActuelleError,"Ne peut pas être négative.");
            else if(d>9999)setInvalid(valeurActuelleField,valeurActuelleError,"Ne peut pas dépasser 9999.");
            else setValid(valeurActuelleField,valeurActuelleError);
        }catch(NumberFormatException e){setInvalid(valeurActuelleField,valeurActuelleError,"Nombre valide requis.");}
    }
    private void liveDateFin() {
        if(dateFinPicker.getValue()==null){reset(dateFinPicker,dateFinError);return;}
        if(dateDebutPicker.getValue()!=null && !dateFinPicker.getValue().isAfter(dateDebutPicker.getValue()))
            setInvalid(dateFinPicker,dateFinError,"Doit être postérieure à la date de début.");
        else setValid(dateFinPicker,dateFinError);
    }

    // ── CSS helpers ───────────────────────────────────────────────────────────
    private void setValid(Control c, Label err) {
        c.getStyleClass().removeAll(VALID,INVALID); c.getStyleClass().add(VALID);
        if(err!=null) err.setText("");
    }
    private void setInvalid(Control c, Label err, String msg) {
        c.getStyleClass().removeAll(VALID,INVALID); c.getStyleClass().add(INVALID);
        if(err!=null) err.setText(msg);
    }
    private void reset(Control c, Label err) {
        c.getStyleClass().removeAll(VALID,INVALID);
        if(err!=null) err.setText("");
    }

    private String str(String s)  { return s==null?"":s.trim(); }
    private String ns(String s)   { return s==null?"":s; }
    private void close() { ((Stage)titreField.getScene().getWindow()).close(); }
}