package org.example;

import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Worker {
    private final int port;
    private ArrayList<particle> particles = new ArrayList<>();
    private int maxX, maxY;
    private final Map<Point, ConcurrentLinkedQueue<particle>> particleMap = new HashMap<>();
    private boolean simulationRunning = false;

    public Worker(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker listening on port " + port);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                     BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

                    String message = in.readLine();
                    System.out.println("Received: " + message);

                    if (!simulationRunning) {
                        simulationRunning = true;
                        new Thread(this::startSimulationLoop).start(); // start particle loop in background
                    }

                    String reply = "Hello from worker on port " + port;
                    out.write(reply);
                    out.newLine();
                    out.flush();
                } catch (IOException e) {
                    System.err.println("Client connection error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Could not listen on port " + port);
            e.printStackTrace();
        }
    }

    private void startSimulationLoop() {
        while (true) {
            particleEngineUtil.updateParticlesDistributed(particleMap, particles);

            // Add logic to transfer particles to other workers if they cross zone bounds

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
