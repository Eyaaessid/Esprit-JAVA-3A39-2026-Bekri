package tn.esprit.user.ui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.shared.DialogHelper;
import tn.esprit.shared.SceneManager;
import tn.esprit.user.dao.ReactivationRequestDao;
import tn.esprit.user.dao.UtilisateurDao;
import tn.esprit.user.entity.ReactivationRequest;
import tn.esprit.user.service.AccountStatusService;
import tn.esprit.user.service.EmailService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReactivationRequestsController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label headerLabel;
    @FXML private TableView<ReactivationRequest> requestsTable;
    @FXML private TableColumn<ReactivationRequest, String> fullNameColumn;
    @FXML private TableColumn<ReactivationRequest, String> emailColumn;
    @FXML private TableColumn<ReactivationRequest, String> reasonColumn;
    @FXML private TableColumn<ReactivationRequest, String> requestedAtColumn;
    @FXML private TableColumn<ReactivationRequest, ReactivationRequest> actionsColumn;
    @FXML private Label emptyStateLabel;

    private final ObservableList<ReactivationRequest> requests = FXCollections.observableArrayList();
    private final ReactivationRequestDao requestDao = new ReactivationRequestDao();
    private final AccountStatusService accountStatusService = new AccountStatusService(
            new UtilisateurDao(),
            requestDao,
            new EmailService()
    );

    @FXML
    private void initialize() {
        requestsTable.setItems(requests);
        fullNameColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getUtilisateur() != null ? data.getValue().getUtilisateur().getFullName().trim() : "-"
        ));
        emailColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getUtilisateur() != null ? data.getValue().getUtilisateur().getEmail() : "-"
        ));
        reasonColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(truncate(data.getValue().getReason())));
        requestedAtColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().getRequestedAt() != null ? data.getValue().getRequestedAt().format(DATE_TIME_FORMATTER) : "-"
        ));
        actionsColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ReactivationRequest request, boolean empty) {
                super.updateItem(request, empty);
                if (empty || request == null) {
                    setGraphic(null);
                    return;
                }
                Button approveBtn = new Button("Approuver");
                approveBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
                approveBtn.setOnAction(event -> confirmApprove(request));

                Button denyBtn = new Button("Refuser");
                denyBtn.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
                denyBtn.setOnAction(event -> openDenyDialog(request));

                setGraphic(new HBox(8, approveBtn, denyBtn));
            }
        });
        loadRequests();
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchTo("admin-users");
        } catch (IOException e) {
            System.err.println("[ReactivationRequestsController.handleBack] " + e.getMessage());
        }
    }

    private void loadRequests() {
        runAsync(() -> {
            try {
                var loaded = requestDao.findPendingWithUsers();
                Platform.runLater(() -> {
                    requests.setAll(loaded);
                    headerLabel.setText("Demandes de réactivation en attente (" + loaded.size() + ")");
                    boolean empty = loaded.isEmpty();
                    requestsTable.setVisible(!empty);
                    requestsTable.setManaged(!empty);
                    emptyStateLabel.setVisible(empty);
                    emptyStateLabel.setManaged(empty);
                });
            } catch (Exception e) {
                System.err.println("[ReactivationRequestsController.loadRequests] " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    private void confirmApprove(ReactivationRequest request) {
        if (!DialogHelper.showConfirm("Confirmer", "Voulez-vous approuver cette demande de reactivation ?")) {
            return;
        }
        runAsync(() -> {
            try {
                accountStatusService.approveRequest(request.getId());
                loadRequests();
            } catch (Exception e) {
                System.err.println("[ReactivationRequestsController.confirmApprove] requestId="
                        + request.getId() + " " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    private void openDenyDialog(ReactivationRequest request) {
        Optional<String> note = showNoteDialog();
        note.ifPresent(value -> runAsync(() -> {
            try {
                accountStatusService.denyRequest(request.getId(), value);
                loadRequests();
            } catch (Exception e) {
                System.err.println("[ReactivationRequestsController.openDenyDialog] requestId="
                        + request.getId() + " " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }));
    }

    private Optional<String> showNoteDialog() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (SceneManager.getPrimaryStage() != null) {
            stage.initOwner(SceneManager.getPrimaryStage());
        }

        Label label = new Label("Note administrateur (optionnelle)");
        TextArea textArea = new TextArea();
        textArea.setPromptText("Expliquez brièvement la raison du refus...");
        textArea.setWrapText(true);
        textArea.setPrefRowCount(6);

        Button confirmBtn = new Button("Confirmer");
        Button cancelBtn = new Button("Annuler");
        final String[] value = new String[1];

        confirmBtn.setOnAction(event -> {
            value[0] = textArea.getText();
            stage.close();
        });
        cancelBtn.setOnAction(event -> {
            value[0] = null;
            stage.close();
        });

        VBox root = new VBox(12, label, textArea, new HBox(8, confirmBtn, cancelBtn));
        root.setPadding(new Insets(16));
        stage.setScene(new Scene(root, 420, 240));
        stage.setTitle("Refuser la demande");
        stage.showAndWait();
        return value[0] == null ? Optional.empty() : Optional.ofNullable(value[0]);
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.length() <= 60 ? reason : reason.substring(0, 57) + "...";
    }

    private void runAsync(Runnable runnable) {
        Thread thread = new Thread(runnable, "reactivation-requests-task");
        thread.setDaemon(true);
        thread.start();
    }
}
