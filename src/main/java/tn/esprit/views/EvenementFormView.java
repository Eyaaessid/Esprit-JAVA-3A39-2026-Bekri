package tn.esprit.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.models.Evenement;
import tn.esprit.services.EvenementService;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class EvenementFormView {
    
    private Evenement evenement;
    private EvenementService evenementService;
    private boolean isEditMode;
    
    public EvenementFormView(Evenement evenement) {
        this.evenement = evenement;
        this.evenementService = new EvenementService();
        this.isEditMode = (evenement != null);
    }
    
    public void show(Stage stage, Runnable onSuccess) {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(isEditMode ? "Modifier l'événement" : "Créer un événement");
        
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(20));
        
        Label lblTitle = new Label(isEditMode ? "Modifier l'événement" : "Créer un événement");
        lblTitle.getStyleClass().add("form-title");
        
        // Champs du formulaire
        TextField txtTitre = new TextField();
        txtTitre.setPromptText("Titre");
        if (isEditMode) txtTitre.setText(evenement.getTitre());
        
        TextArea txtDescription = new TextArea();
        txtDescription.setPromptText("Description");
        txtDescription.setPrefRowCount(3);
        if (isEditMode) txtDescription.setText(evenement.getDescription());
        
        // Date Début
        DatePicker dpDateDebut = new DatePicker();
        if (isEditMode && evenement.getDate_debut() != null) {
            dpDateDebut.setValue(evenement.getDate_debut().toLocalDate());
        }
        
        Spinner<Integer> spinnerHeureDebut = new Spinner<>(0, 23, 
            isEditMode && evenement.getDate_debut() != null ? evenement.getDate_debut().getHour() : 9);
        spinnerHeureDebut.setPrefWidth(70);
        spinnerHeureDebut.setEditable(true);
        
        Spinner<Integer> spinnerMinDebut = new Spinner<>(0, 59, 
            isEditMode && evenement.getDate_debut() != null ? evenement.getDate_debut().getMinute() : 0);
        spinnerMinDebut.setPrefWidth(70);
        spinnerMinDebut.setEditable(true);
        
        HBox dateDebutBox = new HBox(10);
        dateDebutBox.getChildren().addAll(dpDateDebut, new Label("H:"), spinnerHeureDebut, new Label("M:"), spinnerMinDebut);
        
        // Date Fin
        DatePicker dpDateFin = new DatePicker();
        if (isEditMode && evenement.getDate_fin() != null) {
            dpDateFin.setValue(evenement.getDate_fin().toLocalDate());
        }
        
        Spinner<Integer> spinnerHeureFin = new Spinner<>(0, 23, 
            isEditMode && evenement.getDate_fin() != null ? evenement.getDate_fin().getHour() : 17);
        spinnerHeureFin.setPrefWidth(70);
        spinnerHeureFin.setEditable(true);
        
        Spinner<Integer> spinnerMinFin = new Spinner<>(0, 59, 
            isEditMode && evenement.getDate_fin() != null ? evenement.getDate_fin().getMinute() : 0);
        spinnerMinFin.setPrefWidth(70);
        spinnerMinFin.setEditable(true);
        
        HBox dateFinBox = new HBox(10);
        dateFinBox.getChildren().addAll(dpDateFin, new Label("H:"), spinnerHeureFin, new Label("M:"), spinnerMinFin);
        
        TextField txtLieu = new TextField();
        txtLieu.setPromptText("Lieu");
        if (isEditMode) txtLieu.setText(evenement.getLieu());
        
        TextField txtCapaciteMax = new TextField();
        txtCapaciteMax.setPromptText("Capacité Max");
        if (isEditMode) txtCapaciteMax.setText(String.valueOf(evenement.getCapacite_max()));
        
        ComboBox<String> cmbType = new ComboBox<>();
        cmbType.getItems().addAll("atelier", "méditation", "défi santé", "conférence", "yoga", "nutrition");
        cmbType.setEditable(true);
        cmbType.setPromptText("Type");
        cmbType.setMaxWidth(Double.MAX_VALUE);
        if (isEditMode) cmbType.setValue(evenement.getType());
        
        ComboBox<String> cmbStatut = new ComboBox<>();
        cmbStatut.getItems().addAll("ouvert", "fermé", "planifié", "annulé", "complet");
        cmbStatut.setEditable(true);
        cmbStatut.setPromptText("Statut");
        cmbStatut.setMaxWidth(Double.MAX_VALUE);
        if (isEditMode) cmbStatut.setValue(evenement.getStatut());
        
        // Boutons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        
        Button btnEnregistrer = new Button(isEditMode ? "Confirmer modification" : "Enregistrer");
        btnEnregistrer.getStyleClass().add("btn-save");
        btnEnregistrer.setOnAction(e -> {
            try {
                LocalDateTime dateDebut = LocalDateTime.of(
                    dpDateDebut.getValue(),
                    LocalTime.of(spinnerHeureDebut.getValue(), spinnerMinDebut.getValue())
                );
                
                LocalDateTime dateFin = LocalDateTime.of(
                    dpDateFin.getValue(),
                    LocalTime.of(spinnerHeureFin.getValue(), spinnerMinFin.getValue())
                );
                
                String type = cmbType.getValue() != null ? cmbType.getValue() : cmbType.getEditor().getText();
                String statut = cmbStatut.getValue() != null ? cmbStatut.getValue() : cmbStatut.getEditor().getText();
                
                if (isEditMode) {
                    evenement.setTitre(txtTitre.getText());
                    evenement.setDescription(txtDescription.getText());
                    evenement.setDate_debut(dateDebut);
                    evenement.setDate_fin(dateFin);
                    evenement.setLieu(txtLieu.getText());
                    evenement.setCapacite_max(Integer.parseInt(txtCapaciteMax.getText()));
                    evenement.setType(type);
                    evenement.setStatut(statut);
                    
                    evenementService.modifier(evenement);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement modifié avec succès !");
                } else {
                    Evenement newEvenement = new Evenement();
                    newEvenement.setTitre(txtTitre.getText());
                    newEvenement.setDescription(txtDescription.getText());
                    newEvenement.setDate_debut(dateDebut);
                    newEvenement.setDate_fin(dateFin);
                    newEvenement.setLieu(txtLieu.getText());
                    newEvenement.setCapacite_max(Integer.parseInt(txtCapaciteMax.getText()));
                    newEvenement.setType(type);
                    newEvenement.setStatut(statut);
                    newEvenement.setImage("");
                    newEvenement.setCreated_at(LocalDateTime.now());
                    newEvenement.setCoach_id(1);
                    
                    evenementService.ajouter(newEvenement);
                    System.out.println("✓ Événement ajouté, ID généré: " + newEvenement.getId());
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Événement créé avec succès !");
                }
                
                stage.close();
                // Appeler le callback APRÈS la fermeture de la fenêtre
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur : " + ex.getMessage());
                ex.printStackTrace();
            }
        });
        
        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.getStyleClass().add("btn-cancel");
        btnAnnuler.setOnAction(e -> stage.close());
        
        buttonBox.getChildren().addAll(btnEnregistrer, btnAnnuler);
        
        formBox.getChildren().addAll(
            lblTitle,
            new Label("Titre:"), txtTitre,
            new Label("Description:"), txtDescription,
            new Label("Date Début:"), dateDebutBox,
            new Label("Date Fin:"), dateFinBox,
            new Label("Lieu:"), txtLieu,
            new Label("Capacité Max:"), txtCapaciteMax,
            new Label("Type:"), cmbType,
            new Label("Statut:"), cmbStatut,
            buttonBox
        );
        
        ScrollPane scrollPane = new ScrollPane(formBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        
        Scene scene = new Scene(scrollPane, 450, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }
    
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
