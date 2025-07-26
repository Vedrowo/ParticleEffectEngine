package org.example;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Worker {
    private final int port;
    private ArrayList<particle> particles;
    private final Map<Point, ConcurrentLinkedQueue<particle>> particleMap = new HashMap<>();
    private boolean simulationRunning = false;
    private final int workerID;
    private int rangeStart, rangeEnd;
    private String mode;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public Worker(int port) {
        this.port = port;
        this.workerID = port % 5000;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker listening on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    out = new ObjectOutputStream(clientSocket.getOutputStream());
                    in = new ObjectInputStream(clientSocket.getInputStream());


                    WorkerData data = (WorkerData) in.readObject();
                    this.rangeStart = data.rangeStart;
                    this.rangeEnd = data.rangeEnd;
                    this.particles = data.particles;
                    this.mode = data.mode;

                    System.out.println("Worker " + workerID + " initialized with " + particles.size() + " particles. Range: " + rangeStart + "-" + rangeEnd);

                    if (!simulationRunning) {
                        simulationRunning = true;
                        new Thread(() -> startSimulationLoop(out)).start();
                    }

                } catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                    simulationRunning = false;
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }


        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            e.printStackTrace();
        }
    }

    private void startSimulationLoop(ObjectOutputStream out) {
        while (true) {
            if(Objects.equals(mode, "Burst")){
                particleEngineUtil.updateParticlesDistributed(particleMap, particles);
            }


            for(particle p : particles) {
                if(p.y > rangeEnd && workerID < 3){
                    System.out.println("particles left boundary");
                }
            }

            try {
                out.reset(); // important to avoid memory leak in object serialization
                out.writeObject(particles);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending updated particles: " + e.getMessage());
                simulationRunning = false;
                break;
            }

            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public static void main(String[] args) {
        int port = 5000;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        Worker worker = new Worker(port);
        worker.start();
    }
}
