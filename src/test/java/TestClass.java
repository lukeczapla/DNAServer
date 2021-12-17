/*
 * Created by luke on 7/3/17 modified 12/16/21.
 * */

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import edu.dnatools.basepairs.BasePairParameters;
import edu.dnatools.calculate.MCDNA;
import edu.dnatools.utils.RefTools;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.StructureIO;
import org.biojava.nbio.structure.StructureImpl;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance;
import org.nd4j.linalg.dimensionalityreduction.PCA;
import org.nd4j.linalg.eigen.Eigen;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.inverse.InvertMatrix;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.util.ArrayUtil;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;
import static java.lang.Math.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class TestClass {

    public static Gson gson = new GsonBuilder().create();

    // seem to be ok:  cg, cp, cm, sp, sm.  fishy: sg ?
    public static INDArray calculateA(INDArray stepdata) {
        INDArray result = Nd4j.create(new double[stepdata.rows()*16], new int[] {stepdata.rows(), 16});
        INDArray t1 = stepdata.getColumn(0).mul(Math.PI/180.0);
        INDArray t2 = stepdata.getColumn(1).mul(Math.PI/180.0);
        INDArray t3 = stepdata.getColumn(2).mul(Math.PI/180.0);
        INDArray gamma = Transforms.sqrt(t1.mul(t1).addi(t2.mul(t2)));
        INDArray phi = Transforms.atan2(t2, t1);

        INDArray sp = Transforms.sin(t3.div(2.0).addi(phi));
        INDArray cp = Transforms.cos(t3.div(2.0).addi(phi));
        INDArray sm = Transforms.sin(t3.div(2.0).subi(phi));
        INDArray cm = Transforms.cos(t3.div(2.0).subi(phi));
        INDArray sg = Transforms.sin(gamma.dup());
        System.out.println(phi.getDouble(0) + " " + cm.getDouble(0) + " " + sg.getDouble(0)+"");
        INDArray cg = Transforms.cos(gamma.dup());
        // cm cg sp cm cp,  sm cg cp cm sp      sm & cm   sp & cp  cg.  SP?
        result.getColumn(0).assign(cm.mul(cg).muli(cp).addi(sm.neg().muli(sp)));
        result.getColumn(1).assign(cm.neg().muli(cg).muli(sp).addi(sm.mul(cp).negi()));
        System.out.println(phi.getDouble(0) +" " +  cm.getDouble(0) + " " + sg.getDouble(0)+"");
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

        result.getColumn(3).assign(t4.mul(cm.mul(cg).muli(cp).addi(sm.mul(sp).negi()))
                .addi(t5.mul(cm.mul(cg).muli(sp).negi().addi(sm.mul(cp).negi())))
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


    public static double[][] calculateA(double[] tp) {

        double[][] M = new double[4][4];


        double gamma, phi, omega,
                sp, cp, sm, cm, sg, cg,
                t1, t2, t3;

        t1 = tp[0]*PI/180.0;
        t2 = tp[1]*PI/180.0;
        t3 = tp[2]*PI/180.0;

        gamma = sqrt(t1*t1+t2*t2);
        phi = atan2(t1,t2);
        omega = t3;

        sp = sin(omega/2.0+phi); cp = cos(omega/2.0+phi); sm = sin(omega/2.0-phi);
        cm = cos(omega/2.0-phi); sg = sin(gamma); cg = cos(gamma);
        System.out.println(phi + " " + cm + " " + sg);


        M[0][0] = cm*cg*cp-sm*sp;
        M[0][1] = -cm*cg*sp-sm*cp;
        M[0][2] = cm*sg;
        M[1][0] = sm*cg*cp+cm*sp;
        M[1][1] = -sm*cg*sp+cm*cp;
        M[1][2] = sm*sg;
        M[2][0] = -sg*cp;
        M[2][1] = sg*sp;
        M[2][2] = cg;
        M[3][0] = 0.0;
        M[3][1] = 0.0;
        M[3][2] = 0.0;
        M[3][3] = 1.0;

        sp = sin(phi); cp = cos(phi); sg = sin(gamma/2.0); cg = cos(gamma/2.0);

        M[0][3] = tp[3]*(cm*cg*cp-sm*sp) + tp[4]*(-cm*cg*sp-sm*cp) + tp[5]*(cm*sg);
        M[1][3] = tp[3]*(sm*cg*cp+cm*sp) + tp[4]*(-sm*cg*sp+cm*cp) + tp[5]*(sm*sg);
        M[2][3] = tp[3]*(-sg*cp) + tp[4]*(sg*sp) + tp[5]*(cg);

        return M;

    }
/*
    @Test
    public void testEuclideanDistances() {
        INDArray initialX = Nd4j.create(10000, 300);
        INDArray initialY = Nd4j.create(20000, 300);
        for (int i = 0; i < initialX.rows(); i++)
            initialX.getRow(i).assign(i+1);
        for (int i = 0; i < initialY.rows(); i++)
            initialY.getRow(i).assign(i+3);
        INDArray result = Transforms.allEuclideanDistances(initialX, initialY, 1);
    }
*/

    @Test
    public void calculateEigen() {
        double[] eigenvalues = new double[6];
        double[][] matrix = new double[][] {{0.0427,0,-0.04,0,0,0}, {0,0.0427,0,0,0,0}, {-0.04,0,0.0597,0,0,0}, {0,0,0,50,0,0}, {0,0,0,0,50,0}, {0,0,0,0,0,50}};
        double[][] result = RefTools.jeigen(matrix, eigenvalues);
        System.out.println(gson.toJson(result));
        System.out.println(gson.toJson(eigenvalues));
        INDArray m = Nd4j.create(ArrayUtil.flattenDoubleArray(matrix), new int[] {6,6});
        INDArray res = Eigen.symmetricGeneralizedEigenvalues(m);
        System.out.println(res+"");
        System.out.println(m+"");
        INDArray x = Nd4j.create(ArrayUtil.flattenDoubleArray(matrix), new int[] {6,6});
        res = Eigen.symmetricGeneralizedEigenvalues(x, m);
        System.out.println(""+x);
        System.out.println(res+"");
    }

    @Test
    public void loadSim() {
        MCDNA mydna = MCDNA.restoreState("simulation.state");
        mydna.launch(20, 2.0f);
    }

    @Test
    public void writeMaddocks() {
        Nd4j.setDataType(DataBuffer.Type.DOUBLE);
        try {
            Scanner scan = new Scanner(new FileInputStream("ordering.txt"));
            List<String> tetramers = new ArrayList<>();
            Map<String, INDArray> tetramerMeans = new LinkedHashMap<>();
            Map<String, INDArray> tetramerCov = new LinkedHashMap<>();
            for (int i = 0; i < 136; i++) {
                String tetramer = scan.nextLine();
                tetramers.add(tetramer);
                tetramerMeans.put(tetramer, Nd4j.create(1, 30));
                tetramerCov.put(tetramer, Nd4j.create(30, 30));
            }
            Gson gson = new Gson();
            scan = new Scanner(new FileInputStream("jmtetramers.txt"));
            for (int i = 0; i < 136; i++) {
                String tetramer = scan.next();
                boolean reverse = false;
                if (!tetramers.contains(tetramer)) {
                    tetramer = BasePairParameters.complement(tetramer, false);
                    reverse = true;
                }
                int steps = Integer.parseInt(scan.next());
                System.out.println(tetramer + " " + steps);
                INDArray dataset = Nd4j.create(steps, 30);
                scan.nextLine();
                for (int j = 0; j < steps; j++) {
                    String array = scan.nextLine();
                    double[] step = gson.fromJson(array, double[].class);
                    if (reverse) step = BasePairParameters.reversePacking(step);
                    for (int k = 0; k < 30; k++) dataset.getRow(j).getColumn(k).assign(step[k]);
                }
                scan.nextLine(); // blank line!
                PCA myPCA = new PCA(dataset);
                tetramerMeans.put(tetramer, myPCA.getMean());
                tetramerCov.put(tetramer, myPCA.getCovarianceMatrix());
            }
            PrintWriter out = new PrintWriter(new FileOutputStream("means.txt"));
            PrintWriter out2 = new PrintWriter(new FileOutputStream("covariances.txt"));
            for (String tetramer : tetramers) {
                INDArray mean = tetramerMeans.get(tetramer);
                out.print(tetramer);
                for (int i = 0; i < 30; i++) out.printf(" %f", mean.getDouble(i));
                out.println();
                INDArray cov = tetramerCov.get(tetramer);
                for (int i = 0; i < 30; i++) {
                    out2.print(tetramer);
                    for (int j = 0; j < 30; j++) {
                        out2.printf(" %f", cov.getRow(i).getDouble(j));
                    }
                    out2.println();
                }
            }
            out.close();
            out2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void analyzeExample() {
        PDBFileReader reader = new PDBFileReader();
        Structure s;
        try {
            //s = StructureIO.getStructure("1e7j");
            s = reader.getStructure(new FileInputStream("test-BDNA.pdb"));
        } catch (IOException/*|StructureException*/ e) {
            e.printStackTrace();
            return;
        }
        BasePairParameters bp = new BasePairParameters(s);
        bp.analyze();

        double[][] phos = bp.getPhosphateParameters();
        double[][] pairs = bp.getPairingParameters();
        double[][] bpsteps = bp.getStepParameters();

        for (int i = 0; i < bp.getLength(); i++) {
            for (int j = 0; j < 6; j++) {
                System.out.printf("%f ", pairs[i][j]);
            }
            System.out.println();
            for (int j = 0; j < 6; j++) {
                System.out.printf("%f ", bpsteps[i][j]);
            }
            System.out.println();
            for (int j = 0; j < 6; j++) {
                System.out.printf("%f ", phos[2*i][j]);
            }
            System.out.println();
            for (int j = 0; j < 6; j++) {
                System.out.printf("%f ", phos[2*i+1][j]);
            }
            System.out.println("\n\n");
        }

        System.out.println(bp);

    }

    @Test
    public void testAllDistances1() throws Exception {
        INDArray initialX = Nd4j.create(5, 10);
        INDArray initialY = Nd4j.create(7, 10);
        for (int i = 0; i < initialX.rows(); i++) {
            initialX.getRow(i).assign(i+1);
        }

        for (int i = 0; i < initialY.rows(); i++) {
            initialY.getRow(i).assign(i+101);
        }

        INDArray result = Transforms.allEuclideanDistances(initialX, initialY, 1);


        assertEquals(5 * 7, result.length());

        for (int x = 0; x < initialX.rows(); x++) {

            INDArray rowX = initialX.getRow(x).dup();

            for (int y = 0; y < initialY.rows(); y++) {

                double res = result.getDouble(x, y);
                double exp = Transforms.euclideanDistance(rowX, initialY.getRow(y).dup());

                assertEquals("Failed for [" + x + ", " + y +"]", exp, res, 0.001);
            }
        }
    }

    @Test
    public void testMe() {

        INDArray myArray = Nd4j.create(new float[] {-4.29f,  -2.65f,  28.45f,  -0.79f,  -0.62f,  3.04f,
                12.94f,  1.95f,  36.86f,  0.73f,  0.46f,  2.99f,
                -5.35f,  10.90f,  42.29f,  -0.03f,  0.70f,  3.50f}, new int[] {3, 6});

        double[][] data = {{-4.29f,  -2.65f,  28.45f,  -0.79f,  -0.62f,  3.04f},
                {12.94f,  1.95f,  36.86f,  0.73f,  0.46f,  2.99f},
                {-5.35f,  10.90f,  42.29f,  -0.03f,  0.70f,  3.50f}};

        INDArray result = calculateA(myArray);
        double[][][] result2 = new double[3][4][4];
        for (int i = 0; i < 3; i++) for (int j = 0; j < 4; j++)
            for (int k = 0; k < 4; k++) result2[i][j][k] = result.get(NDArrayIndex.point(i), NDArrayIndex.point(j), NDArrayIndex.point(k)).getDouble(0);

// print them out in a comparable way using JSON:
        for (int i = 0; i < 3; i++) {
            System.out.println(gson.toJson(calculateA((data[i]))).replace("],", "]\n"));
            System.out.println("compared to:");
            System.out.println(gson.toJson(result2[i]).replace("],", "]\n"));
            System.out.println();
        }

    }
}

