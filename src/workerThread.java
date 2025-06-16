import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class workerThread extends Thread {

    ArrayList<particle> particles;
    private final HashMap<Point, ArrayList<particle>> particleMap = new HashMap<>();

    public workerThread(ArrayList<particle> particles) {
        this.particles = particles;
    }

    @Override
    public void run() {

    }
}
