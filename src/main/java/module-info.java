module hu.okrim.personalprojectmanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens hu.okrim.personalprojectmanager to javafx.fxml;
    exports hu.okrim.personalprojectmanager;
}