package hu.okrim.personalprojectmanager;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class TableController implements Initializable {
    @FXML
    public Button btnDelete;
    @FXML
    public Button btnUpdate;
    @FXML
    public Button btnInsert;
    @FXML
    TableView<TableRowData> table;

    private final ArrayList<String> COLUMNNAMES = new ArrayList<>();
    private final ArrayList<String> KEYS = new ArrayList<>();

    public void initTable() {
        // Getting the name of the columns in the current table
        String sqlString = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH\n" +
                "FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE TABLE_NAME = '" + ManagerController.selectedTable + "'";
        // Getting everything from the currently selected table
        String sqlSelect = "SELECT * FROM " + ManagerController.selectedTable;
        try (Connection connection = ConnectionController.establishConnection(ManagerController.currentConnectionURL)) {
            try {
                // Running query to get the column information from INFORMATION_SCHEMA
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sqlSelect);

                // Get the ResultSet metadata to retrieve column information
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Create a list to hold the rows of data
                List<TableRowData> rows = new ArrayList<>();

                // Populate the list with data from the ResultSet
                while (resultSet.next()) {
                    TableRowData row = new TableRowData();
                    // Adding one row for each row of data
                    for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                        row.addColumnData(resultSet.getString(columnIndex));
                    }
                    rows.add(row);
                }
                // Manually adding one empty line to the rows for edit placeholder
                TableRowData row = new TableRowData();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    row.addColumnData("");
                }
                rows.add(row);
                // Set up the TableView with the data
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    // For each column we retrieve the column name and the data it contains
                    TableColumn<TableRowData, String> column = new TableColumn<>(metaData.getColumnName(columnIndex + 1));
                    final int colIndex = columnIndex;
                    column.setCellValueFactory(cellData -> cellData.getValue().getColumnData(colIndex));
                    // Setting the width so that columns span all available space
                    column.prefWidthProperty().bind(table.widthProperty().divide(columnCount));
                    table.getColumns().add(column);
                    // Setting a cell factory for each column to handle edits
                    column.setCellFactory(TextFieldTableCell.forTableColumn());
                    // Reading iteration variable for lambda expression
                    int finalColumnIndex = columnIndex;
                    column.setOnEditCommit(event -> {
                        // Get the new value entered by the user
                        String newValue = event.getNewValue();
                        // Get the index of the edited row
                        int rowIndex = event.getTablePosition().getRow();
                        // Get the corresponding row data
                        TableRowData rowData = table.getItems().get(rowIndex);
                        // Update the value in the row data
                        rowData.setColumnData(finalColumnIndex, newValue);
                    });
                }
                // Create an ObservableList from the rows and set it as the items of the TableView
                ObservableList<TableRowData> tableData = FXCollections.observableArrayList(rows);
                table.setItems(tableData);

                // Queuing the table scroll once the table has loaded
                Platform.runLater(() -> {
                    // Scroll to the last row
                    int lastIndex = table.getItems().size() - 1;
                    table.scrollTo(lastIndex);
                    // Select the last row so it's highlighted
                    table.getSelectionModel().select(lastIndex);
                });

                // Saving the currently loaded table's information
                saveTableInfo();

            } catch (SQLTimeoutException SQLTOE) {
                ManagerController.showErrorDialog("ERROR: Your request to load the database tables has timed out.");
            } catch (SQLException SQLE) {
                ManagerController.showErrorDialog("ERROR: Loading the database tables was unsuccessful. :(");
            }
        } catch (Exception e) {
            ManagerController.showErrorDialog("ERROR: Unknown error occurred. :(");
        }
    }

    public void insertRow(){
        // Only let us insert if the table is not re-ordered
        if (table.getSortOrder().isEmpty()) {
            // We know that if the table is unsorted, then the last row is the row of input
            // So we get the data of the last row
            int lastIndex = table.getItems().size() - 1;
            TableRowData data = table.getItems().get(lastIndex);
            for(int i = 0; i < table.getColumns().size(); i++){
                System.out.println(data.getColumnData(i).toString());
            }
        } else {
            ManagerController.showErrorDialog("You cannot insert until the table is custom " +
                    "sorted! Please reset the sorting of the columns!");
        }
    }
    public void saveTableInfo() throws SQLException {
        String sqlColumns = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE TABLE_NAME = '" + ManagerController.selectedTable + "'";
        String sqlKeys =    "SELECT COLUMN_NAME\n" +
                            "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE\n" +
                            "WHERE TABLE_NAME = '" + ManagerController.selectedTable +"'\n" +
                            "AND CONSTRAINT_NAME LIKE '%PK%' \n" +
                            "OR TABLE_NAME = '" + ManagerController.selectedTable + "' AND \n" +
                            "CONSTRAINT_NAME LIKE '%PRIMARY%'";
        try (Connection connection = ConnectionController.establishConnection(ManagerController.currentConnectionURL)) {

            // Saving the column information
            // Running query to get the column information from INFORMATION_SCHEMA
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlColumns);

            // Get the ResultSet metadata to retrieve column information
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Saving column information
            while (resultSet.next()) {
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    COLUMNNAMES.add(resultSet.getString(columnIndex));
                }
            }

            // Saving the keys
            // Running query to get the column information from INFORMATION_SCHEMA
            Statement statementKeys = connection.createStatement();
            ResultSet resultSetKeys = statementKeys.executeQuery(sqlKeys);

            // Get the ResultSet metadata to retrieve column information
            ResultSetMetaData metaDataKeys = resultSetKeys.getMetaData();
            int columnCountKeys = metaDataKeys.getColumnCount();

            // Saving column information
            while (resultSetKeys.next()) {
                for (int columnIndex = 1; columnIndex <= columnCountKeys; columnIndex++) {
                    KEYS.add(resultSetKeys.getString(columnIndex));
                }
            }

            System.out.println(COLUMNNAMES);
            System.out.println(KEYS);

        } catch (Exception e) {
            ManagerController.showErrorDialog("ERROR: Unknown error occurred. :(");
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTable();
    }
}
