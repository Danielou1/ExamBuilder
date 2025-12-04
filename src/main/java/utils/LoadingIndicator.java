package utils;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Utility class for displaying a modal loading indicator to the user.
 * This indicator consists of a {@link javafx.scene.control.ProgressIndicator}
 * and a "Loading..." label, shown in an undecorated, application-modal stage.
 * It is used to provide visual feedback during long-running operations (e.g., export).
 */
public class LoadingIndicator {

    private static final Stage dialog = new Stage();

    static {
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().add(new ProgressIndicator());
        vbox.getChildren().add(new Label("Loading..."));
        Scene scene = new Scene(vbox, 200, 100);
        dialog.setScene(scene);
    }

    /**
     * Makes the loading indicator dialog visible.
     */
    public static void show() {
        dialog.show();
    }

    /**
     * Makes the loading indicator dialog invisible.
     */
    public static void hide() {
        dialog.hide();
    }
}
