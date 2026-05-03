package tn.esprit.evenement.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.evenement.entity.Evenement;
import tn.esprit.evenement.entity.ParticipationEvenement;
import tn.esprit.evenement.service.EvenementService;
import tn.esprit.evenement.service.FavoriService;
import tn.esprit.evenement.service.ParticipationService;
import tn.esprit.session.SessionManager;
import tn.esprit.shared.SceneData;
import tn.esprit.user.shell.UserShellNavigator;
import tn.esprit.user.shell.UserShellRoute;
import tn.esprit.user.entity.Utilisateur;
import tn.esprit.user.enums.UtilisateurRole;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class EvenementsListController implements Initializable {
    @FXML private FlowPane cardsContainer;
    @FXML private VBox emptyState;
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> filterType;
    @FXML private TextField searchField;
    @FXML private Button btnCoachEvenements;
    @FXML private Button btnCoachDashboard;

    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private final FavoriService favoriService = new FavoriService();
    private Utilisateur currentUser;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        boolean coach = currentUser != null && currentUser.getRole() == UtilisateurRole.COACH;
        if (btnCoachEvenements != null) {
            btnCoachEvenements.setVisible(coach);
            btnCoachEvenements.setManaged(coach);
        }
        if (btnCoachDashboard != null) {
            btnCoachDashboard.setVisible(coach);
            btnCoachDashboard.setManaged(coach);
        }

        filterStatut.getItems().setAll("Tous", "OPEN", "CLOSED", "PLANIFIE", "ANNULE", "COMPLET");
        filterType.getItems().setAll("Tous", "EVENT", "SESSION");
        filterStatut.getSelectionModel().selectFirst();
        filterType.getSelectionModel().selectFirst();

        filterStatut.valueProperty().addListener((obs, oldV, newV) -> refreshCards());
        filterType.valueProperty().addListener((obs, oldV, newV) -> refreshCards());
        searchField.textProperty().addListener((obs, oldV, newV) -> refreshCards());

        refreshCards();
    }

    @FXML private void handleRefresh(ActionEvent e) { refreshCards(); }
    @FXML private void handleAccueil(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.DASHBOARD); }
    @FXML private void handleLogout(ActionEvent e) { UserShellNavigator.logout(); }
    @FXML private void handleEvenements(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_LIST); }
    @FXML private void handleMesParticipations(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_PARTICIPATIONS); }
    @FXML private void handleMesFavoris(ActionEvent e) { UserShellNavigator.navigate(UserShellRoute.EVENTS_FAVORIS); }
    @FXML private void handleCoachEvenements(ActionEvent e) throws Exception { UserShellNavigator.leaveShellToScene("coach-evenements"); }
    @FXML private void handleCoachDashboard(ActionEvent e) throws Exception { UserShellNavigator.leaveShellToScene("coach-dashboard"); }

    private void refreshCards() {
        List<Evenement> evenements = evenementService.afficherAll();
        cardsContainer.getChildren().clear();
        String fs = filterStatut.getValue() == null ? "Tous" : filterStatut.getValue();
        String ft = filterType.getValue() == null ? "Tous" : filterType.getValue();
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        for (Evenement ev : evenements) {
            if (!"Tous".equalsIgnoreCase(fs) && !safe(ev.getStatut()).equalsIgnoreCase(fs)) continue;
            if (!"Tous".equalsIgnoreCase(ft) && !safe(ev.getType()).equalsIgnoreCase(ft)) continue;
            if (!q.isEmpty() && !safe(ev.getTitre()).toLowerCase(Locale.ROOT).contains(q)) continue;
            cardsContainer.getChildren().add(buildCard(ev));
        }
        boolean empty = cardsContainer.getChildren().isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
    }

    private VBox buildCard(Evenement e) {
        VBox card = new VBox();
        card.getStyleClass().addAll("card", "obj-card");
        card.setPrefWidth(320);

        VBox top = new VBox(8);
        top.getStyleClass().add("obj-card-top");

        HBox tags = new HBox(8);
        Label type = new Label(safe(e.getType()));
        type.getStyleClass().addAll("chip", "chip-type");
        Label statut = new Label(safe(e.getStatut()));
        statut.getStyleClass().addAll("badge", badgeClassForStatut(e.getStatut()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        tags.getChildren().addAll(type, spacer, statut);

        Label titre = new Label(safe(e.getTitre()));
        titre.getStyleClass().add("page-title");
        titre.setStyle("-fx-font-size:16px;");
        titre.setWrapText(true);
        Label lien = new Label(e.getLien_session() != null && !e.getLien_session().isEmpty()
                ? "🔗 Session en ligne" : "📍 Présentiel");
        lien.getStyleClass().add("muted");
        Label date = new Label("📅 " + fmt(e.getDate_debut()));
        date.getStyleClass().add("muted");
        Label capa = new Label("👥 " + participationService.afficherAllEnriched(currentUser.getId()).stream().filter(p -> p.getEvenement().getId() == e.getId()).count() + " / " + e.getCapacite_max() + " places");
        capa.getStyleClass().add("muted");
        top.getChildren().addAll(tags, titre, lien, date, capa);

        HBox actions = new HBox(10);
        actions.getStyleClass().add("obj-card-actions");
        actions.setAlignment(Pos.CENTER_LEFT);
        Button btnDetails = new Button("Détails");
        btnDetails.getStyleClass().add("btn-ghost");
        btnDetails.setOnAction(evt -> {
            SceneData.getInstance().set(e);
            UserShellNavigator.navigate(UserShellRoute.EVENTS_DETAIL);
        });

        Button btnParticiper = new Button("Participer");
        btnParticiper.getStyleClass().add("btn-primary");
        boolean already = participationService.estDejaInscrit(currentUser.getId(), e.getId());
        btnParticiper.setVisible(!already);
        btnParticiper.setManaged(!already);
        btnParticiper.setOnAction(evt -> {
            ParticipationEvenement p = new ParticipationEvenement();
            p.setEvenement_id(e.getId());
            p.setUtilisateur_id(currentUser.getId());
            p.setDate_inscription(LocalDateTime.now());
            p.setStatut("INSCRIT");
            participationService.ajouter(p);
            refreshCards();
        });

        boolean inFav = favoriService.estEnFavori(currentUser.getId(), e.getId());
        Button btnFav = new Button(inFav ? "❤" : "🤍");
        btnFav.getStyleClass().add("btn-icon");
        if (inFav) btnFav.getStyleClass().add("btn-favori-active");
        btnFav.setOnAction(evt -> {
            boolean fav = favoriService.estEnFavori(currentUser.getId(), e.getId());
            if (fav) favoriService.retirerFavori(currentUser.getId(), e.getId());
            else favoriService.ajouterFavori(currentUser.getId(), e.getId());
            refreshCards();
        });

        actions.getChildren().addAll(btnDetails, btnParticiper, btnFav);
        card.getChildren().addAll(top, actions);
        return card;
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String fmt(LocalDateTime d) { return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")); }
    private String badgeClassForStatut(String statut) {
        String s = safe(statut).toLowerCase(Locale.ROOT);
        return switch (s) {
            case "open" -> "badge-ouvert";
            case "closed" -> "badge-ferme";
            case "planifie" -> "badge-planifie";
            case "complet" -> "badge-complet";
            case "annule" -> "badge-annule";
            default -> "badge-info";
        };
    }
}
