package org.example;

import java.io.Serializable;
import java.util.ArrayList;

public class WorkerData implements Serializable {
    private static final long serialVersionUID = 1L;

    public int rangeStart, rangeEnd;
    public ArrayList<particle> particles;
    String mode;

    public WorkerData(int rangeStart, int rangeEnd, ArrayList<particle> particles, String mode) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.particles = particles;
        this.mode = mode;
    }
}
