package tn.esprit.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.models.ParticipationEvenement;

public class ParticipationDetailsView {
    
    private ParticipationEvenement participation;
    
    public ParticipationDetailsView(ParticipationEvenement participation) {
        this.participation = participation;
    }
    
    public void show(Stage stage) {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Détails de la participation");
        
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        
        Label lblTitle = new Label("Détails de la participation");
        lblTitle.getStyleClass().add("form-title");
        
        GridPane grid = new GridPane();
        grid.getStyleClass().add("details-grid");
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        
        int row = 0;
        
        addDetailRow(grid, row++, "ID:", String.valueOf(participation.getId()));
        addDetailRow(grid, row++, "Utilisateur ID:", String.valueOf(participation.getUtilisateur_id()));
        addDetailRow(grid, row++, "Événement ID:", String.valueOf(participation.getEvenement_id()));
        addDetailRow(grid, row++, "Statut:", participation.getStatut());
        addDetailRow(grid, row++, "Commentaire:", participation.getCommentaire());
        addDetailRow(grid, row++, "Date Inscription:", participation.getDate_inscription() != null ? participation.getDate_inscription().toString() : "");
        
        Button btnFermer = new Button("Fermer");
        btnFermer.getStyleClass().add("btn-close");
        btnFermer.setOnAction(e -> stage.close());
        
        HBox buttonBox = new HBox(btnFermer);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        
        mainBox.getChildren().addAll(lblTitle, grid, buttonBox);
        
        Scene scene = new Scene(mainBox, 450, 350);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }
    
    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label lblLabel = new Label(label);
        lblLabel.getStyleClass().add("detail-label");
        
        Label lblValue = new Label(value != null ? value : "");
        lblValue.getStyleClass().add("detail-value");
        lblValue.setWrapText(true);
        lblValue.setMaxWidth(250);
        
        grid.add(lblLabel, 0, row);
        grid.add(lblValue, 1, row);
    }
}
