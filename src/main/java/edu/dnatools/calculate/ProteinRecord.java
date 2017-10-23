package edu.dnatools.calculate;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by luke on 7/2/17.
 */
public class ProteinRecord implements Serializable {

    public INDArray allpositions;
    public INDArray CApositions;
    public INDArray proteinCharge;
    public INDArray steps;

    public List<String> CAresidue = new ArrayList<>();
    public List<String> atomNames = new ArrayList<>();
    public List<String> resNames = new ArrayList<>();
    public List<Integer> resNums = new ArrayList<>();
    public List<String> chainId = new ArrayList<>();

    public INDArray transformCACoordinates(INDArray A) {
        return Nd4j.tensorMmul(A, CApositions, new int[][] {{1},{1}}).transpose().get(NDArrayIndex.all(), NDArrayIndex.interval(0,3));
    }

    public INDArray transformAllCoordinates(INDArray A) {
        return Nd4j.tensorMmul(A, allpositions, new int[][] {{1},{1}}).transpose();
    }

    public static int assignCharge(String resName) {
        if (resName.equals("LYS") || resName.equals("ARG")) return +1;
        else return 0;
    }

    public void writePDB(String filename, INDArray A) {
        try {
            File f = new File(filename);
            if (!f.exists()) f.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsoluteFile(), true));
            INDArray coordinates = transformAllCoordinates(A);
            for (int i = 0; i < coordinates.size(0); i++) {
                String line = String.format("ATOM  %5s  %-3s%4s%2s%4s%4s%8.2f%8.2f%8.2f  1.00  1.00    \n", (i+1)+"",
                        atomNames.get(i), resNames.get(i), chainId.get(i), resNums.get(i), "", coordinates.getRow(i).getFloat(0),
                        coordinates.getRow(i).getFloat(1), coordinates.getRow(i).getFloat(2));
                bw.write(line);
            }
            bw.write("TER\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
