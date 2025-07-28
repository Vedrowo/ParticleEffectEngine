package org.example;

import java.io.Serializable;
import java.util.ArrayList;

public class WorkerResponse implements Serializable {
    public ArrayList<particle> particles;
    public ArrayList<particle> adioParticles;

    public WorkerResponse(ArrayList<particle> particles, ArrayList<particle> adioParticles) {
        this.particles = particles;
        this.adioParticles = adioParticles;
    }
}
