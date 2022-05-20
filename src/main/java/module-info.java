module com.dow.spaceclearer {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.dow.spaceclearer to javafx.fxml;
    exports com.dow.spaceclearer;
}