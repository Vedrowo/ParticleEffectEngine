package org.example;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import java.io.IOException;

public class BuildScene {

    public Label runtime;

    private particleEngine engine;

    public Button startButton;
    @FXML
    private ChoiceBox<String> myChoiceBox;
    @FXML
    private ChoiceBox<String> myChoiceBox1;
    @FXML
    private Canvas drawingCanvas;
    @FXML
    private AnchorPane canvas;

    private Stage stage;



    public void bindWindowSizeToFields(Stage stage) {
        this.stage = stage;

        stage.widthProperty().addListener((a, b, newVal) -> windowWidthField.setText(String.valueOf(newVal.intValue())));

        stage.heightProperty().addListener((a, b, newVal) -> windowHeightField.setText(String.valueOf(newVal.intValue())));

        windowWidthField.setText(String.valueOf((int) stage.getWidth()));
        windowHeightField.setText(String.valueOf((int) stage.getHeight()));
    }

    @FXML
    public void initialize() {
        myChoiceBox.getItems().addAll("Over time", "Burst");
        myChoiceBox.setValue("Over time"); // default selected item
        myChoiceBox1.getItems().addAll("Sequential", "Parallel", "Distributed");
        myChoiceBox1.setValue("Sequential"); // default selected item
    }

    @FXML private TextField particleCountField, windowWidthField, windowHeightField, xField, yField;

    private int particlesAdded = 0;
    private int numParticles;

    private AnimationTimer renderLoop;


    @FXML
    private void startSimulation() {
        try {
            long startTime = System.nanoTime();

            numParticles = Integer.parseInt(particleCountField.getText());
            int width = Integer.parseInt(windowWidthField.getText());
            int height = Integer.parseInt(windowHeightField.getText());
            int x = Integer.parseInt(xField.getText());
            int y = Integer.parseInt(yField.getText());
            String mode = myChoiceBox.getValue();
            String concurrencyType = myChoiceBox1.getValue();

            canvas.setPrefWidth(width);
            canvas.setPrefHeight(height);
            stage.setWidth(width);
            stage.setHeight(height);

            resizeCanvas(width-215, height-275);

            if (engine == null) {
                engine = new particleEngine(x, y, numParticles);
            } else {
                engine.resetEngine(x, y, numParticles);
            }

            engine.setBounds(width, height);

            // entire distributed logic dumped here
            if (concurrencyType.equalsIgnoreCase("Distributed")) {
                int workerCount = 4;
                StringBuilder results = new StringBuilder();

                if(mode.equalsIgnoreCase("Burst")){
                    engine.addParticlesDistributed(x, y, width-215, height, numParticles);
                } else {
                    particlesAdded = 0;
                    AnimationTimer particleAdder;

                    particleAdder = new AnimationTimer() {
                        @Override
                        public void handle(long now) {
                            int addCount = 10;
                            if(particlesAdded < numParticles) {
                                engine.addParticlesDistributed(x, y, width-215, height, addCount);
                                particlesAdded+=addCount;
                            }
                            if (particlesAdded >= numParticles) stop();
                        }
                    };
                    particleAdder.start();
                }

                for (int rank = 0; rank < workerCount; rank++) {
                    int port = 5000 + rank;
                    try (java.net.Socket socket = new java.net.Socket("localhost", port);
                         java.io.BufferedWriter out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(socket.getOutputStream()));
                         java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))) {

                        int rangeSize = height / workerCount;
                        int rangeStart = rank * rangeSize;
                        int rangeEnd = rangeStart + rangeSize;

                        String message = "Size of worker " + rank + " is " + rangeStart + " - " + rangeEnd;
                        out.write(message);
                        out.newLine();
                        out.flush();

                        String reply = in.readLine();
                        results.append("Worker ").append(rank).append(" replied: ").append(reply).append("\n");

                    } catch (IOException ex) {
                        results.append("Failed to connect to worker ").append(rank).append(" on port ").append(port).append("\n");
                    }
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Distributed Communication Test");
                alert.setHeaderText("Worker Responses:");
                alert.setContentText(results.toString());
                alert.show();

                return;
            }


            if (mode.equalsIgnoreCase("Burst")) {
                if(concurrencyType.equalsIgnoreCase("Sequential")) {
                    engine.addParticles(x, y, width-215, height, numParticles);
                }
                else if (concurrencyType.equalsIgnoreCase("Parallel")) {
                    engine.addParticlesParallelBurst(x, y, width-215, height, numParticles);
                }

            } else {
                particlesAdded = 0;
                AnimationTimer particleAdder;

                if (concurrencyType.equals("Sequential")) {
                    particleAdder = new AnimationTimer() {
                        @Override
                        public void handle(long now) {
                            int addCount = 10;
                            if(particlesAdded < numParticles) {
                                engine.addParticles(x, y, width-215, height, addCount);
                                particlesAdded+=addCount;
                            }
                            if (particlesAdded >= numParticles) stop();
                        }
                    };
                    particleAdder.start();

                } else if (concurrencyType.equals("Parallel")) {
                    particleAdder = new AnimationTimer() {
                        @Override
                        public void handle(long now) {
                            int addCount = 10;
                            if(particlesAdded < numParticles) {
                                engine.addParticlesParallel(x, y, width-215, height, addCount);
                                particlesAdded+=addCount;
                            }
                            if (particlesAdded >= numParticles) stop();
                        }
                    };
                    particleAdder.start();
                }
            }

            if (renderLoop != null) {
                renderLoop.stop();
                renderLoop = null;
            }

            GraphicsContext gc = drawingCanvas.getGraphicsContext2D();

            if(concurrencyType.equalsIgnoreCase("Sequential")) {
                renderLoop = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        clearCanvas(gc);
                        boolean drawn = engine.updateParticles();

                        if (!drawn) {
                            long end = System.nanoTime();
                            long elapsedNanos = end - startTime;
                            runtime.setText("Run time: " + elapsedNanos/1_000_000 + " ms");
                            renderLoop.stop();
                        }

                        engine.paint(gc, width-215, height-275);
                    }
                };
                renderLoop.start();

            } else if (concurrencyType.equalsIgnoreCase("Parallel")) {
                renderLoop = new AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        clearCanvas(gc);
                        boolean drawn = engine.updateParticlesParallel();

                        if (!drawn) {
                            long end = System.nanoTime();
                            long elapsedNanos = end - startTime;
                            runtime.setText("Run time: " + elapsedNanos/1_000_000 + " ms");
                            renderLoop.stop();
                        }
                        
                        engine.paintParallel(gc, width-215, height-275);
                    }
                };
                renderLoop.start();
            }

        } catch (NumberFormatException e) {
            showAlert();
        }
    }

    private void clearCanvas(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void resizeCanvas(double width, double height) {
        drawingCanvas.setWidth(width);
        drawingCanvas.setHeight(height);
    }

    private void showAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText("Please enter valid numbers.");
        alert.show();
    }
}
