import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngine {

    private final ArrayList<particle> particles = new ArrayList<>();
    private final ArrayList<ArrayList<particle>> ArrList = new ArrayList<>();
    private final ConcurrentHashMap<Point, ArrayList<particle>> particleMap = new ConcurrentHashMap<>();
    int numParticles;
    double x, y;
    AtomicInteger nextListIndex = new AtomicInteger(0);
    AtomicInteger count = new AtomicInteger(0);

    private final ExecutorService executor = Executors.newFixedThreadPool(8);

    public particleEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.x = x;
        this.y = y;
    }

    // Paint particles to the screen
    public void paint(GraphicsContext g2d) {
        for (particle particle : particles) {
            particle.draw(g2d);
        }
    }

    public void paintParallel(GraphicsContext gc) {
        for (ArrayList<particle> particles : ArrList) {
            for (particle particle : particles) {
                particle.draw(gc);
            }
        }
    }

    public void updateParticles(){
        particleEngineUtil.updateParticles(particleMap, particles, numParticles, count);
    }

    public void updateParticlesParallel() {
        ArrayList<Callable<Void>> tasks = new ArrayList<>();

        for (ArrayList<particle> partition : ArrList) {
            tasks.add(() -> {
                particleEngineUtil.updateParticles(particleMap, partition, numParticles, count);
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public void addParticles(double x, double y, int maxX, int maxY){
        particleEngineUtil.addParticles(x, y, maxX, maxY, particles, count);
    }

    public void addParticlesParallel(double x, double y, int maxX, int maxY){
        particleEngineUtil.addParticlesParallel(x, y, maxX, maxY, count, ArrList, nextListIndex);
    }

    public void setBounds(int width, int height){
        particleEngineUtil.setBounds(width, height, particles);
    }


    public void resetEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        count.set(0);
        this.x = x;
        this.y = y;
    }
}
