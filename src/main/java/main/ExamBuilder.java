package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the ExamBuilder JavaFX application.
 * This class extends {@link javafx.application.Application} and is
 * responsible for initializing the primary stage, loading the main
 * user interface from `MainView.fxml`, and setting up the core controller.
 */
public class ExamBuilder extends Application {

    /**
     * The main entry point for the JavaFX application.
     * This method is called after the application has been launched.
     * It sets up the primary stage, loads the main user interface from `MainView.fxml`,
     * links it with the {@link controller.MainController}, and displays the stage.
     * @param primaryStage The primary stage for this application, onto which the application scene can be set.
     * @throws Exception If an error occurs during FXML loading or stage setup.
     */
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

    /**
     * The main method that launches the JavaFX application.
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
