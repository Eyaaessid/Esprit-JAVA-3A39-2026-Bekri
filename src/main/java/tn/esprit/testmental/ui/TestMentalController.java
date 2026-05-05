package tn.esprit.testmental.ui;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.testmental.dao.TestMentalDAO;
import tn.esprit.testmental.model.TestMental;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TestMentalController {

    @FXML private TableView<TestMental> tableTests;
    @FXML private TableColumn<TestMental, Integer> colId;
    @FXML private TableColumn<TestMental, String> colTitre;
    @FXML private TableColumn<TestMental, String> colDescription;
    @FXML private TableColumn<TestMental, String> colNiveau;
    @FXML private TableColumn<TestMental, Integer> colDuree;
    @FXML private TableColumn<TestMental, String> colType;

    @FXML private TextField txtTitre;
    @FXML private TextField txtDescription;
    @FXML private TextField txtNiveau;
    @FXML private TextField txtDuree;
    @FXML private TextField txtType;
    @FXML private TextField txtRecherche;

    @FXML private PieChart pieChart;

    private final TestMentalDAO dao = new TestMentalDAO();
    private ObservableList<TestMental> listeTests;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colNiveau.setCellValueFactory(new PropertyValueFactory<>("niveau"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeTest"));
        afficherTests();
        configurerRecherche();
        configurerSelection();
        afficherStatistiques();
    }

//    }
    private void afficherTests() {
        listeTests = FXCollections.observableArrayList(dao.afficher());
        tableTests.setItems(listeTests);
        afficherStatistiques();
    }

    private void configurerSelection() {
        tableTests.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        txtTitre.setText(newSelection.getTitre());
                        txtDescription.setText(newSelection.getDescription());
                        txtNiveau.setText(newSelection.getNiveau());
                        txtDuree.setText(String.valueOf(newSelection.getDuree()));
                        txtType.setText(newSelection.getTypeTest());
                    }
                }
        );
    }

    private boolean validerSaisie() {
        if (txtTitre.getText().isEmpty() || txtDescription.getText().isEmpty() ||
                txtNiveau.getText().isEmpty() || txtDuree.getText().isEmpty() ||
                txtType.getText().isEmpty()) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de saisie", "Tous les champs doivent être remplis !");
            return false;
        }
        try {
            Integer.parseInt(txtDuree.getText());
        } catch (NumberFormatException e) {
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de saisie", "La durée doit être un nombre !");
            return false;
        }
        return true;
    }
    @FXML
    private void goAccueil(ActionEvent event) {
        try {
            // on récupère la fenêtre actuelle
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // on recharge le dashboard COMPLET (car tu n'as pas accès au controller)
            FXMLLoader loader = new FXMLLoader(
                    getClass().getClassLoader()
                            .getResource("fxml/community/user-dashboard.fxml")
            );

            Parent root = loader.load();

            stage.getScene().setRoot(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void ajouter() {
        if (!validerSaisie()) return;
        TestMental test = new TestMental(
                txtTitre.getText(), txtDescription.getText(),
                txtNiveau.getText(), Integer.parseInt(txtDuree.getText()), txtType.getText()
        );
        dao.ajouter(test);
        afficherTests();
        viderChamps();
        afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Test ajouté avec succès !");
    }

    @FXML
    private void modifier() {
        TestMental selected = tableTests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucune sélection", "Veuillez sélectionner un test à modifier.");
            return;
        }
        if (!validerSaisie()) return;
        selected.setTitre(txtTitre.getText());
        selected.setDescription(txtDescription.getText());
        selected.setNiveau(txtNiveau.getText());
        selected.setDuree(Integer.parseInt(txtDuree.getText()));
        selected.setTypeTest(txtType.getText());
        dao.modifier(selected);
        afficherTests();
        viderChamps();
        afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "Test modifié avec succès !");
    }


    @FXML
    private void supprimer() {
        TestMental selected = tableTests.getSelectionModel().getSelectedItem();
        if (selected == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucune sélection", "Veuillez sélectionner un test à supprimer.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment supprimer ce test ?");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                dao.supprimer(selected.getId());
                afficherTests();
                viderChamps();
            }
        });
    }
    @FXML
    private void ouvrirPassageTest() {
        try {
            TestMental selected = tableTests.getSelectionModel().getSelectedItem();

            if (selected == null) {
                afficherAlerte(Alert.AlertType.WARNING, "Aucun test sélectionné",
                        "Veuillez sélectionner un test avant de commencer.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/community/passer_test.fxml") // ✅ CHANGE HERE
            );

            Scene scene = new Scene(loader.load());

            PasserTestController controller = loader.getController(); // now OK
            controller.initTest(selected);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Passage du Test");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void retourDashboard() {
        Stage stage = (Stage) tableTests.getScene().getWindow();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/user-dashboard.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private BorderPane rootPane;

    public void loadPage(String fxml) {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getClassLoader()
                            .getResource("fxml/community/" + fxml)
            );
            rootPane.setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void actualiser() { afficherTests(); }

    private void viderChamps() {
        txtTitre.clear(); txtDescription.clear(); txtNiveau.clear();
        txtDuree.clear(); txtType.clear();
        tableTests.getSelectionModel().clearSelection();
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void configurerRecherche() {
        if (txtRecherche != null) {
            txtRecherche.textProperty().addListener((obs, oldValue, newValue) -> filtrerTests(newValue));
        }
    }

    private void filtrerTests(String motCle) {
        ObservableList<TestMental> listeFiltree = FXCollections.observableArrayList();
        for (TestMental test : listeTests) {
            if (test.getTitre().toLowerCase().contains(motCle.toLowerCase()) ||
                    test.getNiveau().toLowerCase().contains(motCle.toLowerCase()) ||
                    test.getTypeTest().toLowerCase().contains(motCle.toLowerCase())) {
                listeFiltree.add(test);
            }
        }
        tableTests.setItems(listeFiltree);
        afficherStatistiques();
    }

    private void afficherStatistiques() {
        if (pieChart == null) return;
        int facile = 0, moyen = 0, difficile = 0;
        for (TestMental test : dao.afficher()) {
            switch (test.getNiveau().toLowerCase()) {
                case "facile": facile++; break;
                case "moyen": moyen++; break;
                case "difficile": difficile++; break;
            }
        }
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                new PieChart.Data("Facile", facile),
                new PieChart.Data("Moyen", moyen),
                new PieChart.Data("Difficile", difficile)
        );
        pieChart.setData(data);
    }

    @FXML
    private void exporterPDF() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        fileChooser.setInitialFileName("tests_mentaux.pdf");
        File file = fileChooser.showSaveDialog((Stage) tableTests.getScene().getWindow());
        if (file == null) return;
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            document.add(new Paragraph("Liste des Tests Mentaux\n\n"));
            for (TestMental test : dao.afficher()) {
                document.add(new Paragraph(
                        "Titre: " + test.getTitre() + " | Niveau: " + test.getNiveau() +
                                " | Durée: " + test.getDuree() + " min | Type: " + test.getTypeTest()
                ));
            }
            document.close();
            afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "PDF généré avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la génération du PDF.");
        }
    }
}