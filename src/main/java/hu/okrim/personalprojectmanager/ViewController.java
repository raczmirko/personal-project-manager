package hu.okrim.personalprojectmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ViewController implements Initializable {
    @FXML
    TableView<TableRowData> tableView;
    private String selectedView;
    public void initTable() {
        // Getting everything from the currently selected table
        String sqlSelect = "SELECT * FROM " + selectedView;
        try (Connection connection =
                     ConnectionController.establishConnection(ManagerController.currentConnectionURL)) {
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
                // Set up the TableView with the data
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    // For each column we retrieve the column name and the data it contains
                    TableColumn<TableRowData, String> column = new TableColumn<>(metaData.getColumnName(columnIndex + 1));
                    final int colIndex = columnIndex;
                    column.setCellValueFactory(cellData -> cellData.getValue().getColumnData(colIndex));
                    // Setting the width so that columns span all available space
                    column.prefWidthProperty().bind(tableView.widthProperty().divide(columnCount));
                    tableView.getColumns().add(column);
                }
                // Create an ObservableList from the rows and set it as the items of the TableView
                ObservableList<TableRowData> tableData = FXCollections.observableArrayList(rows);
                tableView.setItems(tableData);
            } catch (SQLTimeoutException SQLTOE) {
                ManagerController.showErrorDialog("ERROR: Your request to load the database tables has timed out.");
            } catch (SQLException SQLE) {
                ManagerController.showErrorDialog(SQLE.getMessage());
            }
        } catch (Exception e) {
            ManagerController.showErrorDialog(e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setting the table in case multiple instances of the editor will be opened
        // So that each instance knows what table it is editing
        selectedView = ManagerController.selectedView;
        // Initialize table
        initTable();
    }
}
