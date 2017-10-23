package edu.dnatools.calculate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.model.Protein;
import edu.dnatools.service.ProteinService;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance;
import org.nd4j.linalg.eigen.Eigen;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Transient;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * Created by luke on 6/16/17.
 */
public class NDDNA implements Serializable {

    public static final long serialVersionUID = 154L;

    private static Gson gson = new GsonBuilder().create();
    private static Logger log = LoggerFactory.getLogger(NDDNA.class);

    protected double[][][] forceConstants;
    protected double[][] stepParameters;
    protected INDArray tp0;
    protected INDArray FGH;
    protected INDArray A;
    protected INDArray refs;

    protected ProteinRecord[] p;

    private String[] stepList;
    private long[][] proteinList;

    protected String sequence;
    protected INDArray steps;
    protected int nsteps;
    protected boolean[] occupied;
    private boolean nbody = false, RNA = false;

    private transient Protein[] proteins;

    protected int[] proteinPositions;

    public NDDNA(String sequence, String stepList, String forceConstants, String stepParameters, String proteinList, ProteinService proteinService) {
        this.sequence = sequence.toUpperCase();
        this.nsteps = sequence.length()-1;
        this.occupied = new boolean[nsteps];
        this.stepList = gson.fromJson(stepList, String[].class);
        this.forceConstants = gson.fromJson(forceConstants, double[][][].class);
        this.stepParameters = gson.fromJson(stepParameters, double[][].class);
        this.proteinList = gson.fromJson(proteinList, long[][].class);
        steps = Nd4j.create(new float[nsteps*6], new int[] {nsteps, 6});

        if (this.proteinList.length > 0) {
            proteins = new Protein[this.proteinList.length];
            proteinPositions = new int[this.proteinList.length];
            for (int i = 0; i < this.proteinList.length; i++) {
                proteins[i] = proteinService.getOne(this.proteinList[i][0]);
                proteinPositions[i] = (int)(this.proteinList[i][1]-1);
                for (int j = 0; j < proteins[i].getStepLength(); j++) {
                    if (proteinPositions[i]+j >= nsteps) break;
                    occupied[proteinPositions[i]+j] = true;
                }
            }
        }
        setupSteps();
        setupProteins();
        A = calculateA(steps);
        refs = calculateE(A);
        float E;
        log.info("Starting energy: " + (E = calculateEnergy(steps, refs)));
        while (E > 1e6) {
            generateAndPlace();
            A = calculateA(steps);
            refs = calculateE(A);
            E = calculateEnergy(steps, refs);
            log.info(""+E);
        }
        log.info("Corrected starting energy: " + E);
    }

    // To try a new starting structure
    protected void generateAndPlace() {
        steps.assign(tp0.dup());
        steps.getColumn(0).addi(Nd4j.randn(new int[] {nsteps,1}).muli(8.0));
        steps.getColumn(1).addi(Nd4j.randn(new int[] {nsteps,1}).muli(8.0));
        steps.getColumn(2).addi(Nd4j.randn(new int[] {nsteps,1}).muli(8.0));
        steps.getColumn(3).addi(Nd4j.randn(new int[] {nsteps,1}).muli(0.05));
        steps.getColumn(4).addi(Nd4j.randn(new int[] {nsteps,1}).muli(0.05));
        steps.getColumn(5).addi(Nd4j.randn(new int[] {nsteps,1}).muli(0.05));
        for (int i = 0; i < proteinPositions.length; i++) {
            steps.get(NDArrayIndex.interval(proteinPositions[i], proteinPositions[i]+p[i].steps.size(0)), NDArrayIndex.all())
                    .assign(p[i].steps);
        }
    }


    protected void setupSteps() {
        float[][][] fci = new float[nsteps][6][6];
        float[][] tpi = new float[nsteps][6];
        for (int i = 0; i < nsteps; i++) {
            String step = ""+sequence.charAt(i)+sequence.charAt(i+1);
            for (int j = 0; j < stepParameters.length; j++) {
                if (step.equals(stepList[j])) {
                    for (int k = 0; k < 6; k++) {
                        tpi[i][k] = (float)stepParameters[j][k];
                        for (int l = 0; l < 6; l++) fci[i][k][l] = (float)forceConstants[j][k][l];
                    }
                }
            }
        }
        float[] flat = ArrayUtil.flattenFloatArray(fci);
        FGH = Nd4j.create(flat, new int[] {nsteps, 6, 6});
        flat = ArrayUtil.flattenFloatArray(tpi);
        tp0 = Nd4j.create(flat, new int[] {nsteps, 1, 6});
        for (int i = 0; i < nsteps; i++) {
            IComplexNDArray[] eigenvectors = Eigen.eigenvectors(FGH.slice(i));
            IComplexNDArray eigenvalues = Eigen.eigenvalues(FGH.slice(i));
            log.info(eigenvectors[0]+"");
            log.info(eigenvalues+"");
        }
        steps.assign(tp0);
        steps.getColumn(0).addi(Nd4j.randn(new int[] {nsteps,1}).muli(3.0));
        steps.getColumn(1).addi(Nd4j.randn(new int[] {nsteps,1}).muli(3.0));
        steps.getColumn(2).addi(Nd4j.randn(new int[] {nsteps,1}).muli(3.0));
        steps.getColumn(3).addi(Nd4j.randn(new int[] {nsteps,1}).muli(0.05));
        steps.getColumn(4).addi(Nd4j.randn(new int[] {nsteps,1}).muli(0.05));
        steps.getColumn(5).addi(Nd4j.randn(new int[] {nsteps,1}).muli(0.05));
    }



    protected void setupProteins() {
        p = new ProteinRecord[proteins.length];
        for (int i = 0; i < proteins.length; i++) {
            String[] pdbs = proteins[i].getPdbs().split("END\n");
            String[] dats = proteins[i].getDats().split("END\n");
            p[i] = new ProteinRecord();
// just using structure 0 for now.
            String[] dat0 = dats[0].split("\\r?\\n");
            int npsteps = Integer.parseInt(dat0[0]);
            p[i].steps = Nd4j.create(new float[npsteps*6], new int[] {npsteps, 6});
            for (int j = 0; j < npsteps; j++) {
                String[] spars = dat0[j+1].split("\\s+");
                for (int k = 0; k < 6; k++)
                    p[i].steps.getRow(j).getColumn(k).assign(Float.parseFloat(spars[k]));
            }
            for (int j = 0; j < pdbs.length; j++) {
                String[] lines = pdbs[j].split("\\r?\\n");
                List<Point3D> CApositions = new ArrayList<>();
                List<Point3D> allpositions = new ArrayList<>();
                for (int k = 0; k < lines.length; k++) {
                    if (lines[k].substring(0, 4).equals("ATOM")) {
                        int n = Integer.parseInt(lines[k].substring(22, 26).trim());
                        float x = Float.parseFloat(lines[k].substring(30, 38));
                        float y = Float.parseFloat(lines[k].substring(38, 46));
                        float z = Float.parseFloat(lines[k].substring(46, 54));
                        String resname = lines[k].substring(17, 21).trim();
                        String atomname = lines[k].substring(13, 17).trim();
                        String chainId = lines[k].substring(21,22);
                        p[i].atomNames.add(atomname);
                        p[i].resNames.add(resname);
                        p[i].resNums.add(n);
                        p[i].chainId.add(chainId);
                        if (atomname.equals("CA")) {
                            CApositions.add(new Point3D(x, y, z));
                            p[i].CAresidue.add(resname);
                            //proteinCA.slice(0).assign(Nd4j.create(new float[] {x, y, z, 0}));
                        }
                        allpositions.add(new Point3D(x, y, z));
                        //log.debug("Atom " + atomname + " of " + resname + " at " + x + " " + y + " " + z);
                    }
                }
                p[i].CApositions = Nd4j.create(new float[CApositions.size()*4], new int[] {CApositions.size(), 4});
                p[i].CApositions.getColumn(3).assign(1.0);
                p[i].proteinCharge = Nd4j.create(new float[CApositions.size()], new int[] {CApositions.size()});
                for (int k = 0; k < CApositions.size(); k++) {
                    p[i].CApositions.getRow(k).getColumn(0).assign(CApositions.get(k).getX());
                    p[i].CApositions.getRow(k).getColumn(1).assign(CApositions.get(k).getY());
                    p[i].CApositions.getRow(k).getColumn(2).assign(CApositions.get(k).getZ());
                    p[i].proteinCharge.getColumn(k).assign(ProteinRecord.assignCharge(p[i].CAresidue.get(k)));
                }
                p[i].allpositions = Nd4j.create(new float[allpositions.size()*4], new int[] {allpositions.size(), 4});
                p[i].allpositions.getColumn(3).assign(1.0);
                for (int k = 0; k < allpositions.size(); k++) {
                    p[i].allpositions.getRow(k).getColumn(0).assign(allpositions.get(k).getX());
                    p[i].allpositions.getRow(k).getColumn(1).assign(allpositions.get(k).getY());
                    p[i].allpositions.getRow(k).getColumn(2).assign(allpositions.get(k).getZ());
                }
            }
            steps.get(NDArrayIndex.interval(proteinPositions[i], proteinPositions[i]+npsteps), NDArrayIndex.all())
                    .assign(p[i].steps);
        }
        A = calculateA(steps);
        refs = calculateE(A);

        log.info(steps+"");
    }




    // this is to try to optimize all body-body interactions
    protected float calculateNbody(INDArray inputsteps, INDArray inputrefs) {
        float E = 0.0f;
        INDArray[] coordinates = new INDArray[proteinPositions.length];
        INDArray[] ncoordinates = new INDArray[proteinPositions.length];
        INDArray distances;
        for (int i = 0; i < proteinPositions.length; i++) {
            //log.info(inputrefs.slice(proteinPositions[i])+"");
            coordinates[i] = p[i].transformCACoordinates(inputrefs.slice(proteinPositions[i]));
            ncoordinates[i] = inputrefs.get(NDArrayIndex.interval(proteinPositions[i], proteinPositions[i]+p[i].steps.size(0)), NDArrayIndex.interval(0,3),  NDArrayIndex.point(3));
        }
        for (int i = 0; i < proteinPositions.length; i++) {
            for (int j = i+1; j < proteinPositions.length; j++) {
                distances = Transforms.allEuclideanDistances(coordinates[i], coordinates[j], 1);
                if (distances.minNumber().floatValue() < 6.0f) E += 1e8;

                distances = Transforms.allEuclideanDistances(coordinates[i], ncoordinates[j], 1);
                if (distances.minNumber().floatValue() < 10.0f) E += 1e8;

                distances = Transforms.allEuclideanDistances(ncoordinates[i], coordinates[j], 1);
                if (distances.minNumber().floatValue() < 10.0f) E += 1e8;
            }
            //distances = Transforms.allEuclideanDistances(coordinates[i], ncoordinates[proteinPositions.length], 1);
            //if (distances.minNumber().floatValue() < 8.0) E += 1e8;
        }
        return E;
    }

    protected float calculateEnergy(INDArray inputsteps, INDArray inputrefs) {
        float E = 0.0f;
        if (nbody) E += calculateNbody(inputsteps, inputrefs);
        for (int i = 0; i < nsteps; i++) {
            INDArray dx = inputsteps.slice(i).sub(tp0.slice(i));
            INDArray result = dx.mmul(FGH.slice(i)).mmul(dx.transpose());
            E += result.getFloat(0);
        }
        return 0.5f*E;
    }

    public float calculateDNAProteinDistances(INDArray refs, INDArray a) {
        float E = 0.0f;
        INDArray r = refs.get(NDArrayIndex.all(), NDArrayIndex.interval(0,3),  NDArrayIndex.point(3));
        INDArray distances = Transforms.allEuclideanDistances(r, a, 1);
        if (distances.minNumber().floatValue() < 5.0) E += 1e8;
        return E;
    }


    protected float calculateProteinDistances(INDArray a, INDArray b) {
        float E = 0.0f;
        INDArray distances = Transforms.allEuclideanDistances(a, b, 1);
        if (distances.minNumber().floatValue() < 6.0) E += 1e8;
        return E;
//        return distances;
    }

    void print3dna(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) f.createNewFile();
            // to-do

            BufferedWriter bw = new BufferedWriter(new FileWriter(f.getAbsoluteFile(), false));
            String pair = (sequence.charAt(0) == 'Z' ? "A-T" : sequence.charAt(0)+"-"+edu.dnatools.basepairs.BasePairParameters.complement(""+sequence.charAt(0), RNA));
            bw.write(String.format("  %d base-pairs\n", (nsteps+1)));
            bw.write(String.format("   0  step parameters\n"));
            bw.write(String.format("      shift   slide    rise    tilt    roll   twist\n"));
            bw.write(String.format(pair + "    0.00    0.00    0.00    0.00    0.00    0.00\n"));

            for (int i = 0; i < steps.size(0); i++) {
                pair = (sequence.charAt(i+1) == 'Z' ? "A-T" : sequence.charAt(i+1)+"-"+edu.dnatools.basepairs.BasePairParameters.complement(sequence.charAt(i+1)+"", RNA));
                String line = String.format(pair+"  %8.4f   %8.4f  %8.4f  %8.4f  %8.4f  %8.4f \n",
                        steps.slice(i).getFloat(3), steps.slice(i).getFloat(4), steps.slice(i).getFloat(5),
                        steps.slice(i).getFloat(0), steps.slice(i).getFloat(1), steps.slice(i).getFloat(2));
                bw.write(line);
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

/*
    public void testTadReduce3_3() throws Exception {
        INDArray initial = Nd4j.create(5, 10);
        for (int i = 0; i < initial.rows(); i++) {
            initial.getRow(i).assign(i + 1);
        }
        INDArray needle = Nd4j.create(1, 10).assign(1.0);
        INDArray reduced = Nd4j.getExecutioner().exec(new EuclideanDistance(initial, needle), 1);

        log.info("Reduced: {}", reduced);
    }
*/

    public static INDArray calculateA(INDArray stepdata) {
        INDArray result = Nd4j.create(new float[stepdata.rows()*16], new int[] {stepdata.rows(), 16}, 'r');
        INDArray t1 = stepdata.getColumn(0).mul(Math.PI/180.0);
        INDArray t2 = stepdata.getColumn(1).mul(Math.PI/180.0);
        INDArray t3 = stepdata.getColumn(2).mul(Math.PI/180.0);
        INDArray gamma = Transforms.sqrt(t1.mul(t1).addi(t2.mul(t2)));
// THIS LINE NEEDED ATAN2()
 //       INDArray phi = Transforms.atan(t1.div(t2), false);
        INDArray phi = Transforms.atan2(t2, t1);

        INDArray sp = Transforms.sin(t3.div(2.0).addi(phi));
        INDArray cp = Transforms.cos(t3.div(2.0).addi(phi));
        INDArray sm = Transforms.sin(t3.div(2.0).subi(phi));
        INDArray cm = Transforms.cos(t3.div(2.0).subi(phi));
        INDArray sg = Transforms.sin(gamma.dup());
        INDArray cg = Transforms.cos(gamma.dup());
        result.getColumn(0).assign(cm.mul(cg).muli(cp).subi(sm.mul(sp)));
        result.getColumn(1).assign(cm.neg().muli(cg)).muli(sp).subi(sm.mul(cp));
        result.getColumn(2).assign(cm.mul(sg));

        result.getColumn(4).assign(sm.mul(cg).muli(cp).addi(cm.mul(sp)));
        result.getColumn(5).assign(sm.neg().muli(cg).muli(sp).addi(cm.mul(cp)));
        result.getColumn(6).assign(sm.mul(sg));

        result.getColumn(8).assign(sg.neg().muli(cp));
        result.getColumn(9).assign(sg.mul(sp));
        result.getColumn(10).assign(cg);

        sp = Transforms.sin(phi.dup());
        cp = Transforms.cos(phi.dup());
        sg = Transforms.sin(gamma.div(2.0));
        cg = Transforms.cos(gamma.div(2.0));

        INDArray t4 = stepdata.getColumn(3);
        INDArray t5 = stepdata.getColumn(4);
        INDArray t6 = stepdata.getColumn(5);

        result.getColumn(3).assign(t4.mul(cm.mul(cg).muli(cp).subi(sm.mul(sp)))
                .addi(t5.mul(cm.mul(cg).muli(sp).negi().subi(sm.mul(cp))))
                .addi(t6.mul(cm).muli(sg)));
        result.getColumn(7).assign(t4.mul(sm.mul(cg).muli(cp).addi(cm.mul(sp)))
                .addi(t5.mul(sm.mul(cg).muli(sp).negi().addi(cm.mul(cp))))
                .addi(t6.mul(sm).muli(sg)));
        result.getColumn(11).assign(t4.mul(sg.mul(cp).negi())
                .addi(t5.mul(sg).muli(sp))
                .addi(t6.mul(cg)));

        result.getColumn(15).assign(1.0);

        INDArray r = result.reshape(stepdata.rows(), 4, 4);

        return r;
    }


    public static INDArray calculatetp(INDArray A) {

        double[] M = new double[6];

        double cosgamma, gamma, phi, omega, sgcp, omega2_minus_phi,
                sm, cm, sp, cp, sg, cg;

        cosgamma = A.getDouble(2,2);
        if (cosgamma > 1.0) cosgamma = 1.0;
        else if (cosgamma < -1.0) cosgamma = -1.0;

        gamma = acos(cosgamma);

        sgcp = A.getDouble(1,1)*A.getDouble(0,2)-A.getDouble(0,1)*A.getDouble(1,2);

        if (gamma == 0.0) omega = -atan2(A.getDouble(0,1),A.getDouble(1,1));
        else omega = atan2(A.getDouble(2,1)*A.getDouble(0,2)+sgcp*A.getDouble(1,2),
                sgcp*A.getDouble(0,2)-A.getDouble(2,1)*A.getDouble(1,2));

        omega2_minus_phi = atan2(A.getDouble(1,2),A.getDouble(0,2));

        phi = omega/2.0 - omega2_minus_phi;

        M[0] = gamma*sin(phi)*180.0/PI;
        M[1] = gamma*cos(phi)*180.0/PI;
        M[2] = omega*180.0/PI;

        sm = sin(omega/2.0-phi);
        cm = cos(omega/2.0-phi);
        sp = sin(phi);
        cp = cos(phi);
        sg = sin(gamma/2.0);
        cg = cos(gamma/2.0);

        M[3] = (cm*cg*cp-sm*sp)*A.getDouble(0,3)+(sm*cg*cp+cm*sp)*A.getDouble(1,3)-sg*cp*A.getDouble(2,3);
        M[4] = (-cm*cg*sp-sm*cp)*A.getDouble(0,3)+(-sm*cg*sp+cm*cp)*A.getDouble(1,3)+sg*sp*A.getDouble(2,3);
        M[5] = (cm*sg)*A.getDouble(0,3)+(sm*sg)*A.getDouble(1,3)+cg*A.getDouble(2,3);

        return Nd4j.create(M, new int[] {1,6});

    }


    public static INDArray calculateE(INDArray A) {
        INDArray refs = Nd4j.create(A.size(0)+1, 4, 4);
        refs.slice(0).assign(Nd4j.eye(4));
        for (int i = 0; i < A.size(0); i++)
            refs.slice(i+1).assign(refs.slice(i).mmul(A.slice(i)));
        //log.debug(refs+"");
        return refs;
    }


}


class Point3D {
    private float x, y, z;

    public Point3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

}