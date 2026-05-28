package com.diskmapper;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainController controller = new MainController(primaryStage);
        Scene scene = new Scene(controller.getRoot(), 900, 650);
        primaryStage.setTitle("DiskMapper - ディスク容量可視化");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(300);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
