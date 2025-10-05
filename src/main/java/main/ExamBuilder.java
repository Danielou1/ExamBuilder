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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        System.out.println("fxml loaded");

        controller.MainController controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        primaryStage.setTitle("ExamBuilder");
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap");
        primaryStage.setScene(scene);
        System.out.println("showing stage");
        primaryStage.show();
        System.out.println("stage shown");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
