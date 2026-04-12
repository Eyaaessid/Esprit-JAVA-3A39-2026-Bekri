package org.example;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.community.model.Comment;
import org.example.community.model.CommentFormData;

import java.util.Optional;

public class CommentFormController {
    @FXML
    private TextArea contentArea;
    @FXML
    private Label validationLabel;

    private Stage dialogStage;
    private CommentFormData result;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void fillFromComment(Comment comment) {
        contentArea.setText(comment.getContenu());
    }

    public Optional<CommentFormData> showAndWait() {
        dialogStage.showAndWait();
        return Optional.ofNullable(result);
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleSave() {
        String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
        if (content.isBlank()) {
            validationLabel.setText("Comment content is required.");
            return;
        }

        result = new CommentFormData(content);
        dialogStage.close();
    }
}
