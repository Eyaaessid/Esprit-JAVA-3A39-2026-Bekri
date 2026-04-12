package tn.esprit.pijavafx.ui;

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
        setContent("/ui/objectifs_view.fxml");
    }

    @FXML
    public void goQuestions() {
        setActive(btnQuestions, btnObjectifs);
        setContent("/ui/questions_view.fxml");
    }

    private void setActive(Button active, Button inactive) {
        active.getStyleClass().removeAll("nav-item", "nav-item-active");
        active.getStyleClass().addAll("nav-item", "nav-item-active");

        inactive.getStyleClass().removeAll("nav-item", "nav-item-active");
        inactive.getStyleClass().add("nav-item");
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