import javafx.scene.canvas.GraphicsContext;

import javafx.scene.paint.Color;

public class particle {

    // Particle properties
    public double x, y, startingX, startingY, startingSize, startingVX, startingVY;
    public double vx, vy, size, lifetime, currentLifetime, age, alpha;
    public Color color;
    public double tempVy = 0;
    public int maxX, maxY;
    private double xOffset, yOffset;
    public boolean mergedThisFrame = false;

    // Constructor to initialize particle
    public particle(double x, double y, int maxX, int maxY) {
        xOffset = (Math.random() - 0.5) * 70;
        yOffset = (Math.random() - 0.5) * 20;
        this.maxX = maxX;
        this.maxY = maxY;
        this.x = x + xOffset;
        this.y = y + yOffset;

        this.currentLifetime = 0;
        this.alpha = 1;
        this.startingX = x;
        this.startingY = y;


        double angle = Math.random() * 2 * Math.PI;
        double speed = 2 + Math.random() * 8;
        double time = System.currentTimeMillis() / 1000.0; // Time in seconds
        double phase = Math.random() * 2 * Math.PI; // Random phase between 0 and 2π

        // velocity range
        double amplitude = 1.5;
        // Oscillations per second
        double frequency = 10.0;
        vx = amplitude * Math.sin(2 * Math.PI * frequency * time + phase);
        vy = 0 - Math.abs(Math.sin(angle) * speed);
        this.startingVX = vx;
        this.startingVY = vy;

        if(vy > -1){
            vy = vy - 1.5;
        }

        this.color = Color.WHITE;

        if (Math.random() < 1.0 / 500) {  // 1 in 200 chance
            this.vx = vx*8;
            this.vy = vy*4;
            this.size = 15;
            this.lifetime = 60;
        }else {
            this.size = 30;
            this.lifetime = 187.5;
        }

        this.startingSize = size;
    }

    public void setBounds(int width, int height) {
        this.maxX = width;
        this.maxY = height;
    }

    // Update particle movement
    public void movement() {
        if (tempVy != 0) {
            vy = tempVy;
        }

        age = Math.max(0, Math.min(1, currentLifetime / lifetime));
        vx *= (1 - 0.01 * age);
        vy *= (1 - 0.01 * age);

        if (y > 1) {
            y += vy;
        }

        if (x > 1 && x < maxX) {
            x += vx;
        }

        currentLifetime++;

        // Adjust size based on age
        size = (age < 0.2) ? size + 0.1 : size - 0.3;
        alpha = 1 * (1 - Math.pow(age, 2));
        color = getColorBasedOnLifetime(currentLifetime, lifetime, alpha);
    }

    // Check if particle is alive
    public boolean isAlive() {
        return currentLifetime < lifetime;
    }

    // Get particle color based on its lifetime
    private Color getColorBasedOnLifetime(double currentLifetime, double lifetime, double alpha) {
        age = Math.max(0, Math.min(1, currentLifetime / lifetime));  // Ensure age is between 0 and 1

        double r = 1.0;
        double g = (1 - 2 * age);
        double b = (1 - 10 * age);

        g = Math.max(0, Math.min(1, g));
        b = Math.max(0, Math.min(1, b));

        return new Color(r, g, b, alpha);
    }

    // Reset particle to its initial state
    public void reset() {
        currentLifetime = 0;
        x = startingX + xOffset;
        y = startingY + yOffset;
        vx = startingVX;
        vy = startingVY;
        alpha = 1;
        size = startingSize;
        tempVy = 0;
        mergedThisFrame = false;
    }

    // Draw particle
    public void draw(GraphicsContext gc) {
        // Draw the glowing effect
        Color fxGlowColor = Color.rgb(
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                alpha
        );

        gc.setFill(fxGlowColor);
        gc.fillOval(
                x - size * 0.5,
                y - size * 0.5,
                size * 1.5,
                size * 1.5
        );

        // Draw the core particle
        Color fxColor = Color.rgb(
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255),
                alpha
        );

        gc.setFill(fxColor);
        gc.fillOval(
                x - size / 2,
                y - size / 2,
                size,
                size
        );
    }

}
