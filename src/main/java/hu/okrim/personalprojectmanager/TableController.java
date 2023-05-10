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
    private final ArrayList<Integer> KEYINDEXES = new ArrayList<>();
    private String selectedTable;
    List<String> columnOrder;
    public void initTable() {
        // Getting everything from the currently selected table
        String sqlSelect = "SELECT * FROM " + selectedTable;
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
                ManagerController.showErrorDialog(SQLE.getMessage());
            }
        } catch (Exception e) {
            ManagerController.showErrorDialog(e.getMessage());
        }
    }
    public void insertRow() {
        ArrayList<String> dataToInsert = new ArrayList<>();
        // Only let us insert if the table is not re-ordered
        if (table.getSortOrder().isEmpty()) {
            // We know that if the table is unsorted, then the last row is the row of input
            // So we get the data of the last row
            int lastIndex = table.getItems().size() - 1;
            TableRowData data = table.getItems().get(lastIndex);
            for(int i = 0; i < table.getColumns().size(); i++){
                dataToInsert.add(data.getColumnData(i).get());
            }
            try (Connection connection =
                         ConnectionController.establishConnection(ManagerController.currentConnectionURL)){
                // Building dynamic-SQL query for the INSERT statement
                StringBuilder insertSQL =
                        new StringBuilder("INSERT INTO " + selectedTable + " (");
                for(String s : COLUMNNAMES){
                    insertSQL.append("[");
                    insertSQL.append(s);
                    insertSQL.append("],");
                }
                // Remove the last colon
                insertSQL.deleteCharAt(insertSQL.length() - 1);
                // Add closing bracket
                insertSQL.append(") VALUES (");
                for(String s : dataToInsert){
                    insertSQL.append("'");
                    insertSQL.append(s);
                    insertSQL.append("',");
                }
                // Remove the last colon
                insertSQL.deleteCharAt(insertSQL.length() - 1);
                // Add closing bracket
                insertSQL.append(")");
                // Executing SQL INSERT statement
                Statement statement = connection.createStatement();
                statement.execute(insertSQL.toString());
                // Show a success message dialog upon successfull insertion
                ManagerController.showHelpDialog("INSERT successful", "You have successfully " +
                            "inserted into the " + selectedTable + " table.");
                // Re-initializate the table to add another input field, etc.
                initTable();
            }catch (Exception ex){
                ManagerController.showErrorDialog(ex.getMessage());
            }
        } else {
            ManagerController.showErrorDialog("You cannot insert until the table is custom " +
                    "sorted! Please reset the sorting of the columns!");
        }
    }
    public void saveTableInfo() throws SQLException {
        String sqlColumns = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS\n" +
                "WHERE TABLE_NAME = '" + selectedTable + "'";
        String sqlKeys =    "SELECT COLUMN_NAME\n" +
                            "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE\n" +
                            "WHERE TABLE_NAME = '" + selectedTable +"'\n" +
                            "AND CONSTRAINT_NAME LIKE '%PK%' \n" +
                            "OR TABLE_NAME = '" + selectedTable + "' AND \n" +
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
        } catch (Exception e) {
            ManagerController.showErrorDialog("ERROR: Unknown error occurred. :(");
        }
    }
    private void getIndexColumnIndex(ArrayList<String> columnNames) {
        ObservableList<TableColumn<TableRowData, ?>> columns = table.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            TableColumn<?, ?> column = columns.get(i);
            if (columnNames.contains(column.getText())) {
                KEYINDEXES.add(i);
            }
        }
        System.out.println("KEYS:" + KEYINDEXES);
    }
    private List<String> getTableColumnOrder(){
        ObservableList<TableColumn<TableRowData, ?>> columns = table.getColumns();
        List<String> columnOrder = new ArrayList<>();
        for (TableColumn<TableRowData, ?> column : columns) {
            String columnName = column.getText();
            columnOrder.add(columnName);
        }
        return columnOrder;
    }
    private boolean checkColumnOrderMatch(){
        boolean columnsMatch = true;
        List<String> initialColumns = columnOrder;
        List<String> currentColumns = getTableColumnOrder();
        for(int i = 0; i < initialColumns.size(); i++){
            if(!initialColumns.get(i).equals(currentColumns.get(i))){
                columnsMatch = false;
                break;
            }
        }
        return columnsMatch;
    }
    public void deleteRow(){
        if(!checkColumnOrderMatch()){
            ManagerController.showErrorDialog("Cannot delete if the columns of the table have " +
                    "been rearranged!");
        }else{
            //DELETE FUNCTIONALITY
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setting the table in case multiple instances of the editor will be opened
        // So that each instance knows what table it is editing
        selectedTable = ManagerController.selectedTable;
        // Initialize table
        initTable();
        // Saving the order of columns
        columnOrder = getTableColumnOrder();
    }
}
