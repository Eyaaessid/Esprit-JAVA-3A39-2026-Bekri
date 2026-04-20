package tn.esprit.profil.ui;

import tn.esprit.profil.entity.ProfilPsychologique;
import tn.esprit.profil.service.ProfilPsychologiqueService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.entity.Utilisateur;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ProfilPsychologiqueController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label scoreLabel;
    @FXML private Label profilTypeLabel;
    @FXML private Label feedbackLabel;
    @FXML private Label dateLabel;
    @FXML private Label badgeLabel;
    @FXML private VBox resultBox;
    @FXML private VBox emptyBox;
    @FXML private Button passerTestBtn;
    @FXML private Button repasserBtn;

    @FXML
    private void initialize() {
        Utilisateur user = SessionManager.getInstance().getCurrentUser();
        if (user == null || user.getId() == null) {
            showEmpty();
            return;
        }
        ProfilPsychologiqueService service = new ProfilPsychologiqueService();
        Optional<ProfilPsychologique> opt = service.getProfilForUser(user.getId());
        if (opt.isPresent()) {
            showResult(opt.get());
        } else {
            showEmpty();
        }
    }

    private void showEmpty() {
        emptyBox.setVisible(true);
        emptyBox.setManaged(true);
        resultBox.setVisible(false);
        resultBox.setManaged(false);
        if (repasserBtn != null) {
            repasserBtn.setVisible(false);
            repasserBtn.setManaged(false);
        }
        if (passerTestBtn != null) {
            passerTestBtn.setVisible(true);
            passerTestBtn.setManaged(true);
        }
    }

    private void showResult(ProfilPsychologique p) {
        emptyBox.setVisible(false);
        emptyBox.setManaged(false);
        resultBox.setVisible(true);
        resultBox.setManaged(true);
        if (passerTestBtn != null) {
            passerTestBtn.setVisible(false);
            passerTestBtn.setManaged(false);
        }
        if (repasserBtn != null) {
            repasserBtn.setVisible(true);
            repasserBtn.setManaged(true);
        }

        Integer sg = p.getScoreGlobal();
        int score = sg != null ? sg : 0;
        scoreLabel.setText("Score : " + score + " / 100");
        profilTypeLabel.setText(p.getProfilType() != null ? p.getProfilType() : "—");
        feedbackLabel.setText(p.getAiFeedback() != null ? p.getAiFeedback() : "");
        if (p.getDateEvaluation() != null) {
            dateLabel.setText(p.getDateEvaluation().format(DATE_FMT));
        } else {
            dateLabel.setText("—");
        }
        applyBadge(score);
    }

    private void applyBadge(int scoreGlobal) {
        String text;
        String bg;
        if (scoreGlobal <= 25) {
            text = "Équilibre très bon";
            bg = "#22c55e";
        } else if (scoreGlobal <= 50) {
            text = "Équilibre modéré";
            bg = "#3b82f6";
        } else if (scoreGlobal <= 75) {
            text = "Vulnérabilité moyenne";
            bg = "#f97316";
        } else {
            text = "Risque élevé";
            bg = "#ef4444";
        }
        badgeLabel.setText(text);
        badgeLabel.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-padding: 8 16; "
                + "-fx-background-radius: 20; -fx-font-weight: bold;");
    }

    @FXML
    private void handleRetour() {
        try {
            SceneManager.switchTo("user-dashboard");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }

    @FXML
    private void handleRepasser() {
        goToTest();
    }

    @FXML
    private void handlePasserTest() {
        goToTest();
    }

    private void goToTest() {
        try {
            SceneManager.switchTo("test");
        } catch (IOException e) {
            DialogHelper.showError("Navigation", e.getMessage());
        }
    }
}
