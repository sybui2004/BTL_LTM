module com.example.memorygame {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    opens com.example.memorygame to javafx.fxml;
    opens com.example.memorygame.controller to javafx.fxml;
    opens com.example.memorygame.controller.room to javafx.fxml;
    opens com.example.memorygame.controller.friend to javafx.fxml;
    opens com.example.memorygame.controller.main to javafx.fxml;
    opens com.example.memorygame.view to javafx.fxml;
    opens com.example.memorygame.model.user to com.fasterxml.jackson.databind;
    opens com.example.memorygame.model.game to com.fasterxml.jackson.databind, javafx.base;
    opens com.example.memorygame.utils to com.fasterxml.jackson.databind;
    exports com.example.memorygame;
    exports com.example.memorygame.model.game;
    exports com.example.memorygame.model.user;
    exports com.example.memorygame.utils to com.fasterxml.jackson.databind;
}