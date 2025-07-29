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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Worker {

    private final int port;
    private ArrayList<particle> particles, overtimeParticles;
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
            System.out.println("Worker bing shilling on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("GUI connected");

                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                Object obj = in.readObject();

                if(obj instanceof WorkerData data){
                    this.rangeStart = data.rangeStart;
                    this.rangeEnd = data.rangeEnd;
                    this.particles = data.particles;
                    this.mode = data.mode;
                    this.overtimeParticles = new ArrayList<>();
                }

                simulationRunning = true;

                if(Objects.equals(mode, "Over time")){
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    scheduler.scheduleAtFixedRate(() -> {
                        int addCount;
                        synchronized (particles) {
                            addCount = Math.min(10, particles.size());
                            if (addCount > 0) {
                                ArrayList<particle> particlesToAdd = new ArrayList<>(particles.subList(0, addCount));
                                particles.subList(0, addCount).clear();
                                synchronized (overtimeParticles) {
                                    overtimeParticles.addAll(particlesToAdd);
                                }
                            }

                            if (particles.isEmpty()) {
                                scheduler.shutdown();
                            }
                        }
                    }, 0, 16, TimeUnit.MILLISECONDS);  // 16 ms ≈ 60fps

                }

                Thread readerThread = new Thread(() -> {
                    try {
                        while (simulationRunning) {
                            Object incoming = in.readObject();
                            System.out.println("Listening");
                            if (incoming instanceof WorkerResponse resp) {
                                ArrayList<particle> newParticles = resp.sayonaraParticles;
                                if (newParticles != null && !newParticles.isEmpty()) {
                                    if (Objects.equals(mode, "Burst")) {
                                        synchronized (particles) {
                                            particles.addAll(newParticles);
                                        }
                                    } else {
                                        synchronized (overtimeParticles) {
                                            overtimeParticles.addAll(newParticles);
                                        }
                                    }
                                    System.out.println("Worker "+workerID+" received "+newParticles.size()+" particles");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        simulationRunning = false;
                    }
                });
                readerThread.setDaemon(true);
                readerThread.start();


                System.out.println("Worker " + workerID + " initialized with " + particles.size() + " particles.");

                // simulation loop in another thread
                Thread simulationThread = new Thread(() -> runSimulationLoop(out, mode));
                simulationThread.setDaemon(true);
                simulationThread.start();


            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runSimulationLoop(ObjectOutputStream out, String mode) {
        try {
            while (simulationRunning) {
                synchronized (particles){
                    ArrayList<particle> adioParticles = new ArrayList<>();

                    if (Objects.equals(mode, "Burst")) {
                        synchronized (particles) {
                            particleEngineUtil.updateParticlesDistributed(particleMap, particles);
                            for (particle p : particles) {
                                if (p.y < rangeEnd && workerID < 3) {
                                    adioParticles.add(p);
                                }
                            }
                            particles.removeAll(adioParticles);
                        }
                    } else {
                        synchronized (overtimeParticles) {
                            particleEngineUtil.updateParticlesDistributed(particleMap, overtimeParticles);
                            for (particle p : overtimeParticles) {
                                if (p.y < rangeEnd && workerID < 3) {
                                    adioParticles.add(p);
                                }
                            }
                            overtimeParticles.removeAll(adioParticles);
                        }
                    }

                    // Send updated particles back to GUI
                    out.reset();

                    if(Objects.equals(mode, "Over time")){
                        ArrayList<particle> safeCopy;
                        synchronized (overtimeParticles) {
                            safeCopy = new ArrayList<>(overtimeParticles);
                        }
                        out.writeObject(new WorkerResponse(safeCopy, adioParticles));
                    } else if (Objects.equals(mode, "Burst")){
                        ArrayList<particle> safeCopy;
                        synchronized (particles) {
                            safeCopy = new ArrayList<>(particles);
                        }
                        out.writeObject(new WorkerResponse(safeCopy, adioParticles));
                    }
                    out.flush();
                }
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
