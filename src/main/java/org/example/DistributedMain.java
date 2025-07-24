package org.example;

import mpi.MPI;

public class DistributedMain {
    public static void main(String[] args) throws Exception {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Hello from process "+ rank +" out of "+ size);

        MPI.Finalize();
    }
}
