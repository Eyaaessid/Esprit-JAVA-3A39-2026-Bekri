package tn.esprit.views;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainView {

    private final BorderPane root;
    private final StackPane content;

    private final EvenementView evenementView;
    private final ParticipationView participationView;
    private final DashboardView dashboardView;
    private final CalendrierView calendrierView;
    private final FavorisView favorisView;

    public MainView() {
        evenementView = new EvenementView();
        participationView = new ParticipationView();
        dashboardView = new DashboardView();
        calendrierView = new CalendrierView();
        favorisView = new FavorisView();

        evenementView.setOnEventsChanged(() -> {
            calendrierView.refreshData();
            dashboardView.refreshStats();
            favorisView.refresh();
        });

        participationView.setOnVoirEvenements(() -> showPage("evenements"));

        root = new BorderPane();
        root.getStyleClass().add("app-root");

        content = new StackPane();
        content.getStyleClass().add("app-content");

        root.setTop(createNavbar());
        root.setCenter(content);

        showPage("evenements");
    }

    private HBox createNavbar() {
        HBox navbar = new HBox(10);
        navbar.getStyleClass().add("navbar");

        Label brand = new Label("BEKRI");
        brand.getStyleClass().add("navbar-brand");
        brand.setOnMouseClicked(e -> showPage("evenements"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleGroup group = new ToggleGroup();

        ToggleButton btnEvenements = createNavButton("Événements", "evenements", group);
        ToggleButton btnParticipations = createNavButton("Participations", "participations", group);
        ToggleButton btnCalendrier = createNavButton("Calendrier", "calendrier", group);
        ToggleButton btnDashboard = createNavButton("Dashboard", "dashboard", group);
        ToggleButton btnFavoris = createNavButton("Favoris", "favoris", group);

        navbar.getChildren().addAll(
                brand,
                spacer,
                btnEvenements,
                btnParticipations,
                btnCalendrier,
                btnDashboard,
                btnFavoris
        );
        return navbar;
    }

    private ToggleButton createNavButton(String label, String pageKey, ToggleGroup group) {
        ToggleButton button = new ToggleButton(label);
        button.getStyleClass().add("nav-item");
        button.setToggleGroup(group);
        button.setUserData(pageKey);
        button.setOnAction(e -> showPage(pageKey));
        return button;
    }

    private void showPage(String key) {
        if (key == null) {
            return;
        }

        switch (key) {
            case "evenements":
                content.getChildren().setAll(evenementView.getView());
                break;
            case "participations":
                participationView.refresh();
                content.getChildren().setAll(participationView.getView());
                break;
            case "dashboard":
                dashboardView.refreshStats();
                content.getChildren().setAll(dashboardView.getView());
                break;
            case "calendrier":
                calendrierView.refreshData();
                content.getChildren().setAll(calendrierView.getView());
                break;
            case "favoris":
                favorisView.refresh();
                content.getChildren().setAll(favorisView.getView());
                break;
            default:
                content.getChildren().setAll(evenementView.getView());
        }

        // Update navbar active state.
        if (root.getTop() instanceof HBox bar) {
            bar.getChildren().stream()
                    .filter(n -> n instanceof ToggleButton)
                    .map(n -> (ToggleButton) n)
                    .forEach(tb -> tb.setSelected(key.equals(tb.getUserData())));
        }
    }

    public void show(Stage stage) {
        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        stage.setTitle("BEKRI");
        stage.setScene(scene);
        stage.show();
    }
}
