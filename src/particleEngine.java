import javafx.scene.canvas.GraphicsContext;

import java.util.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngine {

    private final ArrayList<particle> particles = new ArrayList<>();
    private final ArrayList<ArrayList<particle>> ArrList = new ArrayList<>();
    private final Map<Point, List<particle>> particleMap = new HashMap<>();
    int numParticles;
    double x, y;
    AtomicInteger nextListIndex = new AtomicInteger(0);
    AtomicInteger count = new AtomicInteger(0);
    AtomicInteger localCount = new AtomicInteger(0);

    int maxThreads = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
    int batchSize;

    public particleEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.x = x;
        this.y = y;
        this.batchSize = numParticles / maxThreads;
    }

    // Paint particles to the screen
    public boolean paint(GraphicsContext g2d) {
        boolean didDrawSomething = false;

        for (particle particle : particles) {
            if(particle.isAlive()){
                particle.draw(g2d);
                didDrawSomething = true;
            }
        }
        return didDrawSomething;
    }

    public boolean paintParallel(GraphicsContext gc) {
        boolean didDrawSomething = false;

        for (ArrayList<particle> particles : ArrList) {
            for (particle particle : particles) {
                if(particle.isAlive()){
                    particle.draw(gc);
                    didDrawSomething = true;
                }
            }
        }
        return didDrawSomething;
    }

    public void updateParticles(){
        particleEngineUtil.updateParticles(particleMap, particles, numParticles, count);
    }

    public void updateParticlesParallel() {

        ArrayList<Callable<Map<Point, List<particle>>>> tasks = new ArrayList<>();

        for (ArrayList<particle> partition : ArrList) {
            tasks.add(() -> particleEngineUtil.updateParticles(particleMap, partition, numParticles, count));
        }

        List<Map<Point, List<particle>>> localMaps = new ArrayList<>();
        try {
            List<Future<Map<Point, List<particle>>>> futures = executor.invokeAll(tasks);

            for (Future<Map<Point, List<particle>>> future : futures) {
                localMaps.add(future.get());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
            return;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return;
        }

        mergeLocalMaps(localMaps);
    }


    public void mergeLocalMaps(List<Map<Point, List<particle>>> localMaps) {
        particleMap.clear();

        for (Map<Point, List<particle>> localMap : localMaps) {
            for (Map.Entry<Point, List<particle>> entry : localMap.entrySet()) {
                Point cell = entry.getKey();
                List<particle> localParticles = entry.getValue();

                particleMap.computeIfAbsent(cell, _ -> new ArrayList<>()).addAll(localParticles);
            }
        }
    }

    public void addParticles(double x, double y, int maxX, int maxY, int addCount){
        particleEngineUtil.addParticles(x, y, maxX, maxY, particles, count, addCount);
    }

    public void addParticlesParallel(double x, double y, int maxX, int maxY, int addCount){
        particleEngineUtil.addParticlesParallel(x, y, maxX, maxY, count, ArrList, nextListIndex, batchSize, localCount, addCount);
    }

    public void addParticlesParallelBurst(double x, double y, int maxX, int maxY, int addCount){
        particleEngineUtil.addParticlesParallelBurst(x, y, maxX, maxY, count, ArrList, executor, addCount);
    }

    public void setBounds(int width, int height){
        particleEngineUtil.setBounds(width, height, particles);
    }

    public void resetEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.x = x;
        this.y = y;
        count.set(0);
        localCount.set(0);
        nextListIndex.set(0);
        particles.clear();
        particleMap.clear();
        for (ArrayList<particle> list : ArrList) {
            list.clear();
        }
    }
}