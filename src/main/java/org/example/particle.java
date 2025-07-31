package org.example;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.io.Serializable;

public class particle implements Serializable {
    private static final long serialVersionUID = 1L;

    public double x, y, startingX, startingY, startingSize, startingVX, startingVY;
    public double vx, vy, size, lifetime, currentLifetime, age, alpha;
    public double r = 1.0, g = 1.0, b = 1.0;
    public double tempVy = 0;
    public int maxX, maxY;
    private final double xOffset, yOffset;
    public boolean mergedThisFrame = false;

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
        double time = System.currentTimeMillis() / 1000.0;
        double phase = Math.random() * 2 * Math.PI;

        double amplitude = 1.5;
        double frequency = 10.0;
        vx = amplitude * Math.sin(2 * Math.PI * frequency * time + phase);
        vy = 0 - Math.abs(Math.sin(angle) * speed);
        this.startingVX = vx;
        this.startingVY = vy;

        if(vy > -1){
            vy = vy - 1.5;
        }

        r = 1.0;
        g = 1.0;
        b = 1.0;

        if (Math.random() < 1.0 / 500) {
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

    public void movement() {
        if (tempVy != 0) {
            vy = tempVy;
        }

        age = Math.max(0, Math.min(1, currentLifetime / lifetime));
        vx *= (1 - 0.01 * age);
        vy *= (1 - 0.01 * age);

        // gravity
        double convection = 0.001;
        vy += convection;

        if (y > 1) {
            y += vy;
        }

        if (x > 1 && x < maxX) {
            x += vx;
        }

        currentLifetime++;

        size = (age < 0.3) ? size + 0.2 : size - 0.6;
        alpha = 1 * (1 - Math.pow(age, 2));
        getColorBasedOnLifetime(currentLifetime, lifetime, alpha);
    }

    public boolean isAlive() {
        return currentLifetime < lifetime;
    }

    private void getColorBasedOnLifetime(double currentLifetime, double lifetime, double alpha) {
        age = Math.max(0, Math.min(1, currentLifetime / lifetime));  // age between 0 and 1

        r = 1.0;
        g = Math.max(0, Math.min(1, 1 - 2 * age));
        b = Math.max(0, Math.min(1, 1 - 10 * age));
        this.alpha = alpha;
    }

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

    public void draw(GraphicsContext gc, double canvasWidth, double canvasHeight) {

        if (x + size < 0 || x - size > canvasWidth || y + size < 0 || y - size > canvasHeight) return;

        gc.setFill(getColor());
        gc.fillOval(
                x - size / 2,
                y - size / 2,
                size,
                size
        );
    }

    public Color getColor() {
        return new Color(r, g, b, alpha);
    }

}
