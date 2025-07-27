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

    public Worker(int port) {
        this.port = port;
        this.workerID = port % 5000;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("GUI connected");

                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                // Read initial config from GUI
                WorkerData data = (WorkerData) in.readObject();
                this.rangeStart = data.rangeStart;
                this.rangeEnd = data.rangeEnd;
                this.particles = data.particles;
                this.mode = data.mode;

                System.out.println("Worker " + workerID + " initialized with " + particles.size() + " particles.");

                simulationRunning = true;

                // Start simulation loop in separate thread
                Thread simulationThread = new Thread(() -> runSimulationLoop(out));
                simulationThread.setDaemon(true);
                simulationThread.start();

                // Optional: if GUI can send commands during simulation, start a thread to listen for commands
                // Thread commandListener = new Thread(() -> listenForCommands(in));
                // commandListener.setDaemon(true);
                // commandListener.start();

                //simulationThread.join(); // Wait for simulation to finish before accepting new connection
                //simulationRunning = false;

                // Close streams and socket after simulation ends
                //in.close();
                //out.close();
                //clientSocket.close();
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runSimulationLoop(ObjectOutputStream out) {
        try {
            while (simulationRunning) {
                // Update particles according to mode
                if (Objects.equals(mode, "Burst")) {
                    particleEngineUtil.updateParticlesDistributed(particleMap, particles);
                } else {
                    // Implement other modes if needed
                }

                for (particle p : particles) {
                    if (p.y > rangeEnd && workerID < 3) {
                        System.out.println("particles left boundary");
                    }
                }

                // Send updated particles back to GUI
                out.reset();
                out.writeObject(particles);
                out.flush();

                // Sleep for ~16ms (~60fps)
                Thread.sleep(16);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Simulation loop error: " + e.getMessage());
            simulationRunning = false;
        }
    }

    public static void main(String[] args) {
        int port = 5000; // default
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        new Worker(port).start();
    }
}
