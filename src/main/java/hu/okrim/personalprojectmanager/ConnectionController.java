package hu.okrim.personalprojectmanager;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

public class ConnectionController {
    public static Stage loadingStage;
    static LoadingController loadingController = null;
    public static String getConnectionURLSS(String server, String db, String user, String password){
        return "jdbc:sqlserver://" + server + ";"
                        + "database=" + db + ";"
                        + "user=" + user + ";"
                        + "password=" + password + ";"
                        + "encrypt=true;"
                        + "trustServerCertificate=true;"
                        + "loginTimeout=30;";
    }
    public static Connection establishConnection(String URL) throws SQLException {
        return DriverManager.getConnection(URL);
    }
    public static CompletableFuture<Connection> establishManualConnection(String URL) {
        CompletableFuture<Connection> connectionFuture = new CompletableFuture<>();
        generateLoadingPopup();
        Task<Connection> connectionTask = new Task<>() {

            @Override
            protected Connection call() throws Exception {
                return DriverManager.getConnection(URL);
            }
        };
        connectionTask.setOnSucceeded(event -> {
            Connection connection = connectionTask.getValue();
            closeLoadingPopup();
            connectionFuture.complete(connection);
        });
        connectionTask.setOnFailed(event -> {
            Throwable exception = connectionTask.getException();
            closeLoadingPopup();
            connectionFuture.completeExceptionally(exception);
        });
        Thread connectionThread = new Thread(connectionTask);
        connectionThread.start();
        return connectionFuture;
    }
    public static void generateLoadingPopup() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ManagerApplication.class.getResource("loading-view.fxml"));
            Parent root = fxmlLoader.load();
            LoadingController loadingController = fxmlLoader.getController();
            // Saving the controller so that when the popup is destroyed Thread can be killed
            ConnectionController.loadingController = loadingController;
            Scene scene = new Scene(root);
            loadingStage = new Stage();
            loadingStage.setTitle("Connecting...");
            loadingStage.setScene(scene);
            loadingStage.show();
            loadingController.startProgressBarThread();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void closeLoadingPopup() {
        loadingStage.close();
        loadingController.stopProgressBarThread();
    }
}