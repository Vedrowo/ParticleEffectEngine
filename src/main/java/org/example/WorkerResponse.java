package org.example;

import java.io.Serializable;
import java.util.ArrayList;

public class WorkerResponse implements Serializable {
    public ArrayList<particle> particles;
    public ArrayList<particle> sayonaraParticles;

    public WorkerResponse(ArrayList<particle> particles, ArrayList<particle> sayonaraParticles) {
        this.particles = particles;
        this.sayonaraParticles = sayonaraParticles;
    }
}
