package hu.okrim.personalprojectmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class ManagerController implements Initializable {
    public ToggleGroup groupDatabaseType;
    public Button btnLogin;
    public Button btnLoadLoginFromFile;
    private String currentDatabase;
    // Input Fields
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
    @FXML
    private ToggleButton toggleTemporaryLogin;

    // Other components
    @FXML
    private ListView<String> listViewTables;
    // RadioButtons
    @FXML
    private RadioButton radioDerby;
    @FXML
    private RadioButton radioSS;

    //---------------------------FUNCTIONS---------------------------------
    // LOGIN-related functions
    public void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    public void showHelpDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
    public void loginDirect(){
        //Checking if the input is not null
        String server = inputServer.getText();
        String database = inputDatabase.getText();
        String username = inputUsername.getText();
        String password = inputPassword.getText();

        // Check if everything is filled out
        if(server.equals("") || database.equals("") || username.equals("") || password.equals("")){
            showErrorDialog("You must fill every field!");
        }
        else{
            // Check if temporary login is OFF
            // Login info is only saved of temp login is OFF
            if(!toggleTemporaryLogin.isSelected()){
                // Checking if password should be saved too
                if(toggleSavePassword.isSelected()){
                    saveNewLoginInfo(server, database, username, password);
                }
                else{
                    saveNewLoginInfo(server, database, username);
                }
            }
            // Create connection URL
            String connectionURL = ConnectionController.getConnectionURLSS(server, database,
                    username, password);
            finalizeConnection(database, connectionURL);
        }
    }

    private void finalizeConnection(String database, String connectionURL) {
        // Establish connection
        try (Connection connection = ConnectionController.establishConnection(connectionURL)){
            showHelpDialog("Connection Successful!", "You have successfully connected to the " +
                    "database!");
            // Saving the current connected DB into a variable for easy access
            currentDatabase = database;
            // Loading the results into the listView on the Manage Data tab
            showTablesInList(connection);
            // Adding eventListener to the ListView
            createListViewListeners(listViewTables);
        } catch (SQLException ex){
            showErrorDialog("ERROR: There was an error at the server login. Check if you gave" +
                    " all the credentials correctly!");
            System.out.println(ex.getMessage());
        }
    }

    // Background login when not logging in directly on the GUI
    public void loginAuto(){
        //-----------------------------------VARIABLES-------------------------------------
        String defaultConfigPath = "src/main/resources/hu/okrim/personalprojectmanager" +
                "/server.JSON";
        ArrayList<String> loginInfo = loadLoginInfo(defaultConfigPath);
        String connectionURL;
        if(loginInfo.size() == 3){
            connectionURL = ConnectionController.getConnectionURLSS(
                    loginInfo.get(0),
                    loginInfo.get(1),
                    loginInfo.get(2),
                    inputPassword.getText()
            );
        }
        else{
            connectionURL = ConnectionController.getConnectionURLSS(
                    loginInfo.get(0),
                    loginInfo.get(1),
                    loginInfo.get(2),
                    loginInfo.get(3)
            );
        }
        // Index 1 is the database since saving follows the following pattern:
        // server;database;user;password
        String database = loginInfo.get(1);
        finalizeConnection(database, connectionURL);
    }
    public void saveNewLoginInfo(String server, String db, String user, String password){
        //At this point exceptions are already checked for
        FileWriter writer = null;
        try{
            String filePath = "src/main/resources/hu/okrim/personalprojectmanager/server.JSON";
            writer = new FileWriter(filePath);
            writer.write('"' + server + ";" + db + ";" + user + ";" + password + '"');
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
    public void saveNewLoginInfo(String server, String db, String user){
        //At this point exceptions are already checked for
        FileWriter writer = null;
        try{
            String filePath = "src/main/resources/hu/okrim/personalprojectmanager/server.JSON";
            writer = new FileWriter(filePath);
            writer.write('"' + server + ";" + db + ";" + user + '"');
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
    public ArrayList<String> loadLoginInfo(String path){
        ArrayList<String> loginInfo = new ArrayList<>();
        Scanner scanner;
        try{
            scanner = new Scanner(new File(path));
            // Read the content of the file line by line
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //Since I save the JSON files with quotation marks I need to cut them off
                //Since the replaceAll function only takes Strings I generate a String
                //From the ASCII code of a quotation mark
                //(char) 34 = '"'
                line = line.replaceAll(Character.toString((char) 34), "");
                //Then I split the line at each separator (I'm using dashes)
                String[] words = line.split(";");
                //Load the words into the return Stack
                loginInfo.addAll(Arrays.asList(words));
            }
            // Close the Scanner
            scanner.close();
        } catch(FileNotFoundException FNFE){
            showErrorDialog("ERROR: Server login config files not found! :(");
        }
        return loginInfo;
    }
    public File loadLoginInfoFile(){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose A File!");

        // Set initial directory (optional)
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // Show open dialog
        return fileChooser.showOpenDialog(new Stage());
    }
    public void handleFileChooseButtonAction(){
        // Saving the chosen file
        File settingsFile = loadLoginInfoFile();
        // Loading the settings from selected file
        ArrayList<String> loginInfo = loadLoginInfo(settingsFile.getAbsolutePath());
        // Displaying the information in the TextInputs
        inputServer.setText(loginInfo.get(0));
        inputDatabase.setText(loginInfo.get(1));
        inputUsername.setText(loginInfo.get(2));
        if(loginInfo.size() == 4){
            inputPassword.setText(loginInfo.get(3));
        }
    }

    // OTHER functions
    public void toggleSavePasswordText(){
        if(toggleSavePassword.isSelected()){
            toggleSavePassword.setText("Save password (unsafe)");
        }else{
            toggleSavePassword.setText("Do not save password");
        }
    }
    public void toggleTemporaryLogin(){
        if(toggleTemporaryLogin.isSelected()){
            toggleTemporaryLogin.setText("Temporary login ON");
            toggleSavePassword.setSelected(false);
            toggleSavePassword.setDisable(true);
        }
        else{
            toggleTemporaryLogin.setText("Temporary login OFF");
            toggleSavePassword.setDisable(false);
        }
    }
    public void handleTellMeMoreButtonAction(){
        String message =
                """
                You password is stored in the server.JSON file WITHOUT encryption.
                
                If you want the maximum amount of security, choose the "Do not save password" option!
                
                You should not use this application to connect to production servers!!!
                
                You should only use this app at your own risk!
                
                """;
        showHelpDialog("What happens with you password?", message);
    }
    public void loadServerTypeRadioButtonState(){
        Scanner scanner;
        try{
            String serverPath = "src/main/resources/hu/okrim/personalprojectmanager" +
                    "/servertype.JSON";
            scanner = new Scanner(new File(serverPath));
            // Read the content of the file line by line
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //Since I save the JSON files with quotation marks I need to cut them off
                //Since the replaceAll function only takes Strings I generate a String
                //From the ASCII code of a quotation mark
                //(char) 34 = '"'
                line = line.replaceAll(Character.toString((char) 34), "");
                if(line.equals("derby")){
                    radioDerby.setSelected(true);
                }
                else{
                    radioSS.setSelected(true);
                }
            }
            // Close the Scanner
            scanner.close();
        } catch(FileNotFoundException FNFE){
            showErrorDialog("ERROR: Servertype config files not found! :(");
        }
    }
    public void showTablesInList(Connection connection){
        String sqlString =  "SELECT TABLE_NAME\n" +
                            "FROM INFORMATION_SCHEMA.TABLES\n" +
                            "WHERE TABLE_TYPE = 'BASE TABLE'\n" +
                            "  AND TABLE_CATALOG = '" + currentDatabase + "'\n";
        System.out.println("\nSQL: " + sqlString);
        try{
            Statement statement = connection.createStatement();
            //Execute the CREATE TABLE statement
            ResultSet resultSet = statement.executeQuery(sqlString);
            List<String> tableNames = new ArrayList<>();
            while (resultSet.next()){
                // Adding each result from the result set to the list on second tab
                String tableName = resultSet.getString("table_name");
                tableNames.add(tableName);
                // Create an ObservableList from the table names
                ObservableList<String> items = FXCollections.observableArrayList(tableNames);
                // Set the items in the ListView
                listViewTables.setItems(items);
            }
        } catch (SQLTimeoutException SQLTOE){
            showErrorDialog("ERROR: You request to load the database tables has timed out.");
        } catch(SQLException SQLE){
            showErrorDialog("ERROR: Loading the database tables was unsuccessful. :(");
        }

    }
    public void createListViewListeners(ListView<String> listView){
        // Adding a click listener for double clicks
        listView.setOnMouseClicked(event -> {
            String selectedTableName = listView.getSelectionModel().getSelectedItem();
            if (selectedTableName != null) {
                if (event.getClickCount() > 1) {
                    System.out.println("Selected table: " + selectedTableName);
                }
            }
        });
        // Adding a keyboard listener for enter
        listView.setOnKeyPressed(event -> {
            String selectedTableName = listView.getSelectionModel().getSelectedItem();
            if (selectedTableName != null) {
                if (event.getCode() == KeyCode.ENTER) {
                    System.out.println("Selected table: " + selectedTableName);
                }
            }
        });
    }
    // Startup functions
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadServerTypeRadioButtonState();
        loginAuto();
    }
}
