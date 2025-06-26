import java.awt.Point;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngineUtil {
    private static final double cellSize = 50;

    private static final Object resetLock = new Object();

    public static void ResetParticle(particle p, AtomicInteger count, int numParticles) {
        synchronized (resetLock) {
            if (count.get() < numParticles) {
                p.reset();
                count.incrementAndGet();
            }
        }
    }

    // Detect if two particles are colliding
    public static boolean isColliding(particle p1, particle p2) {
        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);
        double distanceSquared = dx * dx + dy * dy;
        double radius = cellSize / 4;
        return distanceSquared < radius * radius;
    }

    // Update particles and manage their state
    public static void updateParticles(Map<Point, List<particle>> map, ArrayList<particle> particles, int numParticles, AtomicInteger count) {

        for (particle particle : particles) {
            if (!particle.isAlive()) {
                particleEngineUtil.ResetParticle(particle, count, numParticles);
            } else {
                particle.movement();
                if(particle.age  > 0.8){
                    Point cell = getCell(particle.x, particle.y);
                    map.computeIfAbsent(cell, _ -> new ArrayList<>()).add(particle);
                    handleCollisions(particle, map);
                }

            }
        }
    }

    public static void updateParticlesParallel(ConcurrentHashMap<Point, List<particle>> map, ArrayList<particle> particles, int numParticles, AtomicInteger count) {

        for (particle p : particles) {
            if (!p.isAlive()) {
                particleEngineUtil.ResetParticle(p, count, numParticles);
            } else {
                p.mergedThisFrame = false;
                p.movement();
                if (p.age > 0.8) {
                    Point cell = getCell(p.x, p.y);
                    map.computeIfAbsent(cell, _ -> Collections.synchronizedList(new ArrayList<>())).add(p);
                    handleCollisions(p, map);
                }
            }



        }
    }

    /*
    private static void addToMap(ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> map, particle p) {
        Point cell = getCell(p.x, p.y);
        map.putIfAbsent(cell, new ConcurrentLinkedQueue<>());
        map.get(cell).add(p);
    }*/

    // Handle collisions between particles
    private static void handleCollisions(particle p, Map<Point, List<particle>> map) {
        if(p.mergedThisFrame) return;

        Point cell = particleEngineUtil.getCell(p.x, p.y);

        // Check the current cell and neighboring cells
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Point neighborCell = new Point(cell.x + dx, cell.y + dy);

                if (map.containsKey(neighborCell)) {
                    for (particle other : map.get(neighborCell)) {
                        if (p != other && particleEngineUtil.isColliding(p, other)) {
                            particleEngineUtil.mergeParticles(p, other);
                            p.mergedThisFrame = true;
                            other.mergedThisFrame = true;
                            return;
                        }
                    }
                }
            }
        }
    }

    // Get the cell for a particle based on position
    public static Point getCell(double x, double y) {
        return new Point((int) (x / cellSize), (int) (y / cellSize));
    }

    // Merge two particles when they collide
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

    public static void addParticlesParallel(
            double x, double y, int maxX, int maxY,
            AtomicInteger count,
            ArrayList<ArrayList<particle>> ArrList,
            AtomicInteger nextListIndex, int batchSize, AtomicInteger localCounter, int addCount
    ) {
        if (ArrList.isEmpty()) {
            makeArrList(ArrList);
        }

        int currentIndex = nextListIndex.get() % ArrList.size();

        for(int i = 0; i < addCount; i++) {
            ArrList.get(currentIndex).add(new particle(x, y, maxX, maxY));
        }

        count.addAndGet(addCount);

        int counter = localCounter.addAndGet(addCount);

        if(counter >= batchSize) {
            nextListIndex.updateAndGet(i -> (i + 1) % ArrList.size());
            localCounter.set(0);
        }
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


    public static void setBounds(int width, int height, ArrayList<particle> particles) {
        for (particle p : particles) {
            p.setBounds(width, height);
        }
    }
}
