package hu.okrim.personalprojectmanager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;

public class TableRowData {
    private final List<StringProperty> rowData = new ArrayList<>();

    public void addColumnData(String columnData) {
        rowData.add(new SimpleStringProperty(columnData));
    }

    public StringProperty getColumnData(int columnIndex) {
        return rowData.get(columnIndex);
    }

    public List<String> getAllDataFromRow(){
        List<String> rowData = new ArrayList<>();
        for(StringProperty str : this.rowData){
            rowData.add(str.get());
        }
        return rowData;
    }

    public void setColumnData(int columnIndex, String newValue) {
        getColumnData(columnIndex).set(newValue);
    }
}

