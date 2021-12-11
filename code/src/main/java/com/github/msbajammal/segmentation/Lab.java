package com.github.msbajammal.segmentation;

import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Lab extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Segmenter");
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        //set Stage boundaries to visible bounds of the main screen
        primaryStage.setX(screenBounds.getMinX());
        primaryStage.setY(screenBounds.getMinY());
        primaryStage.setWidth(screenBounds.getWidth());
        primaryStage.setHeight(screenBounds.getHeight());

        GridPane rootGrid = new GridPane();
        rootGrid.setHgap(20);
        rootGrid.setVgap(20);
        rootGrid.setMinWidth(screenBounds.getWidth()-20);
        rootGrid.setPrefWidth(screenBounds.getWidth());


        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(event -> System.out.println("Hello World!"));

        WebView wv = new WebView();
        WebEngine we = wv.getEngine();
        we.load("https://www.google.com");

        rootGrid.add(btn, 0, 0, 2, 1);
        rootGrid.add(new Label("Left area"), 0, 1, 1, 1);
        rootGrid.add(wv, 1, 1, 1, 1);

        Scene scn = new Scene(rootGrid);
        primaryStage.setScene(scn);
        primaryStage.show();
    }
}