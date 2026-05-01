package tn.esprit.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.models.ParticipationEvenement;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.UtilisateurService;

import java.time.LocalDateTime;

public class ParticipationFormView {

    private ParticipationEvenement participation;
    private ParticipationService participationService;
    private UtilisateurService utilisateurService;
    private boolean isEditMode;
    private Integer prefilledEvenementId;
    private String evenementTitre;

    public ParticipationFormView(ParticipationEvenement participation) {
        this.participation = participation;
        this.participationService = new ParticipationService();
        this.utilisateurService = new UtilisateurService();
        this.isEditMode = (participation != null);
        this.prefilledEvenementId = null;
        this.evenementTitre = null;
    }

    public ParticipationFormView(ParticipationEvenement participation, Integer evenementId, String evenementTitre) {
        this.participation = participation;
        this.participationService = new ParticipationService();
        this.utilisateurService = new UtilisateurService();
        this.isEditMode = (participation != null);
        this.prefilledEvenementId = evenementId;
        this.evenementTitre = evenementTitre;
    }

    public void show(Stage stage, Runnable onSuccess) {
        stage.initModality(Modality.APPLICATION_MODAL);
        String titreFenetre = isEditMode ? "Modifier la participation" : "Participer à l'événement";
        stage.setTitle(titreFenetre);

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(25));
        formBox.setStyle("-fx-background-color: white;");

        Label lblTitle = new Label(titreFenetre);
        lblTitle.getStyleClass().add("form-title");

        if (prefilledEvenementId != null && evenementTitre != null) {
            VBox eventInfoBox = new VBox(5);
            eventInfoBox.setStyle("-fx-background-color: #e8f4f8; -fx-padding: 15px; -fx-background-radius: 5px; -fx-border-color: #3498db; -fx-border-width: 1px; -fx-border-radius: 5px;");

            Label lblEventTitle = new Label("Événement sélectionné :");
            lblEventTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

            Label lblEventName = new Label(evenementTitre);
            lblEventName.setStyle("-fx-font-size: 15px; -fx-text-fill: #3498db; -fx-font-weight: bold;");

            Label lblEventId = new Label("ID événement : " + prefilledEvenementId + " (verrouillé)");
            lblEventId.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

            eventInfoBox.getChildren().addAll(lblEventTitle, lblEventName, lblEventId);
            formBox.getChildren().add(eventInfoBox);
        }

        Label lblUtilisateur = new Label("ID utilisateur :");
        lblUtilisateur.getStyleClass().add("form-label");

        TextField txtUtilisateurId = new TextField();
        txtUtilisateurId.setPromptText("Tapez un ID utilisateur existant");
        txtUtilisateurId.setMaxWidth(Double.MAX_VALUE);
        if (isEditMode) {
            txtUtilisateurId.setText(String.valueOf(participation.getUtilisateur_id()));
        }

        Label lblEvenement = new Label("ID événement :");
        lblEvenement.getStyleClass().add("form-label");

        TextField txtEvenementId = new TextField();
        txtEvenementId.setPromptText("ID de l'événement");
        if (isEditMode) {
            txtEvenementId.setText(String.valueOf(participation.getEvenement_id()));
        } else if (prefilledEvenementId != null) {
            txtEvenementId.setText(String.valueOf(prefilledEvenementId));
            txtEvenementId.setEditable(false);
            txtEvenementId.setStyle("-fx-background-color: #ecf0f1; -fx-opacity: 1.0;");
        }

        Label lblStatut = new Label("Statut :");
        lblStatut.getStyleClass().add("form-label");

        ComboBox<String> cmbStatut = new ComboBox<>();
        cmbStatut.getItems().addAll("confirmé", "en attente", "annulé", "présent", "absent");
        cmbStatut.setPromptText("Choisir un statut");
        cmbStatut.setMaxWidth(Double.MAX_VALUE);
        if (isEditMode) {
            cmbStatut.setValue(participation.getStatut());
        } else {
            cmbStatut.setValue("confirmé");
        }

        Label lblCommentaire = new Label("Commentaire (optionnel) :");
        lblCommentaire.getStyleClass().add("form-label");

        TextArea txtCommentaire = new TextArea();
        txtCommentaire.setPromptText("Ajoutez un commentaire...");
        txtCommentaire.setPrefRowCount(3);
        if (isEditMode) {
            txtCommentaire.setText(participation.getCommentaire() != null ? participation.getCommentaire() : "");
        }

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));

        Button btnEnregistrer = new Button(isEditMode ? "Confirmer modification" : "Enregistrer ma participation");
        btnEnregistrer.getStyleClass().add("btn-save");

        btnEnregistrer.setOnAction(e -> {
            try {
                String rawUser = txtUtilisateurId.getText();
                if (rawUser == null || rawUser.trim().isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "ID utilisateur requis",
                            "Veuillez saisir un ID utilisateur.");
                    return;
                }

                int utilisateurId;
                try {
                    utilisateurId = Integer.parseInt(rawUser.trim());
                } catch (NumberFormatException nfe) {
                    showAlert(Alert.AlertType.ERROR, "ID invalide",
                            "L'ID utilisateur doit être un nombre.");
                    return;
                }

                if (!utilisateurService.existsById(utilisateurId)) {
                    showAlert(Alert.AlertType.ERROR, "Utilisateur introuvable",
                            "Utilisateur inexistant. Veuillez saisir un ID utilisateur valide.");
                    return;
                }

                String statut = cmbStatut.getValue();
                if (statut == null || statut.isBlank()) {
                    showAlert(Alert.AlertType.ERROR, "Statut requis", "Veuillez choisir un statut.");
                    return;
                }

                int evenementId;
                try {
                    evenementId = Integer.parseInt(txtEvenementId.getText().trim());
                } catch (NumberFormatException nfe) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "ID événement invalide.");
                    return;
                }

                if (isEditMode) {
                    participation.setUtilisateur_id(utilisateurId);
                    participation.setEvenement_id(evenementId);
                    participation.setStatut(statut);
                    participation.setCommentaire(txtCommentaire.getText());

                    participationService.modifier(participation);

                    stage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation modifiée avec succès !");

                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    ParticipationEvenement newParticipation = new ParticipationEvenement();
                    newParticipation.setUtilisateur_id(utilisateurId);
                    newParticipation.setEvenement_id(evenementId);
                    newParticipation.setStatut(statut);
                    newParticipation.setCommentaire(txtCommentaire.getText());
                    newParticipation.setDate_inscription(LocalDateTime.now());

                    participationService.ajouter(newParticipation);

                    stage.close();
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Participation enregistrée avec succès !");

                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }
            } catch (RuntimeException ex) {
                String msg = ex.getMessage();
                if (msg != null && (msg.toLowerCase().contains("foreign key") || msg.contains("Cannot add or update a child row"))) {
                    showAlert(Alert.AlertType.ERROR, "Enregistrement impossible",
                            "Les données ne correspondent pas à la base. Vérifiez l'ID utilisateur et l'événement.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Erreur : " + (msg != null ? msg : ex.getClass().getSimpleName()));
                }
                ex.printStackTrace();
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
                lblUtilisateur, txtUtilisateurId,
                lblEvenement, txtEvenementId,
                lblStatut, cmbStatut,
                lblCommentaire, txtCommentaire,
                buttonBox
        );

        ScrollPane scrollPane = new ScrollPane(formBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(scrollPane, 480, 580);
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
