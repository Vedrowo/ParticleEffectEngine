package org.example;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiApp extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        MyStage.setup(primaryStage);
        primaryStage.show();

        String classpath = System.getProperty("java.class.path");

        List<Process> workers = new ArrayList<>();

        // spawn em
        for (int i = 0; i < 4; i++) {
            int port = 5000 + i;
            Process p = new ProcessBuilder("java", "-cp", classpath, "org.example.Worker", String.valueOf(port))
                    .inheritIO()
                    .start();
            workers.add(p);
        }

        // kill em
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process p : workers) {
                p.destroy();
            }
        }));

    }
}