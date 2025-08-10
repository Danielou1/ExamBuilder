package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ExamBuilder extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("start method called");
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainView.fxml"));
        System.out.println("fxml loaded");
        primaryStage.setTitle("ExamBuilder");
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        System.out.println("showing stage");
        primaryStage.show();
        System.out.println("stage shown");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
