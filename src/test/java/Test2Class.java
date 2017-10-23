
import edu.dnatools.calculate.Covariance;
import org.apache.tomcat.util.buf.ByteBufferUtils;
import org.junit.Test;
import static org.junit.Assert.*;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dimensionalityreduction.PCA;
import org.nd4j.linalg.eigen.Eigen;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.inverse.InvertMatrix;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.string.NDArrayStrings;
import org.nd4j.linalg.util.ArrayUtil;



/**
 * Created by luke on 7/8/17.
 * This is a comparison between ND4J symmetricGeneralizedEigenvalues(A) and
 * a standard implemention from Numerical Recipes.
 * Note one eigenvector is the opposite of one of the others, and it's just
 * implementation-specific and does not make either any different!
 * The Numerical Recipes version does not sort the eigenvalues (and corresponding
 * eigenvectors) by eigenvalue.
 *
 * Also some tests were done in solving A x = L B x as well
 */
public class Test2Class {

    private static NDArrayStrings ns = new NDArrayStrings(5);

    @Test
    public void mpowtest() {

        NDArrayStrings ns = new NDArrayStrings(5);
        INDArray val = Nd4j.eye(4).muli(4.0);
        System.out.println(ns.format(Covariance.mpow(val, 5, true)) + "\n" + ns.format(val));
    }

    @Test
    public void testPCA() {
        INDArray m = Nd4j.randn(10000, 16);
        m.getColumn(0).muli(4.84);
        m.getColumn(1).muli(4.84);
        m.getColumn(2).muli(4.09);
        m.getColumn(1).addi(m.getColumn(2).div(2.0));
        m.getColumn(2).addi(34.286);
        m.getColumn(1).addi(m.getColumn(4));
        m.getColumn(4).subi(m.getColumn(5).div(2.0));
        m.getColumn(5).addi(3.4);
        m.getColumn(6).muli(6.0);
        m.getColumn(7).muli(0.2);
        m.getColumn(8).muli(2.0);
        m.getColumn(9).muli(6.0);
        m.getColumn(9).addi(m.getColumn(6).mul(1.0));
        m.getColumn(10).muli(0.2);
        m.getColumn(11).muli(2.0);
        m.getColumn(12).muli(0.2);
        m.getColumn(13).muli(4.0);
        m.getColumn(14).muli(3.2);
        m.getColumn(14).addi(m.getColumn(2).mul(1.0)).subi(m.getColumn(13).div(2.0));
        m.getColumn(15).muli(1.0);
        PCA myPCA = new PCA(m);
        INDArray reduced70 = myPCA.reducedBasis(0.70);
        INDArray reduced90 = myPCA.reducedBasis(0.90);
        INDArray reduced99 = myPCA.reducedBasis(0.99);
        assertTrue("Major variance differences should change number of basis vectors", reduced90.columns() > reduced70.columns());
        INDArray reduced100 = myPCA.reducedBasis(1.0);
        assertTrue("100% variance coverage should include all eigenvectors", reduced100.columns() == m.columns());
        NDArrayStrings ns = new NDArrayStrings(5);
        System.out.println("Eigenvectors:\n" + ns.format(myPCA.getEigenvectors()));
        System.out.println("Eigenvalues:\n" + ns.format(myPCA.getEigenvalues()));
        System.out.println("Mean values:\n" + ns.format(myPCA.getMean()));
        double error = 0.0;
        for (int i = 0; i < 10000; i++)
            error += myPCA.estimateVariance(m.getRow(i), reduced90.columns());
        error /= 10000.0;
        System.out.println("Fractional variance using 90% variance with " + reduced90.columns() + " columns: " + error);
        assertTrue("Variance does not cover intended 90% variance", error > 0.90);
    }

    @Test
    public void testCovariance() {
        INDArray m = Nd4j.randn(100000, 6);
        m.getColumn(0).muli(4.84);
        m.getColumn(1).muli(4.84);
        m.getColumn(2).muli(4.09);
        m.getColumn(1).addi(m.getColumn(2).div(2.0));
        m.getColumn(2).addi(34.286);
        m.getColumn(3).muli(1.0/2.0);
        m.getColumn(4).muli(1.0/2.0);
        m.getColumn(5).muli(1.0/2.0);
        m.getColumn(1).addi(m.getColumn(5).mul(10.0));
        m.getColumn(4).subi(m.getColumn(5).div(2.0));
        m.getColumn(5).addi(3.4);

        INDArray[] data = Covariance.covarianceMatrix(m);
        System.out.println(ns.format(data[0])+"\n");
        System.out.println(ns.format(data[1])+"\n");
        System.out.println(ns.format(InvertMatrix.invert(data[0], false)));
        INDArray[] result = Covariance.principalComponents(data[0]);
        System.out.println(ns.format(result[0]));
        System.out.println(ns.format(result[1]));
        INDArray x = m.getRow(0).sub(data[1]);
        System.out.println("Analyzing " + ns.format(x));
        INDArray transformed = result[0].transpose().mmul(x.reshape(6,1));
        INDArray t2 = Transforms.pow(transformed, 2, true);
        System.out.println((Math.sqrt(t2.getRows(0,1,2).sumNumber().doubleValue()) / Math.sqrt(t2.sumNumber().doubleValue())));
        System.out.println("Trying the variance method");
        INDArray r = Covariance.pca(m, 0.95);
        System.out.println("RESULT:\n"+ns.format(r));
        //assert result[0].columns() == 6 && result[0].rows() == 6 && result[1].columns() == 6;
    }
    // test of functions added by Luke Czapla
    // Compares solution of A x = L x  to solution to A x = L B x when it is simple
    @Test
    public void test2Syev() {
        double[][] matrix = new double[][] {{0.0427, -0.04, 0, 0, 0, 0}, {-0.04, 0.0427, 0, 0, 0, 0}, {0, 0.00, 0.0597, 0, 0, 0}, {0, 0, 0, 50, 0, 0}, {0, 0, 0, 0, 50, 0}, {0, 0, 0, 0, 0, 50}};
        INDArray m = Nd4j.create(ArrayUtil.flattenDoubleArray(matrix), new int[] {6,6});

        INDArray res = Eigen.symmetricGeneralizedEigenvalues(m, true);

        INDArray n = Nd4j.create(ArrayUtil.flattenDoubleArray(matrix), new int[] {6,6});
        INDArray res2 = Eigen.symmetricGeneralizedEigenvalues(n, Nd4j.eye(6).mul(2.0), true);

        for (int i = 0; i < 6; i++) {
            assertEquals(res.getDouble(i), 2*res2.getDouble(i), 0.000001);
        }

    }


    @Test
    public void calculateEigen() {

        double[] eigenvalues = new double[6];
        double[][] matrix = new double[][] {{0.0427, -0.04, 0, 0, 0, 0}, {-0.04, 0.0427, 0, 0, 0, 0}, {0, 0.00, 0.0597, 0, 0, 0}, {0, 0, 0, 50, 0, 0}, {0, 0, 0, 0, 50, 0}, {0, 0, 0, 0, 0, 50}};
        double[][] result = jeigen(matrix, eigenvalues);

        INDArray m = Nd4j.create(ArrayUtil.flattenDoubleArray(matrix), new int[] {6,6});
        INDArray res = Eigen.symmetricGeneralizedEigenvalues(m,true);

        // dump out the results and compare side by side
        for (int i = 0; i < 6; i++) {
            System.out.println(eigenvalues[i] + " " + res.getDouble(i));
            System.out.println();
            for (int j = 0; j < 6; j++) {
                System.out.println(result[j][i] + " " + m.get(NDArrayIndex.point(j), NDArrayIndex.point(i)).getDouble(0));
            }
            System.out.println();
        }

        m = Nd4j.create(ArrayUtil.flattenDoubleArray(matrix), new int[] {6,6});
        res = Eigen.symmetricGeneralizedEigenvalues(m, Nd4j.eye(6).muli(2), true);

        // output this as well... etc.

    }



    public static double[][] jeigen(double[][] a, double[] d) {

        if (a.length != a[0].length) return null;

        int ip, iq, i, j;
        double tresh, theta, tau, t, sm, s, h, g, c;

        double[][] x = new double[a.length][a.length];
        double[][] v = new double[a.length][a.length];
        //let v = a.slice();
        for (i = 0; i < a.length; i++) {
            for (j = 0; j < a.length; j++) {
                if (i == j) v[i][j] = 1.0; else v[i][j] = 0.0;
                x[i][j] = a[i][j];
            }
        }
        //let x = numeric.eye(a.length);

        double[] b = new double[6];
        double[] z = new double[6];

        tresh = 0.0;

        for (ip = 0; ip < a.length; ip++) {
            d[ip]=x[ip][ip];
            b[ip]=d[ip];
            z[ip]=0.0;
        }

        for (i = 0; i < 500; i++) {
            //  x.writematrix(stdout);
            sm = 0.0;
            for(ip=0; ip < a.length-1; ip++)
                for (iq=ip+1; iq < a.length; iq++) sm += Math.abs(x[ip][iq]);
            if (sm == 0.0) {
                return v;
            }

            for (ip = 0; ip < a.length-1; ip++) {
                for (iq = ip+1; iq < a.length; iq++) {
                    g = 100.0*Math.abs(x[ip][iq]);
                    if (Math.abs(x[ip][iq]) > tresh) {

                        h = d[iq]-d[ip];

                        if ((Math.abs(h)+g) == Math.abs(h)) {
                            if (h != 0.0)
                                t = (x[ip][iq])/h;
                            else t = 0.0;
                        } else {
                            if (x[ip][iq] != 0.0) theta = 0.5*h/x[ip][iq];
                            else theta = 0.0;
                            t = 1.0/(Math.abs(theta)+Math.sqrt(1.0+theta*theta));
                            if (theta < 0.0) t = -t;
                        }

                        c = 1.0/Math.sqrt(1.0+t*t);
                        s = t*c;
                        tau = s/(1.0+c);
                        h = t*x[ip][iq];
                        z[ip] -= h;
                        z[iq] += h;
                        d[ip] -= h;
                        d[iq] += h;

                        x[ip][iq] = 0.0;
                        for (j = 0; j <= ip-1; j++) {
                            g=x[j][ip];
                            h=x[j][iq];
                            x[j][ip]=g-s*(h+g*tau);
                            x[j][iq]=h+s*(g-h*tau);
                        }
                        for (j = ip+1; j <= iq-1; j++) {
                            g=x[ip][j];
                            h=x[j][iq];
                            x[ip][j]=g-s*(h+g*tau);
                            x[j][iq]=h+s*(g-h*tau);
                        }
                        for (j = iq+1; j < a.length; j++) {
                            g=x[ip][j];
                            h=x[iq][j];
                            x[ip][j]=g-s*(h+g*tau);
                            x[iq][j]=h+s*(g-h*tau);
                        }
                        for (j = 0; j < a.length; j++) {
                            g=v[j][ip];
                            h=v[j][iq];
                            v[j][ip]=g-s*(h+g*tau);
                            v[j][iq]=h+s*(g-h*tau);
                        }

                    }
                }
            }
            for (ip=0; ip < a.length; ip++) {
                b[ip] += z[ip];
                d[ip] = b[ip];
                z[ip] = 0.0;
            }
        }

        System.out.println("could not solve eigenvectors of matrix in 500 iterations");
        return null;

    }



}
