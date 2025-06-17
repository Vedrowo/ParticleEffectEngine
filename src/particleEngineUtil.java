import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class particleEngineUtil {
    private static final double cellSize = 50;

    // Detect if two particles are colliding
    public static boolean isColliding(particle p1, particle p2) {
        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);
        double distanceSquared = dx * dx + dy * dy;
        double radius = cellSize / 4;
        return distanceSquared < radius * radius;
    }

    // Update particles and manage their state
    public static void updateParticles(ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> map, ConcurrentLinkedQueue<particle> particles, int numParticles, AtomicInteger count) {
        map.clear();

        Iterator<particle> iterator = particles.iterator();

        while (iterator.hasNext()) {
            particle particle = iterator.next();
            //addToMap(map, particle);

            if (!particle.isAlive() && numParticles > count.get()) {
                particle.reset();
                count.incrementAndGet();
            }
            if(!particle.isAlive()) {
                iterator.remove();
                continue;
            }

            if (particle.age > 0.8) {
                //handleCollisions(particle, map);
            }

            particle.movement();
        }
    }

    // Add particle to map based on cell position
    private static void addToMap(ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> map, particle p) {
        Point cell = getCell(p.x, p.y);
        map.putIfAbsent(cell, new ConcurrentLinkedQueue<>());
        map.get(cell).add(p);
    }

    // Handle collisions between particles
    private static void handleCollisions(particle p, ConcurrentHashMap<Point, ConcurrentLinkedQueue<particle>> map) {
        Point cell = particleEngineUtil.getCell(p.x, p.y);

        // Check the current cell and neighboring cells
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Point neighborCell = new Point(cell.x + dx, cell.y + dy);

                if (map.containsKey(neighborCell)) {
                    for (particle other : map.get(neighborCell)) {
                        if (p != other && particleEngineUtil.isColliding(p, other)) {
                            particleEngineUtil.mergeParticles(p, other);
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

    // Add a new particle
    public static void addParticles(double x, double y, int maxX, int maxY, ConcurrentLinkedQueue<particle> particles, AtomicInteger count, int addCount) {
        for(int i = 0; i < addCount; i++) {
            particles.add(new particle(x, y, maxX, maxY));
        }
        count.addAndGet(addCount);
    }

    public static void addParticlesParallel(double x, double y, int maxX, int maxY,
                                            AtomicInteger count, ArrayList<ConcurrentLinkedQueue<particle>> ArrList,
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

        int counter = localCounter.incrementAndGet();

        if(counter >= batchSize) {
            nextListIndex.updateAndGet(i -> (i + 1) % ArrList.size());
            localCounter.set(0);
        }
    }

    public static void makeArrList(ArrayList<ConcurrentLinkedQueue<particle>> ArrList) {
        for (int i = 0; i < 7; i++){
            ArrList.add(new ConcurrentLinkedQueue<>());
        }
    }


    public static void setBounds(int width, int height, ConcurrentLinkedQueue<particle> particles) {
        for (particle p : particles) {
            p.setBounds(width, height);
        }
    }
}
