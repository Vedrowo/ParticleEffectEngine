package org.example;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
        myChoiceBox1.getItems().addAll("Sequential", "Parallel");
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
                } else {
                    //prep for distributed
                    particleAdder = new AnimationTimer() {
                        @Override
                        public void handle(long now) {

                        }
                    };
                }

                particleAdder.start();
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
            }

            renderLoop.start();

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
