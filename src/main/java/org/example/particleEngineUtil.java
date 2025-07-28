package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngineUtil {
    private static final double cellSize = 50;

    record CellKey(double x, double y) {
        public int hashCode() { return (int) (31 * x + y); }
    }

    // Detect if two particles are colliding
    public static boolean isColliding(particle p1, particle p2) {
        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);
        double distanceSquared = dx * dx + dy * dy;
        double radius = cellSize / 4;
        return distanceSquared < radius * radius;
    }

    public static void updateParticles(Map<Point, ConcurrentLinkedQueue<particle>> map, ArrayList<particle> particles, int numParticles, AtomicInteger count) {
        int c = 0;
        for (particle p : particles) {
            if (!p.isAlive()) {
                if (count.get() < numParticles) {
                    p.reset();
                    c++;
                }
            } else {
                p.movement();
                if(p.age  > 0.8){
                    Point cell = getCell(p.x, p.y);
                    map.computeIfAbsent(cell, x -> new ConcurrentLinkedQueue<>()).add(p);
                    handleCollisions(p, map);
                }
            }
            count.addAndGet(c);
        }
    }

    public static void updateParticlesParallel(ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> map, ArrayList<particle> particles, int numParticles, AtomicInteger count) {
        int c = 0;
        for (particle p : particles) {
            if (!p.isAlive()) {
                if (count.get() < numParticles) {
                    p.reset();
                    c++;
                }
            } else {
                p.mergedThisFrame = false;
                p.movement();
                if (p.age > 0.8) {
                    Point cell = getCell(p.x, p.y);
                    map.computeIfAbsent(cell, x -> new ConcurrentLinkedQueue<>()).add(p);
                    handleCollisions(p, map);
                }
            }
            count.addAndGet(c);
        }
    }

    public static void updateParticlesDistributed(Map<Point, ConcurrentLinkedQueue<particle>> map, ArrayList<particle> particles) {
        for (particle p : particles) {
            p.mergedThisFrame = false;
            p.movement();
            if (p.age > 0.8) {
                Point cell = getCell(p.x, p.y);
                map.computeIfAbsent(cell, x -> new ConcurrentLinkedQueue<>()).add(p);
                handleCollisions(p, map);
            }
        }
    }

    private static void handleCollisions(particle p, Map<Point, ConcurrentLinkedQueue<particle>> map) {
        if(p.mergedThisFrame) return;

        CellKey cell = new CellKey(p.x, p.y);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                CellKey neighbor = new CellKey(cell.x() + dx, cell.y() + dy);

                var others = map.get(neighbor);
                if (others == null) continue;

                for (particle other : others) {
                    if (p != other && !other.mergedThisFrame &&
                            particleEngineUtil.isColliding(p, other)) {
                        particleEngineUtil.mergeParticles(p, other);
                        p.mergedThisFrame = true;
                        other.mergedThisFrame = true;
                        return;
                    }
                }
            }
        }
    }

    public static Point getCell(double x, double y) {
        return new Point((int) (x / cellSize), (int) (y / cellSize));
    }

    public static void mergeParticles(particle p1, particle p2) {
        double avgX = (p1.x + p2.x) / 2;
        p1.x += (avgX - p1.x) / 10;
        p2.x += (avgX - p2.x) / 10;

        double avgVy = (p1.vy + p2.vy) / 2;
        p1.tempVy = avgVy;
        p2.tempVy = avgVy;
    }

    public static void addParticles(double x, double y, int maxX, int maxY, ArrayList<particle> particles, AtomicInteger count, int addCount) {
        for(int i = 0; i < addCount; i++) {
            particles.add(new particle(x, y, maxX, maxY));
        }
        count.addAndGet(addCount);
    }

    public static int addParticlesParallel(
            double x, double y, int maxX, int maxY,
            AtomicInteger count,
            ArrayList<ArrayList<particle>> ArrList,
            AtomicInteger nextListIndex, int batchSize, int localCounter, int addCount
    ) {
        if (ArrList.isEmpty()) {
            makeArrList(ArrList);
        }

        int currentIndex = nextListIndex.get() % ArrList.size();

        for(int i = 0; i < addCount; i++) {
            ArrList.get(currentIndex).add(new particle(x, y, maxX, maxY));
        }

        count.addAndGet(addCount);

        localCounter += addCount;

        if(localCounter >= batchSize) {
            nextListIndex.updateAndGet(i -> (i + 1) % ArrList.size());
            localCounter = 0;
        }
        return localCounter;
    }

    public static void addParticlesParallelBurst(
            double x, double y, int maxX, int maxY,
            AtomicInteger count,
            ArrayList<ArrayList<particle>> ArrList,
            ExecutorService executor,
            int addCount
    ) {
        if (ArrList.isEmpty()) {
            makeArrList(ArrList);
        }

        int numThreads = ArrList.size();
        int particlesPerThread = addCount / numThreads;
        int remaining = addCount % numThreads;

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            int taskIndex = i;
            int taskParticleCount = particlesPerThread + (i < remaining ? 1 : 0);

            tasks.add(() -> {

                List<particle> localParticles = new ArrayList<>(taskParticleCount);
                for (int j = 0; j < taskParticleCount; j++) {
                    localParticles.add(new particle(x, y, maxX, maxY));
                }
                ArrList.get(taskIndex).addAll(localParticles);
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
            count.addAndGet(addCount);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public static void makeArrList(ArrayList<ArrayList<particle>> ArrList) {
        for (int i = 0; i < 7; i++){
            ArrList.add(new ArrayList<>());
        }
    }

    public static void makeArrListDistributed(ArrayList<ArrayList<particle>> ArrList){
        for (int i = 0; i < 4; i++){
            ArrList.add(new ArrayList<>());
        }
    }


    public static void setBounds(int width, int height, ArrayList<particle> particles) {
        for (particle p : particles) {
            p.setBounds(width, height);
        }
    }

    public static void addParticlesDistributed(
            double x, double y, int maxX, int maxY,
            ArrayList<ArrayList<particle>> ArrList,
            int addCount
    ) {
        if (ArrList.isEmpty()) {
            makeArrListDistributed(ArrList);
        }

        for (int i = 0; i < addCount; i++) {
            ArrList.getFirst().add(new particle(x, y, maxX, maxY));
        }
    }
}
