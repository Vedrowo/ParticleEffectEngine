package org.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class WorkerListener implements Runnable {
    private final ArrayList<particle> particles;
    private final ObjectInputStream in;
    private final int port, rank;
    private final boolean[] isWorkerDone;

    public WorkerListener(ArrayList<particle> particles, ObjectInputStream in, int port, boolean[] isWorkerDone) {
        this.particles = particles;
        this.in = in;
        this.port = port;
        this.rank = port % 5000;
        this.isWorkerDone = isWorkerDone;
    }

    @Override
    public void run() {
        try {
            while (true) {
                @SuppressWarnings("unchecked")
                ArrayList<particle> updatedParticles = (ArrayList<particle>) in.readObject();

                synchronized (particles) {
                    particles.clear();
                    particles.addAll(updatedParticles);

                    // Check if all particles are dead:
                    boolean allDead = true;
                    for (particle p : particles) {
                        if (p.isAlive()) {
                            allDead = false;
                            break;
                        }
                    }
                    isWorkerDone[rank] = allDead;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in WorkerListener for port " + port + ": " + e.getMessage());
        }
    }
}

