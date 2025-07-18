package org.example;

import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngine {

    private final ArrayList<particle> particles = new ArrayList<>();
    private final ArrayList<ArrayList<particle>> ArrList = new ArrayList<>();
    private final Map<Point, ConcurrentLinkedQueue<particle>> particleMap = new HashMap<>();
    int numParticles, localCount = 0;
    double x, y;
    AtomicInteger nextListIndex = new AtomicInteger(0);
    AtomicInteger count = new AtomicInteger(0);

    ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> particleMap2 = new ConcurrentHashMap<>();

    int maxThreads = Runtime.getRuntime().availableProcessors();
    private final ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
    int batchSize;

    public particleEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.x = x;
        this.y = y;
        this.batchSize = numParticles / maxThreads;
    }

    public void paint(GraphicsContext g2d, int width, int height) {

        for (particle particle : particles) {
            if(particle.isAlive()){
                particle.draw(g2d, width, height);
            }
        }
    }

    public void paintParallel(GraphicsContext gc, int width, int height) {

        for (ArrayList<particle> particles : ArrList) {
            for (particle particle : particles) {
                if(particle.isAlive()){
                    particle.draw(gc, width, height);
                }
            }
        }

    }

    public boolean updateParticles(){
        particleMap.clear();
        particleEngineUtil.updateParticles(particleMap, particles, numParticles, count);

        for (particle p : particles) {
            if(p.isAlive()){
                return true;
            }
        }

        return false;
    }

    public boolean updateParticlesParallel() {
        particleMap2.clear();

        ArrayList<Callable<Void>> tasks = new ArrayList<>();

        for (ArrayList<particle> partition : ArrList) {
            tasks.add(() -> {
                particleEngineUtil.updateParticlesParallel(particleMap2, partition, numParticles, count);
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);

            for (ArrayList<particle> partition : ArrList) {
                for (particle p : partition) {
                    if (p.isAlive()) return true;
                }
            }
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    public void addParticles(double x, double y, int maxX, int maxY, int addCount){
        particleEngineUtil.addParticles(x, y, maxX, maxY, particles, count, addCount);
    }

    public void addParticlesParallel(double x, double y, int maxX, int maxY, int addCount){
        localCount = particleEngineUtil.addParticlesParallel(x, y, maxX, maxY, count, ArrList, nextListIndex, batchSize, localCount, addCount);
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
        localCount = 0;
        nextListIndex.set(0);
        particles.clear();
        particleMap.clear();
        for (ArrayList<particle> list : ArrList) {
            list.clear();
        }
    }
}