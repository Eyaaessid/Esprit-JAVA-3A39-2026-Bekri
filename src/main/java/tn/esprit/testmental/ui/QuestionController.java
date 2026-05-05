package tn.esprit.testmental.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.testmental.dao.QuestionDAO;
import tn.esprit.testmental.dao.TestMentalDAO;
import tn.esprit.testmental.model.Question;
import tn.esprit.testmental.model.TestMental;

import java.io.IOException;
import java.util.List;

public class QuestionController {

    @FXML private TableView<Question> tableQuestions;
    @FXML private TableColumn<Question, Integer> colId;
    @FXML private TableColumn<Question, String> colContenu;
    @FXML private TableColumn<Question, String> colChoixA;
    @FXML private TableColumn<Question, String> colChoixB;
    @FXML private TableColumn<Question, String> colChoixC;
    @FXML private TableColumn<Question, String> colBonneReponse;
    @FXML private TableColumn<Question, Integer> colTestMental;

    @FXML private TextField txtContenu;
    @FXML private TextField txtChoixA;
    @FXML private TextField txtChoixB;
    @FXML private TextField txtChoixC;

    @FXML private ComboBox<String> comboBonneReponse;
    @FXML private ComboBox<TestMental> comboTestMental;

    private final QuestionDAO dao = new QuestionDAO();
    private final TestMentalDAO testMentalDAO = new TestMentalDAO();
    private ObservableList<Question> questionList;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colContenu.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        colChoixA.setCellValueFactory(new PropertyValueFactory<>("choixA"));
        colChoixB.setCellValueFactory(new PropertyValueFactory<>("choixB"));
        colChoixC.setCellValueFactory(new PropertyValueFactory<>("choixC"));
        colBonneReponse.setCellValueFactory(new PropertyValueFactory<>("bonneReponse"));
        colTestMental.setCellValueFactory(new PropertyValueFactory<>("testMentalId"));

        comboBonneReponse.setItems(FXCollections.observableArrayList("A", "B", "C"));
        chargerTestsMentaux();
        afficherQuestions();

        tableQuestions.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) remplirChamps(newSelection);
                }
        );
    }

    private void chargerTestsMentaux() {
        List<TestMental> tests = testMentalDAO.afficher();
        comboTestMental.setItems(FXCollections.observableArrayList(tests));
    }

    private void afficherQuestions() {
        questionList = FXCollections.observableArrayList(dao.afficher());
        tableQuestions.setItems(questionList);
    }

    private void remplirChamps(Question q) {
        txtContenu.setText(q.getContenu());
        txtChoixA.setText(q.getChoixA());
        txtChoixB.setText(q.getChoixB());
        txtChoixC.setText(q.getChoixC());
        comboBonneReponse.setValue(q.getBonneReponse());
        for (TestMental t : comboTestMental.getItems()) {
            if (t.getId() == q.getTestMentalId()) {
                comboTestMental.setValue(t);
                break;
            }
        }
    }

    @FXML
    private void ajouter() {
        if (!validerChamps()) return;
        TestMental test = comboTestMental.getValue();
        Question q = new Question(
                txtContenu.getText(), txtChoixA.getText(),
                txtChoixB.getText(), txtChoixC.getText(),
                comboBonneReponse.getValue(), test.getId()
        );
        dao.ajouter(q);
        afficherQuestions();
        viderChamps();
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Question ajoutée avec succès !");
    }

    @FXML
    private void modifier() {
        Question selected = tableQuestions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une question.");
            return;
        }
        if (!validerChamps()) return;
        selected.setContenu(txtContenu.getText());
        selected.setChoixA(txtChoixA.getText());
        selected.setChoixB(txtChoixB.getText());
        selected.setChoixC(txtChoixC.getText());
        selected.setBonneReponse(comboBonneReponse.getValue());
        selected.setTestMentalId(comboTestMental.getValue().getId());
        dao.modifier(selected);
        afficherQuestions();
        viderChamps();
        showAlert(Alert.AlertType.INFORMATION, "Succès", "Question modifiée avec succès !");
    }

    @FXML
    private void supprimer() {
        Question selected = tableQuestions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner une question.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment supprimer cette question ?");
        if (confirmation.showAndWait().get() == ButtonType.OK) {
            dao.supprimer(selected.getId());
            afficherQuestions();
            viderChamps();
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Question supprimée avec succès !");
        }
    }

    @FXML
    private void actualiser() { afficherQuestions(); }

    private void viderChamps() {
        txtContenu.clear(); txtChoixA.clear();
        txtChoixB.clear(); txtChoixC.clear();
        comboBonneReponse.getSelectionModel().clearSelection();
        comboTestMental.getSelectionModel().clearSelection();
        tableQuestions.getSelectionModel().clearSelection();
    }

    private boolean validerChamps() {
        if (txtContenu.getText().isEmpty() || txtChoixA.getText().isEmpty() ||
                txtChoixB.getText().isEmpty() || txtChoixC.getText().isEmpty() ||
                comboBonneReponse.getValue() == null || comboTestMental.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void ouvrirTestsMentaux() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/tn/esprit/testmental/test_mental.fxml")
            );
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) tableQuestions.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gestion des Tests Mentaux");
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'interface des tests mentaux.");
        }
    }
}