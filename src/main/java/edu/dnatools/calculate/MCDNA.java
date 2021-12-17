package edu.dnatools.calculate;

import edu.dnatools.service.ProteinService;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;

/**
 * Created by luke on 7/2/17.
 */
public class MCDNA extends NDDNA implements Serializable, MonteCarloEngine {

    @Serial
    private static final long serialVersionUID = 621L;
    public static final Logger log = LoggerFactory.getLogger(MCDNA.class);

    private INDArray newA, newsteps, newrefs;
    private float oldEnergy, newEnergy;

    private float MOVESIZE = 0.5f;
    int count = 0, accepted = 0;

    public MCDNA(String sequence, String stepList, String forceConstants, String stepParameters, String proteinList, ProteinService proteinService) {
        super(sequence, stepList, forceConstants, stepParameters, proteinList, proteinService);
        oldEnergy = calculateEnergy(steps, refs);
        count = 0; accepted = 0;
    }

    public void printStats() {
        System.out.println("Count: " + count + " with accepted: " + accepted);
        System.out.println("Old = " + oldEnergy + " New = " + newEnergy);
        System.out.println(steps+"");
    }

    public void launch(int nIterations, float MOVESIZE) {
        this.MOVESIZE = MOVESIZE;
        for (int i = 0; i < nIterations; i++) {
            move();
            if (test()) accept(); else reject();
        }
        log.info("Acceptance rate " + ((float)accepted/(float)count));
        log.info("System energy " + oldEnergy + " kT");
        A = calculateA(steps);
        refs = calculateE(A);
        //log.info(refs+"");
        //log.info("BASIS: " + refs.slice(0)+"");
        //log.info(A+"");
        print3dna("output-structure.dat");
        File f;
        if ((f = new File("proteins.pdb")).exists()) f.delete();
        for (int i = 0; i < p.length; i++) {
            p[i].writePDB("proteins.pdb", refs.slice(proteinPositions[i]));
        }
        saveState("simulation.state");
    }

    @Override
    public void move() {
        newsteps = steps.dup();
        for (int i = 0; i < nsteps; i++) {
            if (!occupied[i]) {
                newsteps.slice(i).getColumn(0).addi(MOVESIZE*(Math.random()-0.5));
                newsteps.slice(i).getColumn(1).addi(MOVESIZE*(Math.random()-0.5));
                newsteps.slice(i).getColumn(2).addi(MOVESIZE*(Math.random()-0.5));
            }
        }
        newA = calculateA(newsteps);
        newrefs = calculateE(newA);
    }

    @Override
    public boolean test() {
        newEnergy = calculateEnergy(newsteps, newrefs);
        if (newEnergy < oldEnergy) return true;
        else if (Math.random() < Math.exp(oldEnergy - newEnergy)) return true;
        return false;
    }

    @Override
    public void accept() {
        steps = newsteps;
        refs = newrefs;
        oldEnergy = newEnergy;
        log.info("Accepted " + newEnergy + " kT");
        accepted++;
        count++;
    }

    @Override
    public void reject() {
        log.info("Rejected " + newEnergy + " kT");
        count++;
    }




    // Serialization and deserialization
    public void saveState(String filename) {
        File f = new File(filename);
        if (f != null) try {
            OutputStream file = new FileOutputStream(f);
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutput output = new ObjectOutputStream(buffer);
            output.writeObject(this);
            output.flush();
            output.close();
            file.flush();
            file.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static MCDNA restoreState(String filename) {

        File f = new File(filename);
        if (f != null && f.exists()) try {
            InputStream file = new FileInputStream(f);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            //deserialize the Map
            MCDNA sim = (MCDNA)input.readObject();
            file.close();
            return sim;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return null;
    }

}
