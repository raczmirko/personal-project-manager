package hu.okrim.personalprojectmanager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ManagerController implements Initializable {
    private Paint lastColour = null;
    public static String selectedTable = null;
    public static String selectedView = null;
    public static String currentConnectionURL = null;
    private String currentDatabase;
    private final String SERVERTYPECONFIGPATH = "src/main/resources/hu/okrim" +
            "/personalprojectmanager" +
            "/servertype.JSON";
    private final String SERVERCONFIGPATH = "src/main/resources/hu/okrim" +
            "/personalprojectmanager" +
            "/server.JSON";
    @FXML
    public ToggleGroup groupDatabaseType;
    @FXML
    public Button btnLogin;
    @FXML
    public Button btnLoadLoginFromFile;
    @FXML
    public Label connectionStatusLabel;
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
    @FXML
    private ListView<String> listViewViews;
    // RadioButtons
    @FXML
    private RadioButton radioDerby;
    @FXML
    private RadioButton radioSS;

    //---------------------------FUNCTIONS---------------------------------
    // LOGIN-related functions
    public static void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
    public static void showHelpDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
    public static boolean showConfirmationDialog(String title, String message) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm your action!");
        confirmation.setHeaderText(title);
        confirmation.setContentText(message);
        confirmation.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        confirmation.showAndWait();
        // Return TRUE if user clicked OK, and FALSE if the user cancelled
        return confirmation.getResult() == ButtonType.OK;
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
            finalizeConnection(database, connectionURL, false);
        }
    }
    private void finalizeConnection(String database, String connectionURL, boolean autoLogin) {
        // Saving the passed URL into a variable
        currentConnectionURL = connectionURL;

        if (autoLogin) {
            // Establish connection
            try (Connection connection = ConnectionController.establishConnection(connectionURL)) {
                // Saving the current connected DB into a variable for easy access
                initializeGUIAfterLogin(database, connection);
                updateStatusLabel(true,true);
            } catch (SQLException ex) {
                showErrorDialog("ERROR: There was an error at the server login. Check if you gave" +
                        " all the credentials correctly!");
                updateStatusLabel(false,true);
            }
        } else {
            // Establish connection manually
            CompletableFuture<Connection> connectionFuture =
                    ConnectionController.establishManualConnection(connectionURL);
            connectionFuture.thenAccept(connection -> {
                if (connection != null) {
                    // Connection succeeded
                    Platform.runLater(() -> {
                        initializeGUIAfterLogin(database, connection);
                        showHelpDialog("Connection Successful!",
                                "You have successfully connected to the database!");
                        updateStatusLabel(true,false);
                    });
                } else {
                    // Connection failed
                    Platform.runLater(() -> showErrorDialog("ERROR: There was an error at the server login. Check if you gave" +
                            " all the credentials correctly!"));
                    updateStatusLabel(false,false);
                }
            }).exceptionally(exception -> {
                // Handle exception
                Platform.runLater(() -> showErrorDialog(exception.getMessage()));
                updateStatusLabel(false,false);
                return null;
            });
        }
    }
    private void initializeGUIAfterLogin(String database, Connection connection) {
        currentDatabase = database;
        showTablesInList(connection, listViewTables);
        createListViewListeners(listViewTables);
        showTablesInList(connection, listViewViews);
        createListViewListeners(listViewViews);
    }

    // Background login when not logging in directly on the GUI
    private void loginAuto(){
        //-----------------------------------VARIABLES-------------------------------------
        ArrayList<String> loginInfo = loadLoginInfo(SERVERCONFIGPATH);
        String connectionURL;
        if(loginInfo.size() == 4){
            connectionURL = ConnectionController.getConnectionURLSS(
                    loginInfo.get(0),
                    loginInfo.get(1),
                    loginInfo.get(2),
                    loginInfo.get(3)
            );
            // Index 1 is the database since saving follows the following pattern:
            // server;database;user;password
            String database = loginInfo.get(1);
            finalizeConnection(database, connectionURL, true);
        }
    }
    private void saveNewLoginInfo(String server, String db, String user, String password){
        //At this point exceptions are already checked for
        FileWriter writer = null;
        try{
            writer = new FileWriter(SERVERCONFIGPATH);
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
    private void saveNewLoginInfo(String server, String db, String user){
        //At this point exceptions are already checked for
        FileWriter writer = null;
        try{
            writer = new FileWriter(SERVERCONFIGPATH);
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
            writer = new FileWriter(SERVERTYPECONFIGPATH);
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
    private ArrayList<String> loadLoginInfo(String path){
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
    private File loadLoginInfoFile(){
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
            scanner = new Scanner(new File(SERVERTYPECONFIGPATH));
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
    public void showTablesInList(Connection connection, ListView<String> listView){
        String sqlString;
        if (listView == listViewTables) {
            sqlString = "SELECT TABLE_NAME\n" +
                                "FROM INFORMATION_SCHEMA.TABLES\n" +
                                "WHERE TABLE_TYPE = 'BASE TABLE'\n" +
                                "  AND TABLE_CATALOG = '" + currentDatabase + "'\n";
        } else {
            sqlString = "SELECT TABLE_NAME\n" +
                    "FROM INFORMATION_SCHEMA.TABLES\n" +
                    "WHERE TABLE_TYPE = 'VIEW'\n" +
                    "  AND TABLE_CATALOG = '" + currentDatabase + "'\n";
        }
        try{
            Statement statement = connection.createStatement();
            // Execute the CREATE TABLE statement
            ResultSet resultSet = statement.executeQuery(sqlString);
            List<String> tableNames = new ArrayList<>();
            // Getting the name of each table in the database
            while (resultSet.next()){
                // Adding each result from the result set to the list on second tab
                String tableName = resultSet.getString("table_name");
                tableNames.add(tableName);
            }
            // Create an ObservableList from the table names
            ObservableList<String> items = FXCollections.observableArrayList(tableNames);

            // Set the cell factory for the ListView to produce fun colourful cells
            setCellFactory(listView);
            // Set the items in the ListView
            listView.setItems(items);
        } catch (SQLTimeoutException SQLTOE){
            showErrorDialog("ERROR: You request to load the database tables has timed out.");
        } catch(SQLException SQLE){
            showErrorDialog("ERROR: Loading the database tables was unsuccessful. :(");
        }
    }
    public void setCellFactory(ListView<String> listView) {
        listView.setCellFactory(param -> new ListCell<>() {
            private boolean isInitialized = false;
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setBackground(null);
                } else {
                    setText(item);
                    setFont(Font.font("Unispace", FontWeight.BOLD, 18));
                    setAlignment(Pos.CENTER);
                    setBorder(new Border(new BorderStroke(null, BorderStrokeStyle.SOLID, null, null)));
                    // Set the background color only if the cell has not been initialized
                    if (!isInitialized) {
                        BackgroundFill backgroundFill = new BackgroundFill(pickRandomColor(), null, null);
                        setBackground(new Background(backgroundFill));
                        isInitialized = true;
                    }
                }
            }
        });
    }
    public void showTablePopup(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ManagerApplication.class.getResource("table" +
                    "-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage tableStage = new Stage();
            tableStage.setTitle("Personal Project Manager - Editor");
            tableStage.setScene(scene);
            setStageIcon(tableStage);
            tableStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void showViewPopup(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(ManagerApplication.class.getResource("view" +
                    "-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage viewStage = new Stage();
            viewStage.setTitle("Personal Project Manager - Views");
            viewStage.setScene(scene);
            setStageIcon(viewStage);
            viewStage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static void setStageIcon(Stage stage){
        // Set the application icon if it exists
        InputStream iconStream = ManagerController.class.getResourceAsStream("icon.png");
        Image icon;
        if (iconStream != null) {
            icon = new Image(iconStream);
            stage.getIcons().add(icon);
        }
    }
    private Paint pickRandomColor() {
        Paint[] colors = {
                Color.rgb(129, 190, 131),   // Payton
                Color.rgb(234, 135, 30),  // Supreme Orange
                Color.rgb(24, 102, 225),  // Royal Azure
                Color.rgb(255, 204, 51),   // Sunglow
                Color.rgb(99, 183, 183),   // Pastel Teal
                Color.rgb(200, 77, 142),   // Royal Lilac
                Color.rgb(133, 113, 83),  // Dark Khaki
                Color.rgb(27, 121, 49)    // Deep Emerald
        };
        Paint currentColor;
        // If the randomly chose colour is the same as the previous one, re-pick
        do {
            currentColor = colors[(int)(Math.random()*colors.length)];
        } while(currentColor.equals(lastColour));
        lastColour = currentColor;
        // Returning a random colour from the list
        // Between index 0 and length of the list
        return currentColor;
    }
    private void createListViewListeners(ListView<String> listView){
        // Adding a click listener for double clicks
        listView.setOnMouseClicked(event -> {
            String selectedElementName = listView.getSelectionModel().getSelectedItem();
            if (selectedElementName != null) {
                if (event.getClickCount() > 1) {
                    if (listView == listViewTables) {
                        selectedTable = selectedElementName;
                        showTablePopup();
                    } else {
                        selectedView = selectedElementName;
                        showViewPopup();
                    }
                }
            }
        });
        // Adding a keyboard listener for enter
        listView.setOnKeyPressed(event -> {
            String selectedElementName = listView.getSelectionModel().getSelectedItem();
            if (selectedElementName != null) {
                if (event.getCode() == KeyCode.ENTER) {
                    if (listView == listViewTables) {
                        selectedTable = selectedElementName;
                        showTablePopup();
                    } else {
                        selectedView = selectedElementName;
                        showViewPopup();
                    }
                }
            }
        });
    }
    public void stopApplication(){
        Platform.exit();
    }
    public void showAbout(){
        showHelpDialog("About Personal Project Manager", """
                This application was developed by Mirkó Rácz in May, 2023.
                \nI have always loved manifesting my ideas, and this project didn't start any different: I had a useful idea that I wanted to make reality.
                \nThank you for checking this project out!""");
    }
    public void updateStatusLabel(boolean success,boolean autologin){
        if (success) {
            if(autologin){
                connectionStatusLabel.setText("Auto-connection successful.");
                connectionStatusLabel.setTextFill(Color.DARKGREEN);
            }else{
                connectionStatusLabel.setText("Connection successful.");
                connectionStatusLabel.setTextFill(Color.DARKGREEN);
            }
        } else{
            if(autologin){
                connectionStatusLabel.setText("Auto-connection failed.");
                connectionStatusLabel.setTextFill(Color.RED);
            }else{
                connectionStatusLabel.setText("Connection failed.");
                connectionStatusLabel.setTextFill(Color.RED);
            }
        }
    }
    // Startup functions
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadServerTypeRadioButtonState();
        loginAuto();
        listViewTables.setPlaceholder(new Label("Couldn't connect to database, please fill in the login information!"));
    }
}
