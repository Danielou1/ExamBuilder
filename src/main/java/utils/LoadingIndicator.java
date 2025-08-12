package utils;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

    public static void show() {
        dialog.show();
    }

    public static void hide() {
        dialog.hide();
    }
}
