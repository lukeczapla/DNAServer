package edu.dnatools.utils;

import com.google.common.io.CharSource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.basepairs.TertiaryBasePairParameters;
import edu.dnatools.model.Job;
import org.apache.commons.io.FileUtils;
import org.biojava.nbio.structure.Structure;
import edu.dnatools.basepairs.BasePairParameters;
import edu.dnatools.basepairs.MismatchedBasePairParameters;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Matrix4d;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by luke on 6/13/17.
 */
public class RefTools {

    public static Logger log = LoggerFactory.getLogger(RefTools.class);
    public static Gson gson = new GsonBuilder().create();

    public static String rootfolder = "jobs/";

    public static String getParameters(String pdbfile) {
        try {
            PDBFileReader pdbFileReader = new PDBFileReader();
            InputStream targetStream =
                    new ReaderInputStream(CharSource.wrap(pdbfile).openStream());
            Structure structure = pdbFileReader.getStructure(targetStream);
            BasePairParameters bp = new MismatchedBasePairParameters(structure, false, false, false).analyze();
            try {
                bp.getLength();
            } catch (IllegalArgumentException e) {
                bp = new TertiaryBasePairParameters(structure, true, false).analyze();
            }
            double[][] output = new double[bp.getPairingParameters().length][12];
            double[][] pairingParameters = bp.getPairingParameters();
            double[][] stepParameters = bp.getStepParameters();
            for (int i = 0; i < bp.getPairingParameters().length; i++) {
                for (int j = 0; j < 6; j++) {
                    output[i][j] = pairingParameters[i][j];
                    output[i][6+j] = stepParameters[i][j];
                }
            }
            return gson.toJson(output);
        } catch (IOException e) {
            return null;
        }
    }

    public static String analyzeReferenceFrames(String pdbfile) {
        try {
            PDBFileReader pdbFileReader = new PDBFileReader();
            InputStream targetStream =
                    new ReaderInputStream(CharSource.wrap(pdbfile).openStream());
            Structure structure = pdbFileReader.getStructure(targetStream);
            BasePairParameters bp = new MismatchedBasePairParameters(structure, false, false, false).analyze();
            try {
                bp.getLength();
            } catch (IllegalArgumentException e) {
                bp = new TertiaryBasePairParameters(structure, true, false).analyze();
            }
            double[][][] refFrames = new double[bp.getPairingParameters().length][4][4];
            for (int i = 0; i < bp.getPairingParameters().length; i++) {
                Matrix4d m = bp.getReferenceFrames().get(i);
                for (int j = 0; j < 4; j++) for (int k = 0; k < 4; k++) {
                    refFrames[i][j][k] = m.getElement(j,k);
                }
            }
            return gson.toJson(refFrames);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Reads 3DNA ref_frames.dat and returns a matrix of 4 x 4 matrices for each frame
     * @param data Contents of ref_frames.dat file as a String
     * @return {nbp} by 4 by 4 matrix of reference frames
     */
    public static String readRefToJSON(String data) {

        double[][][] A = null;
        String[] lines = data.split("\\r?\\n");
        String header = lines[0];

        int nbp;
        try {
            nbp = Integer.parseInt(header.split("\\s+")[1]);
            log.debug(header + "" + nbp + "");
            A = new double[nbp][4][4];
            for (int i = 0; i < nbp; i++) {
                String[] xyz = lines[2 + 5 * i].split("\\s+");
                String[] l1 = lines[3 + 5 * i].split("\\s+");
                String[] l2 = lines[4 + 5 * i].split("\\s+");
                String[] l3 = lines[5 + 5 * i].split("\\s+");
                for (int j = 0; j < 3; j++) {
                    A[i][j][0] = Double.parseDouble(l1[j+1]);
                    A[i][j][1] = Double.parseDouble(l2[j+1]);
                    A[i][j][2] = Double.parseDouble(l3[j+1]);
                    A[i][j][3] = Double.parseDouble(xyz[j+1]);
                }
                A[i][3][3] = 1.0;
            }
        } catch (NumberFormatException e) {
            log.debug("Error converting numbers");
            log.debug(e.getMessage());
            return null;
        }

        return gson.toJson(A);
    }

    /**
     *
     * @param filename The ref_frames.dat file to turn into JSON matrix
     * @return {nbp} by 4 by 4 matrix of reference frames
     */
    public static String readRefFileToJSON(String filename) {
        File f = new File(filename);
        try {
            if (f.exists()) {
                String s = FileUtils.readFileToString(f);
                log.debug(s);
                return readRefToJSON(s);
            }
        } catch (IOException e) {
            log.debug("IOException readRef");

            return null;
        }
        log.debug("No data readRef");
        return null;
    }

    public static String readStepsToJSON(String filename) {
        File f = new File(filename);
        try {
            if (f.exists()) {
                String s = FileUtils.readFileToString(f);
                String[] lines = s.split("\\r?\\n");
                int nbp = Integer.parseInt(lines[0].split("\\s+")[1]) - 1;
                double[][] tps = new double[nbp][6];
                for (int i = 0; i < nbp; i++) {
                    String[] line = lines[i+4].split("\\s+");
                    tps[i][0] = Double.parseDouble(line[4]);
                    tps[i][1] = Double.parseDouble(line[5]);
                    tps[i][2] = Double.parseDouble(line[6]);
                    tps[i][3] = Double.parseDouble(line[1]);
                    tps[i][4] = Double.parseDouble(line[2]);
                    tps[i][5] = Double.parseDouble(line[3]);
                }
                return gson.toJson(tps);
            }
        } catch (IOException e) {
            log.debug("IOException readSteps");
            return null;
        }
        return null;
    }

    /**
     *
     * @param tp the step parameters
     * @return The 4x4 matrix (SE(3) space) representing the step
     */
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

    public static double[] calculatetp(double[][] A) {

        double[] M = new double[6];

        double cosgamma, gamma, phi, omega, sgcp, omega2_minus_phi,
                sm, cm, sp, cp, sg, cg;

        cosgamma = A[2][2];
        if (cosgamma > 1.0) cosgamma = 1.0;
        else if (cosgamma < -1.0) cosgamma = -1.0;

        gamma = acos(cosgamma);

        sgcp = A[1][1]*A[0][2]-A[0][1]*A[1][2];

        if (gamma == 0.0) omega = -atan2(A[0][1],A[1][1]);
        else omega = atan2(A[2][1]*A[0][2]+sgcp*A[1][2],sgcp*A[0][2]-A[2][1]*A[1][2]);

        omega2_minus_phi = atan2(A[1][2],A[0][2]);

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

        M[3] = (cm*cg*cp-sm*sp)*A[0][3]+(sm*cg*cp+cm*sp)*A[1][3]-sg*cp*A[2][3];
        M[4] = (-cm*cg*sp-sm*cp)*A[0][3]+(-sm*cg*sp+cm*cp)*A[1][3]+sg*sp*A[2][3];
        M[5] = (cm*sg)*A[0][3]+(sm*sg)*A[1][3]+cg*A[2][3];

        return M;

    }


    public static double[][] multiply(double[][] a, double[][] b) {
        if (a == null || b == null) return null;
        if (a[0].length != b.length) return null;
        double[][] c = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < b[0].length; j++) {
                c[i][j] = 0.0;
                for (int k = 0; k < a[0].length; k++) {
                    c[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return c;
    }



    public static double[][] invert(double[][] a) {

        double[][] R = new double[4][4];

        double A = a[0][0];
        double B = a[0][1];
        double C = a[0][2];
        double D = a[0][3];
        double E = a[1][0];
        double F = a[1][1];
        double G = a[1][2];
        double H = a[1][3];
        double I = a[2][0];
        double J = a[2][1];
        double K = a[2][2];
        double L = a[2][3];
        double denom = -C*F*I + B*G*I + C*E*J - A*G*J - B*E*K + A*F*K;

        R[0][0] = (-G*J+F*K)/denom;
        R[0][1] = (C*J-B*K)/denom;
        R[0][2] = (-C*F+B*G)/denom;
        R[0][3] = (D*G*J-C*H*J-D*F*K+B*H*K+C*F*L-B*G*L)/denom;
        R[1][0] = (G*I-E*K)/denom;
        R[1][1] = (-C*I+A*K)/denom;
        R[1][2] = (C*E-A*G)/denom;
        R[1][3] = (-D*G*I+C*H*I+D*E*K-A*H*K-C*E*L+A*G*L)/denom;
        R[2][0] = (-F*I+E*J)/denom;
        R[2][1] = (B*I-A*J)/denom;
        R[2][2] = (-B*E+A*F)/denom;
        R[2][3] = (D*F*I-B*H*I-D*E*J+A*H*J+B*E*L-A*F*L)/denom;
        R[3][0] = 0.0;
        R[3][1] = 0.0;
        R[3][2] = 0.0;
        R[3][3] = 1.0;

        return R;

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
                if (i == j) v[i][j] = 1.0;
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

        log.info("could not solve eigenvectors of matrix in 500 iterations");
        return null;

    }



}
