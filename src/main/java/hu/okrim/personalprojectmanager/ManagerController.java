package hu.okrim.personalprojectmanager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class ManagerController {

    //Input Fields
    @FXML
    private TextField inputServer;
    @FXML
    private TextField inputDatabase;
    @FXML
    private TextField inputUsername;
    @FXML
    private TextField inputPassword;
    @FXML
    private ToggleButton toggleSavePassword;

    //Buttons
    @FXML
    private Button btnLogin;

    //RadioButtons
    @FXML
    private RadioButton radioDerby;
    @FXML
    private RadioButton radioSS;

    //LOGIN-related functions
    public void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void login(){
        //Checking if the input is not null
        String server = inputServer.getText();
        String database = inputDatabase.getText();
        String username = inputUsername.getText();
        String password = inputPassword.getText();

        if(server.equals("") || database.equals("") || username.equals("") || password.equals("")){
            showErrorDialog("You must fill every field!");
        }
        else{
            if(toggleSavePassword.isSelected()){
                saveNewLoginInfo(server, database, username, password);
            }
            else{
                saveNewLoginInfo(server, database, username);
            }
            //Login to server
            //TODO
        }
    }
    public void saveNewLoginInfo(String server, String db, String user, String password){
        //Encrypt the password
        String encryptedPassword = "";
        try {
            encryptedPassword = Encryptor.encryptString(password);
        } catch (NoSuchAlgorithmException e) {
            showErrorDialog("ERROR: Encrypting the password has failed. :(");
        }
        if(!encryptedPassword.equals("")){
            //At this point exceptions are already checked for
            FileWriter writer = null;
            try{
                String filePath = "src/main/resources/hu/okrim/personalprojectmanager/server.JSON";
                writer = new FileWriter(filePath);
                writer.write('"' + server + "-" + db + "-" + user + "-" + encryptedPassword + '"');
            } catch(IOException exception){
                showErrorDialog("ERROR: Server configuration file is not found. :(");
            } finally {
                if (writer != null){
                    try {
                        writer.close();
                    } catch (IOException exception){
                        showErrorDialog("ERROR: Failed to saved server configuration. :(");
                    }
                }
            }
        }
    }
    public void saveNewLoginInfo(String server, String db, String user){
        //At this point exceptions are already checked for
        FileWriter writer = null;
        try{
            String filePath = "src/main/resources/hu/okrim/personalprojectmanager/server.JSON";
            writer = new FileWriter(filePath);
            writer.write('"' + server + "-" + db + "-" + user + '"');
        } catch(IOException exception){
            showErrorDialog("ERROR: Server configuration file is not found. :(");
        } finally {
            if (writer != null){
                try {
                    writer.close();
                } catch (IOException exception){
                    showErrorDialog("ERROR: Failed to saved server configuration. :(");
                }
            }
        }
    }
    public void saveDatabaseType(){
        String serverType = radioDerby.isSelected() ? "derby" : "ss";
        FileWriter writer = null;
        try{
            String filePath = "src/main/resources/hu/okrim/personalprojectmanager/servertype" +
                    ".JSON";
            writer = new FileWriter(filePath);
            writer.write('"' + serverType + '"');
        } catch(IOException exception){
            showErrorDialog("ERROR: Server-type configuration file is not found. :(");
        } finally {
            if (writer != null){
                try {
                    writer.close();
                } catch (IOException exception){
                    showErrorDialog("ERROR: Failed to saved server configuration. :(");
                }
            }
        }
    }
    public void loadLoginInfo(){

    }
    //Other functions
    public void toggleSavePasswordText(){
        if(toggleSavePassword.isSelected()){
            toggleSavePassword.setText("YES");
        }else{
            toggleSavePassword.setText("NO");
        }
    }
}