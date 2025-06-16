import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class simulationPanel extends Pane {
    /*
    private particleEngine engine;
    private Canvas canvas;
    private AnimationTimer timer;

    public simulationPanel(particleEngine engine) {
        this.engine = engine;

        canvas = new Canvas(800, 600); // Or bind to size
        this.getChildren().add(canvas);
        this.setStyle("-fx-background-color: black;");

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                engine.updateParticles();
                draw();
            }
        };
        timer.start();

        // Optional: Bind canvas size to parent size
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());
    }

    private void draw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        engine.paint(gc); // You must update particleEngine's paint() to use GraphicsContext
    }*/
}
