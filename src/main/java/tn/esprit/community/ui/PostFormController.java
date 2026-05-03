package tn.esprit.community.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.community.model.Post;
import tn.esprit.community.model.PostFormData;
import tn.esprit.community.model.UserSummary;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class PostFormController {
    @FXML
    private ComboBox<UserSummary> authorComboBox;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;
    @FXML
    private Label imagePathLabel;

    private Stage dialogStage;
    private PostFormData result;
    private Path selectedImagePath;
    private boolean removeExistingImage;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList("", "Health", "Nutrition", "Mental", "Fitness"));
        categoryComboBox.getSelectionModel().selectFirst();
        imagePathLabel.setText("No image selected");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setUsers(List<UserSummary> users, UserSummary selectedUser) {
        authorComboBox.setItems(FXCollections.observableArrayList(users));
        authorComboBox.getSelectionModel().select(selectedUser);
    }

    public void fillFromPost(Post post) {
        titleField.setText(post.getTitre());
        contentArea.setText(post.getContenu());
        categoryComboBox.getSelectionModel().select(post.getCategorie() == null ? "" : post.getCategorie());
        imagePathLabel.setText(post.getMediaUrl() == null || post.getMediaUrl().isBlank() ? "No image selected" : post.getMediaUrl());
        removeExistingImage = false;
    }

    public Optional<PostFormData> showAndWait() {
        dialogStage.showAndWait();
        return Optional.ofNullable(result);
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = chooser.showOpenDialog(dialogStage);
        if (file != null) {
            selectedImagePath = file.toPath();
            removeExistingImage = false;
            imagePathLabel.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleRemoveImage() {
        selectedImagePath = null;
        removeExistingImage = true;
        imagePathLabel.setText("Image will be removed");
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleSave() {
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
        if (title.isBlank() || content.isBlank()) {
            imagePathLabel.setText("Title and content are required.");
            return;
        }

        result = new PostFormData(
                title,
                categoryComboBox.getValue(),
                content,
                selectedImagePath,
                removeExistingImage
        );
        dialogStage.close();
    }
}


