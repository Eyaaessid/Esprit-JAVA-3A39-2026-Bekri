package tn.esprit.objectif.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

public class MainLayoutController {

    @FXML private StackPane contentHost;
    @FXML private Button    btnObjectifs;
    @FXML private Button    btnQuestions;

    @FXML
    public void initialize() {
        goObjectifs();
    }

    @FXML
    public void goObjectifs() {
        setActive(btnObjectifs, btnQuestions);
        setContent("/fxml/objectifs.fxml");
    }

    @FXML
    public void goQuestions() {
        setActive(btnQuestions, btnObjectifs);
        setContent("/fxml/questions.fxml");
    }

    private void setActive(Button active, Button inactive) {
        active.getStyleClass().removeAll("nav-item", "nav-item-active");
        active.getStyleClass().addAll("nav-item", "nav-item-active");

        inactive.getStyleClass().removeAll("nav-item", "nav-item-active");
        inactive.getStyleClass().addAll("nav-item");
    }

    private void setContent(String fxml) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(fxml));
            contentHost.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
