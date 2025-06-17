import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngine {

    private final ConcurrentLinkedQueue<particle> particles = new ConcurrentLinkedQueue<>();
    private final ArrayList<ConcurrentLinkedQueue<particle>> ArrList = new ArrayList<>();
    private final ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> particleMap = new ConcurrentHashMap<>();
    int numParticles;
    double x, y;
    AtomicInteger nextListIndex = new AtomicInteger(0);
    AtomicInteger count = new AtomicInteger(0);
    AtomicInteger localCount = new AtomicInteger(0);

    int maxThreads = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
    int batchSize = numParticles / maxThreads;

    public particleEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.x = x;
        this.y = y;
    }

    // Paint particles to the screen
    public boolean paint(GraphicsContext g2d) {
        boolean didDrawSomething = false;

        for (particle particle : particles) {
            particle.draw(g2d);
            didDrawSomething = true;
        }
        return didDrawSomething;
    }

    public boolean paintParallel(GraphicsContext gc) {
        boolean didDrawSomething = false;

        for (ConcurrentLinkedQueue<particle> particles : ArrList) {
            for (particle particle : particles) {
                particle.draw(gc);
                didDrawSomething = true;
            }
        }
        return didDrawSomething;
    }

    public void updateParticles(){
        particleEngineUtil.updateParticles(particleMap, particles, numParticles, count);
    }

    public void updateParticlesParallel() {
        ArrayList<Callable<Void>> tasks = new ArrayList<>();

        for (ConcurrentLinkedQueue<particle> partition : ArrList) {
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

    public void addParticles(double x, double y, int maxX, int maxY, int addCount){
        particleEngineUtil.addParticles(x, y, maxX, maxY, particles, count, addCount);
    }

    public void addParticlesParallel(double x, double y, int maxX, int maxY, int addCount){
        particleEngineUtil.addParticlesParallel(x, y, maxX, maxY, count, ArrList, nextListIndex, batchSize, localCount, addCount);
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