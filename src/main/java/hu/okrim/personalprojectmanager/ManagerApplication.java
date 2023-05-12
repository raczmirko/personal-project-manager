package hu.okrim.personalprojectmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class ManagerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ManagerApplication.class.getResource("manager-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        stage.setTitle("Personal Project Manager");
        // Set the application icon if it exists
        InputStream iconStream = getClass().getResourceAsStream("icon.png");
        Image icon;
        if (iconStream != null) {
            icon = new Image(iconStream);
            stage.getIcons().add(icon);
        }
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}