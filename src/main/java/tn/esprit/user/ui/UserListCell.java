package tn.esprit.user.ui;

import tn.esprit.user.entity.Utilisateur;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Cellule ListView admin : avatar à gauche, nom + email au centre, badges + actions à droite.
 */
public class UserListCell extends ListCell<Utilisateur> {

    private final HBox row = new HBox(14);
    private final Label avatarLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label emailLabel = new Label();
    private final Label dateHintLabel = new Label();
    private final Label roleLabel = new Label();
    private final Label statutLabel = new Label();
    private final Button viewBtn = new Button("👁");
    private final Button editBtn = new Button("✏️");
    private final Button deleteBtn = new Button("🗑");
    private final Region spacer = new Region();

    public UserListCell(Consumer<Utilisateur> onViewAction,
                        Consumer<Utilisateur> onEditAction,
                        Consumer<Utilisateur> onDeleteAction) {
        super();

        row.setMinHeight(72);

        avatarLabel.getStyleClass().add("list-avatar");
        avatarLabel.setMinSize(44, 44);
        avatarLabel.setPrefSize(44, 44);
        avatarLabel.setMaxSize(44, 44);
        avatarLabel.setAlignment(Pos.CENTER);

        nameLabel.getStyleClass().add("list-name");
        emailLabel.getStyleClass().add("list-email");
        dateHintLabel.getStyleClass().add("list-sub");
        dateHintLabel.setStyle("-fx-font-size: 10px;");

        VBox center = new VBox(4, nameLabel, emailLabel, dateHintLabel);
        center.setAlignment(Pos.CENTER_LEFT);
        center.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(center, Priority.ALWAYS);

        roleLabel.setMinWidth(70);
        statutLabel.setMinWidth(70);
        roleLabel.setAlignment(Pos.CENTER);
        statutLabel.setAlignment(Pos.CENTER);

        viewBtn.getStyleClass().add("icon-btn-view");
        editBtn.getStyleClass().add("icon-btn-edit");
        deleteBtn.getStyleClass().add("icon-btn-delete");

        HBox actions = new HBox(8, viewBtn, editBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox right = new HBox(12, roleLabel, statutLabel, spacer, actions);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(avatarLabel, center, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-list-row");
        row.setPadding(new Insets(12, 16, 12, 16));

        viewBtn.setOnAction(e -> {
            Utilisateur u = getItem();
            if (u != null) {
                onViewAction.accept(u);
            }
        });
        editBtn.setOnAction(e -> {
            Utilisateur u = getItem();
            if (u != null) {
                onEditAction.accept(u);
            }
        });
        deleteBtn.setOnAction(e -> {
            Utilisateur u = getItem();
            if (u != null) {
                onDeleteAction.accept(u);
            }
        });
    }

    @Override
    protected void updateItem(Utilisateur user, boolean empty) {
        super.updateItem(user, empty);
        if (empty || user == null) {
            setGraphic(null);
            getStyleClass().removeAll("user-row-even", "user-row-odd");
            return;
        }

        int idx = getIndex();
        getStyleClass().removeAll("user-row-even", "user-row-odd");
        if (idx >= 0) {
            getStyleClass().add(idx % 2 == 0 ? "user-row-even" : "user-row-odd");
        }

        avatarLabel.setText(user.getInitials());

        nameLabel.setText(user.getFullName().trim());
        emailLabel.setText(user.getEmail());
        String created = user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDate().toString()
                : "—";
        dateHintLabel.setText("Inscrit le " + created);

        roleLabel.getStyleClass().removeIf(c -> c.startsWith("badge-"));
        roleLabel.setText(capitalize(user.getRoleKey()));
        roleLabel.getStyleClass().addAll("role-badge-row", "badge-" + user.getRoleKey());

        statutLabel.getStyleClass().removeIf(c -> c.startsWith("statut-"));
        String statut = user.getStatutKey() != null && !user.getStatutKey().isEmpty()
                ? user.getStatutKey() : "—";
        statutLabel.setText(capitalize(statut));
        statutLabel.getStyleClass().addAll("statut-badge-row", "statut-" + statut.toLowerCase());

        deleteBtn.setVisible(!"admin".equalsIgnoreCase(user.getRoleKey()));

        setGraphic(row);
        setStyle("-fx-padding: 0; -fx-background-color: transparent;");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
