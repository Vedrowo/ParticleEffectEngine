package org.example;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.IOException;

public class MyStage {
    public static void setup(Stage stage) throws IOException {
        stage.setWidth(800);
        stage.setHeight(600);

        FXMLLoader loader = new FXMLLoader(MyStage.class.getResource("/buildScene.fxml"));
        AnchorPane rootFXML = loader.load();

        BuildScene controller = loader.getController();

        Scene scene = new Scene(rootFXML, 800, 600);

        controller.bindWindowSizeToFields(stage);

        Image icon = new Image("/icon.png");
        stage.getIcons().add(icon);

        Line line = new Line();
        line.setStartX(0);
        line.setStartY(70);
        line.setEndY(70);
        line.setStrokeWidth(3);
        line.setStroke(Color.WHITE);
        line.endXProperty().bind(scene.widthProperty());

        rootFXML.getChildren().add(line);

        stage.setScene(scene);
        stage.setTitle("Particle Effect Engine");
        stage.show();
    }
}
