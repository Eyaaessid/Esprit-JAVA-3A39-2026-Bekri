package tn.esprit.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.models.Evenement;
import tn.esprit.services.EvenementService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendrierView {
    private static String lastSelectedStatut = "Tous";

    private final BorderPane mainLayout;
    private final EvenementService evenementService;
    private final GridPane calendarGrid;
    private final Label monthLabel;
    private final ListView<Evenement> eventsListView;
    private final Label selectedDateLabel;
    private final ComboBox<String> statutFilter;

    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private Map<LocalDate, List<Evenement>> eventsByDate;

    public CalendrierView() {
        this.evenementService = new EvenementService();
        this.mainLayout = new BorderPane();
        this.calendarGrid = new GridPane();
        this.monthLabel = new Label();
        this.eventsListView = new ListView<>();
        this.selectedDateLabel = new Label("Selectionnez une date");
        this.statutFilter = new ComboBox<>();
        this.currentMonth = YearMonth.now();
        this.selectedDate = null;
        this.eventsByDate = new HashMap<>();

        mainLayout.setCenter(createContent());

        eventsListView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Evenement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String heure = item.getDate_debut() != null
                            ? item.getDate_debut().format(DateTimeFormatter.ofPattern("HH:mm"))
                            : "--:--";
                    setText(heure + " - " + item.getTitre() + " (" + item.getLieu() + ")");
                }
            }
        });
        eventsListView.setPlaceholder(createDayEventsPlaceholder("Aucun evenement pour la date selectionnee."));

        eventsListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Evenement selected = eventsListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    EvenementDetailsView detailsView = new EvenementDetailsView(selected);
                    detailsView.show(new Stage());
                }
            }
        });

        refreshMonth();
    }

    private Node createContent() {
        VBox page = new VBox(14);
        page.setMaxWidth(1400);

        HBox header = createHeader();

        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel");

        HBox nav = createNavRow();
        HBox contentRow = createCalendarRow();

        panel.getChildren().addAll(nav, contentRow);
        page.getChildren().addAll(header, panel);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        HBox center = new HBox(scrollPane);
        center.setAlignment(Pos.TOP_CENTER);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        return center;
    }

    private HBox createHeader() {
        HBox header = new HBox(12);
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titles = new VBox(3);
        Label title = new Label("Calendrier");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue mensuelle et événements du jour.");
        subtitle.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(titles);
        return header;
    }

    private HBox createNavRow() {
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER_LEFT);

        Button prev = new Button("Mois précédent");
        prev.getStyleClass().addAll("btn-outline");
        prev.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            selectedDate = null;
            refreshMonth();
        });

        Button today = new Button("Aujourd'hui");
        today.getStyleClass().addAll("btn-primary");
        today.setOnAction(e -> {
            currentMonth = YearMonth.now();
            selectedDate = LocalDate.now();
            refreshMonth();
        });

        Button next = new Button("Mois suivant");
        next.getStyleClass().addAll("btn-outline");
        next.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            selectedDate = null;
            refreshMonth();
        });

        Button refresh = new Button("Actualiser");
        refresh.getStyleClass().addAll("btn-outline");
        refresh.setOnAction(e -> refreshData());

        statutFilter.getItems().setAll("Tous", "Ouvert", "Ferme", "Planifie");
        statutFilter.setValue(lastSelectedStatut);
        statutFilter.getStyleClass().add("filter-combo");
        statutFilter.setPrefWidth(190);
        statutFilter.setOnAction(e -> {
            lastSelectedStatut = statutFilter.getValue();
            refreshMonth();
        });

        monthLabel.getStyleClass().add("month-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        nav.getChildren().addAll(prev, today, next, refresh, statutFilter, spacer, monthLabel);
        return nav;
    }

    private HBox createCalendarRow() {
        HBox content = new HBox(16);
        content.setAlignment(Pos.TOP_LEFT);

        VBox calendarBox = new VBox(10);
        calendarBox.setPrefWidth(812);

        HBox dayHeaders = new HBox(0);
        dayHeaders.getStyleClass().add("calendar-header-row");
        for (DayOfWeek day : DayOfWeek.values()) {
            Label dayLabel = new Label(day.getDisplayName(TextStyle.SHORT, Locale.FRENCH));
            dayLabel.getStyleClass().add("calendar-day-header");
            dayLabel.setPrefWidth(110);
            dayLabel.setAlignment(Pos.CENTER);
            dayHeaders.getChildren().add(dayLabel);
        }

        calendarGrid.setHgap(6);
        calendarGrid.setVgap(6);
        calendarBox.getChildren().addAll(dayHeaders, calendarGrid);

        VBox sidePanel = new VBox(10);
        sidePanel.getStyleClass().addAll("panel", "calendar-side-panel");
        sidePanel.setPrefWidth(340);

        Label sideTitle = new Label("Événements du jour");
        sideTitle.getStyleClass().add("section-title");

        selectedDateLabel.getStyleClass().add("page-subtitle");
        eventsListView.getStyleClass().add("calendar-day-list");

        sidePanel.getChildren().addAll(sideTitle, selectedDateLabel, eventsListView);
        VBox.setVgrow(eventsListView, Priority.ALWAYS);

        content.getChildren().addAll(calendarBox, sidePanel);
        HBox.setHgrow(calendarBox, Priority.ALWAYS);
        return content;
    }

    private Node createDayEventsPlaceholder(String message) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16));

        Label icon = new Label("CAL");
        icon.getStyleClass().add("empty-state-icon");

        Label text = new Label(message);
        text.getStyleClass().add("page-subtitle");
        text.setWrapText(true);
        text.setAlignment(Pos.CENTER);

        box.getChildren().addAll(icon, text);
        return box;
    }

    private void refreshMonth() {
        List<Evenement> monthEvents = evenementService.getEvenementsParMois(currentMonth);
        monthEvents = applyStatusFilter(monthEvents);
        eventsByDate = new HashMap<>();
        for (Evenement e : monthEvents) {
            if (e.getDate_debut() == null) {
                continue;
            }
            LocalDate date = e.getDate_debut().toLocalDate();
            eventsByDate.computeIfAbsent(date, k -> new java.util.ArrayList<>()).add(e);
        }

        if (selectedDate != null && !YearMonth.from(selectedDate).equals(currentMonth)) {
            selectedDate = null;
        }

        monthLabel.setText(currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH) + " " + currentMonth.getYear());
        buildCalendarGrid();
        loadDayEvents(selectedDate);
    }

    private void buildCalendarGrid() {
        calendarGrid.getChildren().clear();
        LocalDate firstDay = currentMonth.atDay(1);
        int startOffset = (firstDay.getDayOfWeek().getValue() + 6) % 7;
        int daysInMonth = currentMonth.lengthOfMonth();

        int dayCounter = 1;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                VBox cell = new VBox(4);
                cell.getStyleClass().add("calendar-cell");
                cell.setPadding(new Insets(6));
                cell.setPrefSize(110, 84);

                int position = row * 7 + col;
                if (position >= startOffset && dayCounter <= daysInMonth) {
                    LocalDate date = currentMonth.atDay(dayCounter);
                    Label dayNumber = new Label(String.valueOf(dayCounter));
                    dayNumber.getStyleClass().add("calendar-day-number");

                    if (date.equals(LocalDate.now())) {
                        cell.getStyleClass().add("calendar-cell-today");
                    }
                    if (date.equals(selectedDate)) {
                        cell.getStyleClass().add("calendar-cell-selected");
                    }

                    List<Evenement> events = eventsByDate.getOrDefault(date, List.of());
                    if (!events.isEmpty()) {
                        cell.getStyleClass().add("calendar-cell-has-events");
                        String text = events.size() == 1 ? events.get(0).getTitre() : events.size() + " événements";
                        Label summary = new Label(text);
                        summary.getStyleClass().add("calendar-event-summary");
                        summary.setWrapText(true);
                        cell.getChildren().addAll(dayNumber, summary);
                    } else {
                        cell.getChildren().add(dayNumber);
                    }

                    cell.setOnMouseClicked(e -> {
                        selectedDate = date;
                        buildCalendarGrid();
                        loadDayEvents(date);
                    });

                    dayCounter++;
                }
                calendarGrid.add(cell, col, row);
            }
        }
    }

    private void loadDayEvents(LocalDate date) {
        if (date == null) {
            eventsListView.getItems().clear();
            selectedDateLabel.setText("Aucune date selectionnee");
            return;
        }
        selectedDateLabel.setText("Date: " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        List<Evenement> events = eventsByDate.getOrDefault(date, List.of());
        eventsListView.getItems().setAll(events);
    }

    private List<Evenement> applyStatusFilter(List<Evenement> source) {
        if (source == null) {
            return List.of();
        }
        String selected = statutFilter.getValue();
        if (selected == null || selected.equalsIgnoreCase("Tous")) {
            return source;
        }
        String target = normalizeStatus(selected);
        return source.stream()
                .filter(e -> normalizeStatus(e.getStatut()).equals(target))
                .collect(Collectors.toList());
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "";
        }
        String v = value.toLowerCase(Locale.ROOT);
        if (v.equals("fermé") || v.equals("ferme")) {
            return "ferme";
        }
        if (v.equals("planifié") || v.equals("planifie")) {
            return "planifie";
        }
        return v;
    }

    public void refreshData() {
        refreshMonth();
    }

    public BorderPane getView() {
        return mainLayout;
    }
}
