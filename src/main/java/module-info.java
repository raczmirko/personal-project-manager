module hu.okrim.personalprojectmanager {
    requires javafx.controls;
    requires javafx.fxml;


    opens hu.okrim.personalprojectmanager to javafx.fxml;
    exports hu.okrim.personalprojectmanager;
}