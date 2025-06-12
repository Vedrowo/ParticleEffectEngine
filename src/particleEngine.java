import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class particleEngine {

    private final CopyOnWriteArrayList<particle> particles = new CopyOnWriteArrayList<>();
    private final HashMap<Point, ArrayList<particle>> particleMap = new HashMap<>();
    private final double cellSize = 50;
    int count = 0;
    int numParticles;
    double x, y;


    public particleEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.x = x;
        this.y = y;
    }

    // Update particles and manage their state
    public void updateParticles() {
        particleMap.clear();

        for (particle particle : particles) {
            addToMap(particle);

            if (!particle.isAlive() && numParticles > count) {
                particle.reset();
                particles.remove(particle);
                particles.add(particle);
                count++;
            }
            if(!particle.isAlive()) {
                particles.remove(particle);
            }

            if (particle.age > 0.6 && particle.startingY - particle.y > 400) {
                handleCollisions(particle);
            }

            particle.movement();

        }
    }

    public void resetEngine(double x, double y, int numParticles) {
        this.numParticles = numParticles;
        this.count = 0;
        this.x = x;
        this.y = y;
    }


    // Add particle to map based on cell position
    private void addToMap(particle p) {
        Point cell = getCell(p.x, p.y);
        particleMap.putIfAbsent(cell, new ArrayList<>());
        particleMap.get(cell).add(p);
    }

    // Get the cell for a particle based on position
    private Point getCell(double x, double y) {
        return new Point((int) (x / cellSize), (int) (y / cellSize));
    }

    // Handle collisions between particles
    private void handleCollisions(particle p) {
        Point cell = getCell(p.x, p.y);

        // Check the current cell and neighboring cells
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                Point neighborCell = new Point(cell.x + dx, cell.y + dy);

                if (particleMap.containsKey(neighborCell)) {
                    for (particle other : particleMap.get(neighborCell)) {
                        if (p != other && isColliding(p, other)) {
                            mergeParticles(p, other);
                        }
                    }
                }
            }
        }
    }

    // Detect if two particles are colliding
    private boolean isColliding(particle p1, particle p2) {
        double dx = Math.abs(p1.x - p2.x);
        double dy = Math.abs(p1.y - p2.y);
        double distanceSquared = Math.sqrt(dx * dx + dy * dy);
        double radius = cellSize / 4;
        return distanceSquared < radius;
    }

    // Merge two particles when they collide
    private void mergeParticles(particle p1, particle p2) {
        p1.x = p1.x + (p2.x - p1.x) / 10;
        p2.x = p2.x + (p1.x - p2.x) / 10;

        if (p1.vy < p2.vy) {
            p2.tempVy = (p2.vy + p1.vy) / 2;
        } else {
            p1.tempVy = (p1.vy + p2.vy) / 2;
        }
    }

    // Paint particles to the screen
    public void paint(GraphicsContext g2d) {
        for (particle particle : particles) {
            particle.draw(g2d);
        }
    }

    // Add a new particle
    public void addParticles(double x, double y, int maxX, int maxY) {
        particles.add(new particle(x, y, maxX, maxY));
        count++;
    }

    public void setBounds(int width, int height) {

        for (particle p : particles) {
            p.setBounds(width, height);
        }
    }

    public boolean done(){
        return count == numParticles;
    }
}
