module com.example.networks1project {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.networks1project to javafx.fxml;
    exports com.example.networks1project;
}