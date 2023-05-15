package hu.okrim.personalprojectmanager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import java.net.URL;
import java.util.ResourceBundle;

public class LoadingController implements Initializable {
    @FXML
    public ProgressBar progressbar;
    private Thread progressThread;
    private static boolean isRunning;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isRunning = true;
    }
    public void startProgressBarThread() {
        progressThread = new Thread(() -> {
            while (isRunning) {
                for (double progress = 0; progress <= 1.0; progress += 0.01) {
                    double finalProgress = progress;
                    Platform.runLater(() -> progressbar.setProgress(finalProgress));
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        progressThread.setDaemon(true);
        progressThread.start();
    }
    public void stopProgressBarThread() {
        isRunning = false;
        progressThread.interrupt();
    }
}