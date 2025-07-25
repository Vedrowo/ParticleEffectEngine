package org.example;

import javafx.application.Application;
import mpi.MPI;
import mpi.MPIException;

public class DistributedMain {
    public static void main(String[] args) throws MPIException {
        MPI.Init(args);
        int rank = MPI.COMM_WORLD.Rank();
        System.out.println("Rank: " + rank);

        if (rank == 0) {
            // launch JavaFX GUI
            Application.launch(GuiApp.class, args);
        } else {
            // worker code here
        }
        MPI.Finalize();
    }

}
