package org.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class WorkerListener implements Runnable {
    private final ArrayList<particle> particles;
    private final ObjectInputStream in;
    private final int port, rank;
    private final boolean[] isWorkerDone;
    private final ObjectOutputStream[] outStream;
    private volatile boolean running = true;

    public WorkerListener(ArrayList<particle> particles, ObjectInputStream in, ObjectOutputStream[] outStream, int port, boolean[] isWorkerDone) {
        this.particles = particles;
        this.in = in;
        this.port = port;
        this.rank = port % 5000;
        this.isWorkerDone = isWorkerDone;
        this.outStream = outStream;
    }

    @Override
    public void run() {
        try {
            while (running) {
                @SuppressWarnings("unchecked")
                WorkerResponse response = (WorkerResponse) in.readObject();
                ArrayList<particle> updatedParticles = response.particles;
                ArrayList<particle> sayonaraParticles = response.sayonaraParticles;

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
                if(!sayonaraParticles.isEmpty() && (rank + 1) < outStream.length) {
                    try{
                        synchronized (outStream[rank+1]) {
                            outStream[rank+1].reset();
                            outStream[rank+1].writeObject(new WorkerResponse(null, sayonaraParticles));
                            outStream[rank+1].flush();
                            System.out.println("Worker fella " +rank+ " yeeted " + sayonaraParticles.size() + " particles to worker "+(rank+1));
                        }
                    } catch (Exception e){
                        System.err.println("Failed to forward particles to worker " + (rank+1) + ": " + e.getMessage());
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error in WorkerListener for port " + port + ": " + e.getMessage());
        }
    }

    public void stopListening() {
        isWorkerDone[rank] = false;
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

