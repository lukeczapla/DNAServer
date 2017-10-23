package edu.dnatools.pdbanalysis;

import edu.dnatools.calculate.NDDNA;
import edu.dnatools.utils.RefTools;
import org.biojava.nbio.structure.*;
import org.biojava.nbio.structure.align.pairwise.AlternativeAlignment;
import org.biojava.nbio.structure.align.pairwise.FragmentPair;
import org.biojava.nbio.structure.geometry.SuperPosition;
import org.biojava.nbio.structure.geometry.SuperPositionQCP;
import org.biojava.nbio.structure.geometry.SuperPositionQuat;
import org.biojava.nbio.structure.io.PDBFileReader;
import org.biojava.nbio.structure.jama.Matrix;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.inverse.InvertMatrix;
import org.nd4j.linalg.string.NDArrayStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by luke on 7/13/17.
 */

public class PDBInformation {

    private static String[] standardBases = new String[] {
            "SEQRES   1 A    1  A\n" +
                    "ATOM      2  N9    A A   1      -1.291   4.498   0.000\n" +
                    "ATOM      3  C8    A A   1       0.024   4.897   0.000\n" +
                    "ATOM      4  N7    A A   1       0.877   3.902   0.000\n" +
                    "ATOM      5  C5    A A   1       0.071   2.771   0.000\n" +
                    "ATOM      6  C6    A A   1       0.369   1.398   0.000\n" +
                    "ATOM      8  N1    A A   1      -0.668   0.532   0.000\n" +
                    "ATOM      9  C2    A A   1      -1.912   1.023   0.000\n" +
                    "ATOM     10  N3    A A   1      -2.320   2.290   0.000\n" +
                    "ATOM     11  C4    A A   1      -1.267   3.124   0.000\n" +
                    "END",
            "SEQRES   1 A    1  G\n" +
                    "ATOM      2  N9    G A   1      -1.289   4.551   0.000\n" +
                    "ATOM      3  C8    G A   1       0.023   4.962   0.000\n" +
                    "ATOM      4  N7    G A   1       0.870   3.969   0.000\n" +
                    "ATOM      5  C5    G A   1       0.071   2.833   0.000\n" +
                    "ATOM      6  C6    G A   1       0.424   1.460   0.000\n" +
                    "ATOM      8  N1    G A   1      -0.700   0.641   0.000\n" +
                    "ATOM      9  C2    G A   1      -1.999   1.087   0.000\n" +
                    "ATOM     11  N3    G A   1      -2.342   2.364   0.001\n" +
                    "ATOM     12  C4    G A   1      -1.265   3.177   0.000\n" +
                    "END",
            "SEQRES   1 A    1  T\n" +
                    "ATOM      2  N1    T A   1      -1.284   4.500   0.000\n" +
                    "ATOM      3  C2    T A   1      -1.462   3.135   0.000\n" +
                    "ATOM      5  N3    T A   1      -0.298   2.407   0.000\n" +
                    "ATOM      6  C4    T A   1       0.994   2.897   0.000\n" +
                    "ATOM      8  C5    T A   1       1.106   4.338   0.000\n" +
                    "ATOM     10  C6    T A   1      -0.024   5.057   0.000\n" +
                    "END",
            "SEQRES   1 A    1  C\n" +
                    "ATOM      2  N1    C A   1      -1.285   4.542   0.000\n" +
                    "ATOM      3  C2    C A   1      -1.472   3.158   0.000\n" +
                    "ATOM      5  N3    C A   1      -0.391   2.344   0.000\n" +
                    "ATOM      6  C4    C A   1       0.837   2.868   0.000\n" +
                    "ATOM      8  C5    C A   1       1.056   4.275   0.000\n" +
                    "ATOM      9  C6    C A   1      -0.023   5.068   0.000\n" +
                    "END"
    };

    public static Logger log = LoggerFactory.getLogger(PDBInformation.class);

    public double[] pairings;
    private static String[] baseListDNA = {"A", "G", "T", "C"};
    private static String[] baseListRNA = {"A", "G", "U", "C"};
    private static Map<String, Integer> map;
    private static Map<Integer, List<String>> ringMap;
    private static Map<Integer, List<String>> innerMap;
    static {
        map = new HashMap<>();
        map.put("DA", 0); map.put("ADE", 0); map.put("A", 0);
        map.put("DG", 1); map.put("GUA", 1); map.put("G", 1);
        map.put("DT", 2); map.put("THY", 2); map.put("T", 2); //RNA lines: map.put("U", 2); map.put("URA", 2);
        map.put("DC", 3); map.put("CYT", 3); map.put("C", 3);
        // chemically modified bases, leaving out right now.
        //map.put("DZM", 0);
        //map.put("UCL", 2);
        //map.put("2DT", 2);
        //map.put("1CC", 3); map.put("5CM", 3);
        ringMap = new HashMap<>();
        ringMap.put(0, Arrays.asList("C8", "C2", "N3", "C4", "C5", "C6", "N7", "N1", "N9"));
        ringMap.put(1, Arrays.asList("C8", "C2", "N3", "C4", "C5", "C6", "N7", "N1", "N9"));
        ringMap.put(2, Arrays.asList("C6", "C2", "N3", "C4", "C5", "N1"));
        ringMap.put(3, Arrays.asList("C6", "C2", "N3", "C4", "C5", "N1"));
        innerMap = new HashMap<>();
        innerMap.put(0, Arrays.asList("C4", "C5", "C6", "N1", "C2", "N3"));
        innerMap.put(1, Arrays.asList("C4", "C5", "C6", "N1", "C2", "N3"));
        innerMap.put(2, Arrays.asList("C6", "C5", "C4", "N3", "C2", "N1"));
        innerMap.put(3, Arrays.asList("C6", "C5", "C4", "N3", "C2", "N1"));
    }

    private String pdbId = "";
    private String pairSequence = "";

    private Structure structure;

    public PDBInformation(String filename) {
        PDBFileReader pdbFileReader = new PDBFileReader();
        pdbFileReader.setPath(".");  // was originally "/Users/luke/DNAServer"
        try {
            structure = pdbFileReader.getStructure(filename);
        } catch (IOException e) {
            log.info("Error reading file");
            structure = null;
        }
    }

    public PDBInformation(String pdbId, boolean dl) {
        //PDBFileReader pdbFileReader = new PDBFileReader();
        this.pdbId = pdbId;
        try {
            structure = StructureIO.getStructure(pdbId);
        } catch (StructureException|IOException e) {
            log.info("Cannot download file");
            structure = null;
        }
    }

    public void generalInformation() {
        if (structure == null) return;
        List<Chain> chains = structure.getChains();
        for (Chain c: chains) {
            EntityInfo ei = c.getEntityInfo();
            System.out.println(ei.getDescription());
            if (c.getSeqResLength() > 0) {
                System.out.println("Chains: " + c.getSeqResSequence() + " " + c.getName() + " " + c.getId());
            }
        }

    }

    public List<Chain> getNucleicChains(boolean removeDups) {
        if (structure == null) return new ArrayList<>();
        List<Chain> chains = structure.getChains();
        List<Chain> result = new ArrayList<>();
        for (Chain c: chains) {
            //EntityInfo ei = c.getEntityInfo();
            if (c.isNucleicAcid()) {
                result.add(c);
            }
            //result.add(c);
        }
        if (removeDups) for (int i = 0; i < result.size(); i++) {
            for (int j = i+2; j < result.size(); j++) {
                // remove double
                if (result.get(i).getSeqResSequence().equals(result.get(j).getSeqResSequence())) {
                    result.remove(j);
                }
            }
        }
        return result;
    }

    public List<Group[]> findPairs(List<Chain> chains) {
        List<Group[]> result = new ArrayList<>();
        for (int i = 0; i < chains.size(); i++) {
            Chain c = chains.get(i);
            for (int j = i+1; j < chains.size(); j++) {
                String complement = complement(chains.get(j).getSeqResSequence(), false);
                String match = longestCommonSubstring(c.getSeqResSequence(), complement);
                //System.out.println(c.getSeqResSequence() + " " + chains.get(j).getSeqResSequence() + " " + match);
                int index1 = c.getSeqResSequence().indexOf(match);
                int index2 = complement.length() - complement.indexOf(match) - 1;
                for (int k = 0; k < match.length(); k++) {
                    Group g1 = c.getSeqResGroup(index1+k);
                    Group g2 = chains.get(j).getSeqResGroup(index2-k);
                    System.out.println(g1.getPDBName() + " " + g2.getPDBName());
                    Integer type1 = map.get(g1.getPDBName());
                    Integer type2 = map.get(g2.getPDBName());
                    if (type1 == null || type2 == null) {
                        if (pairSequence.length() != 0 && pairSequence.charAt(pairSequence.length()-1) != ' ') pairSequence += ' ';
                        continue;
                    }
                    Atom a1 = g1.getAtom(ringMap.get(type1).get(0));
                    Atom a2 = g2.getAtom(ringMap.get(type2).get(0));

                    if (a1 == null) {
                        System.out.println("Error processing " + g1.getPDBName() + " in " + pdbId);
                        if (pairSequence.length() != 0 && pairSequence.charAt(pairSequence.length()-1) != ' ') pairSequence += ' ';
                        continue;
                    }
                    if (a2 == null) {
                        System.out.println("Error processing " + g2.getPDBName() + " in " + pdbId);
                        if (pairSequence.length() != 0 && pairSequence.charAt(pairSequence.length()-1) != ' ') pairSequence += ' ';
                        continue;
                    }

                    double dx = a1.getX()-a2.getX();
                    double dy = a1.getY()-a2.getY();
                    double dz = a1.getZ()-a2.getZ();
                    double distance = Math.sqrt(dx*dx+dy*dy+dz*dz);
                    System.out.println("C8-C6 Distance (Å): " + distance);
                    // could be a base pair
                    if (Math.abs(distance-10.0) < 2.5) {
                        boolean valid = true;
                        for (String atomname : ringMap.get(type1)) {
                            Atom a = g1.getAtom(atomname);
                            if (a == null) valid = false;
                        }
                        if (valid) for (String atomname: ringMap.get(type2)) {
                            Atom a = g2.getAtom(atomname);
                            if (a == null) valid = false;
                        }
                        if (valid) {
                            Group g3 = null;
                            Group g4 = null;
                            if (k + 1 < match.length()) g3 = c.getSeqResGroup(index1 + k + 1);
                            if (k != 0) g4 = c.getSeqResGroup(index1 + k - 1);
                            result.add(new Group[]{g1, g2, g3, g4});
                            pairSequence += c.getSeqResSequence().charAt(index1 + k);
                        } else if (pairSequence.length() != 0 && pairSequence.charAt(pairSequence.length()-1) != ' ') pairSequence += ' ';
                    } else if (pairSequence.length() != 0 && pairSequence.charAt(pairSequence.length()-1) != ' ') pairSequence += ' ';
                }
                if (pairSequence.length() != 0 && pairSequence.charAt(pairSequence.length()-1) != ' ') pairSequence += ' ';
            }
            //System.out.println();
        }
        System.out.println("Matched: " + pairSequence);
        return result;
    }

    public INDArray basePairReferenceFrame2(Group[] pair) {
        Integer type1 = map.get(pair[0].getPDBName());
        Integer type2 = map.get(pair[1].getPDBName());
        SuperPosition sp = new SuperPositionQCP(true);
        if (type1 == null || type2 == null) return null;
        String pairString = baseListDNA[type1] + baseListDNA[type2];
        PDBFileReader pdbFileReader = new PDBFileReader();
        Structure s1, s2;
        try {
            s1 = pdbFileReader.getStructure(new ByteArrayInputStream(standardBases[type1].getBytes()));
            s2 = pdbFileReader.getStructure(new ByteArrayInputStream(standardBases[type2].getBytes()));
            //s1 = pdbFileReader.getStructure(baseListDNA[type1] + ".pdb");
            //s2 = pdbFileReader.getStructure(baseListDNA[type2] + ".pdb");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Group std1 = s1.getChain("A").getAtomGroup(0);
        Group std2 = s2.getChain("A").getAtomGroup(0);

        Point3d[] pointref = new Point3d[std1.getAtoms().size()];
        Point3d[] pointact = new Point3d[std1.getAtoms().size()];
        int count = 0;

        for (Atom a : std1.getAtoms()) {
            if (pair[0].getAtom(a.getName()) == null) {
                System.out.println("Missing: " + a.getName());
                return null;
            }
            pointref[count] = a.getCoordsAsPoint3d();
            pointact[count] = pair[0].getAtom(a.getName()).getCoordsAsPoint3d();
            count++;
        }
        assert count == std1.getAtoms().size();
        Matrix4d ref1 = (Matrix4d)sp.superpose(pointact, pointref).clone();

        pointref = new Point3d[std2.getAtoms().size()];
        pointact = new Point3d[std2.getAtoms().size()];

        count = 0;
        for (Atom a : std2.getAtoms()) {
            if (pair[1].getAtom(a.getName()) == null) {
                System.out.println("Missing: " + a.getName());
                return null;
            }
            pointref[count] = a.getCoordsAsPoint3d();
            pointact[count] = pair[1].getAtom(a.getName()).getCoordsAsPoint3d();
            count++;
        }
        assert count == std2.getAtoms().size();

    //    System.out.println(ref1);
        Matrix4d temp = (Matrix4d)ref1.clone();
        Matrix4d temp2 = (Matrix4d)temp.clone();
        Matrix4d ref2 = (Matrix4d)sp.superpose(pointact, pointref).clone();
    //    System.out.println(ref2);
        double[][] v = new double[3][4];
        double[] y3 = new double[4];
        double[] z3 = new double[4];
        ref2.getColumn(1, y3);
        ref2.getColumn(2, z3);
        for (int i = 0; i < 4; i++) {
            y3[i] *= -1.0;
            z3[i] *= -1.0;
        }
        ref2.setColumn(1, y3);
        ref2.setColumn(2, z3);
        temp.add(ref2);
        temp.mul(0.5);

        temp.getColumn(1, v[1]);
        temp.getColumn(2, v[2]);

        double r = Math.sqrt(v[2][0]*v[2][0]+v[2][1]*v[2][1]+v[2][2]*v[2][2]);
        for (int j = 0; j < 3; j++) {
            v[2][j] /= r;
        }

        r = v[2][0] * v[1][0] + v[2][1] * v[1][1] + v[2][2] * v[1][2];
        for (int j = 0; j < 3; j++) {
            v[1][j] -= v[2][0]*r + v[2][1]*r + v[2][2]*r;
        }
        r = Math.sqrt(v[1][0]*v[1][0]+v[1][1]*v[1][1]+v[1][2]*v[1][2]);
        for (int j = 0; j < 3; j++) {
            v[1][j] /= r;
        }
        v[0][0] = v[1][1]*v[2][2] - v[1][2]*v[2][1];
        v[0][1] = v[1][2]*v[2][0] - v[1][0]*v[2][2];
        v[0][2] = v[1][0]*v[2][1] - v[1][1]*v[2][0];

        temp.setColumn(0, v[0]);
        temp.setColumn(1, v[1]);

        temp2.invert();
        temp2.mul(ref2);
// pairing parameters
        double[][] Atmp = new double[4][4];
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) Atmp[i][j] = temp2.getElement(i,j);
        pairings = RefTools.calculatetp(Atmp);
    //    for (int i = 0; i < 6; i++)
      //      System.out.print(-pairings[i]+ " ");
     //   System.out.println();

        INDArray result = Nd4j.create(4, 4);
        for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) result.getRow(i).getColumn(j).assign(temp.getElement(i, j));
        return result;


    }

    public Matrix4d basePairReferenceFrame(Group[] pair) {
        Integer type1 = map.get(pair[0].getPDBName());
        Integer type2 = map.get(pair[1].getPDBName());
        if (type1 == null || type2 == null) return null;
        String pairString = baseListDNA[type1] + baseListDNA[type2];
        PDBFileReader pdbFileReader = new PDBFileReader();
        Structure s;
        try {
            s = pdbFileReader.getStructure(pairString + ".pdb");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Group std1 = s.getChain("A").getAtomGroup(0);
        Group std2 = s.getChain("B").getAtomGroup(0);

        Point3d[] pointref = new Point3d[std1.getAtoms().size()+std2.getAtoms().size()];
        Point3d[] pointact = new Point3d[std1.getAtoms().size()+std2.getAtoms().size()];
        int count = 0;

        for (Atom a : std1.getAtoms()) {
            if (pair[0].getAtom(a.getName()) == null) return null;
            pointref[count] = a.getCoordsAsPoint3d();
            pointact[count] = pair[0].getAtom(a.getName()).getCoordsAsPoint3d();
            count++;
        }
        for (Atom a : std2.getAtoms()) {
            if (pair[1].getAtom(a.getName()) == null) return null;
            pointref[count] = a.getCoordsAsPoint3d();
            pointact[count] = pair[1].getAtom(a.getName()).getCoordsAsPoint3d();
            count++;
        }
        assert count == std1.getAtoms().size()+std2.getAtoms().size();

        SuperPosition sp = new SuperPositionQCP(true);
        return sp.superposeAndTransform(pointact, pointref);

    }

    public INDArray[] basePairParameters(Group[] pair) {
        int type1 = map.get(pair[0].getPDBName());
        int type2 = map.get(pair[1].getPDBName());
        double[] x1 = new double[ringMap.get(type1).size()];
        double[] y1 = new double[ringMap.get(type1).size()];
        double[] z1 = new double[ringMap.get(type1).size()];
        double nx1 = 0, ny1 = 0, nz1 = 0, nx2 = 0, ny2 = 0, nz2 = 0;
        double avgx = 0, avgy = 0, avgz = 0;
        int count = 0;
        for (String atomname : ringMap.get(type1)) {
            Atom a = pair[0].getAtom(atomname);
            if (a == null) {
                System.out.println("No atom " + atomname + " on " + pair[0].getPDBName());
                return null;
            }
            x1[count] = a.getX();
            y1[count] = a.getY();
            z1[count] = a.getZ();
            count++;
        }
        // compute mean normal
        Group comp = (pair[2] == null ? pair[3] : pair[2]);
        if (pair[2] == null) System.out.println("Using pair[3] - " + pair[3].getAtom("C6").getZ() + " " + pair[0].getResidueNumber().printFull() + " " + pair[1].getResidueNumber().printFull());
        double xa = 0, ya = 0, za = 0;
        for (String atomname : innerMap.get(type1)) {
            xa += comp.getAtom(atomname).getX();
            ya += comp.getAtom(atomname).getY();
            za += comp.getAtom(atomname).getZ();
        }
        xa /= 6.0; ya /= 6.0; za /= 6.0;

        for (int i = 0; i < count; i++) for (int j = i+1; j < count; j++) for (int k = j+1; k < count; k++) {
            double vx1 = x1[i]-x1[j];
            double vx2 = x1[k]-x1[j];
            double vy1 = y1[i]-y1[j];
            double vy2 = y1[k]-y1[j];
            double vz1 = z1[i]-z1[j];
            double vz2 = z1[k]-z1[j];
            double x = vy1*vz2-vz1*vy2;
            double y = vz1*vx2-vx1*vz2;
            double z = vx1*vy2-vy1*vx2;
            double norm = Math.sqrt(x*x+y*y+z*z);
            x /= norm;  y /= norm; z /= norm;
            double cx = xa-pair[0].getAtom("C6").getX();
            double cy = ya-pair[0].getAtom("C6").getY();
            double cz = za-pair[0].getAtom("C6").getZ();
            if (x*cx+y*cy+z*cz < 0) {
                x = -x;  y = -y;  z = -z;
            }
            //System.out.println("Normal: "+x+" "+y+" "+z);
            nx1 += x;
            ny1 += y;
            nz1 += z;
        }
        double r = Math.sqrt(nx1*nx1+ny1*ny1+nz1*nz1);
        nx1 /= r; ny1 /= r; nz1 /= r;
        count = 0;
        x1 = new double[ringMap.get(type2).size()];
        y1 = new double[ringMap.get(type2).size()];
        z1 = new double[ringMap.get(type2).size()];
        for (String atomname : ringMap.get(type2)) {
            Atom a = pair[1].getAtom(atomname);
            if (a == null) {
                System.out.println("No atom " + atomname + " on " + pair[1].getPDBName());
                return null;
            }
            x1[count] = a.getX();
            y1[count] = a.getY();
            z1[count] = a.getZ();
            count++;
        }
        // compute mean normal
        for (int i = 0; i < count; i++) for (int j = i+1; j < count; j++) for (int k = j+1; k < count; k++) {
            double vx1 = x1[i]-x1[j];
            double vx2 = x1[k]-x1[j];
            double vy1 = y1[i]-y1[j];
            double vy2 = y1[k]-y1[j];
            double vz1 = z1[i]-z1[j];
            double vz2 = z1[k]-z1[j];
            double x = vy1*vz2-vz1*vy2;
            double y = vz1*vx2-vx1*vz2;
            double z = vx1*vy2-vy1*vx2;
            double norm = Math.sqrt(x*x+y*y+z*z);
            x /= norm;  y /= norm; z /= norm;
            double cx = xa-pair[0].getAtom("C6").getX();
            double cy = ya-pair[0].getAtom("C6").getY();
            double cz = za-pair[0].getAtom("C6").getZ();
            if (x*cx+y*cy+z*cz < 0) {
                x = -x;  y = -y;  z = -z;
            }
            //System.out.println("Normal: "+x+" "+y+" "+z);
            nx2 += x;
            ny2 += y;
            nz2 += z;
        }
        r = Math.sqrt(nx2*nx2+ny2*ny2+nz2*nz2);
        nx2 /= r; ny2 /= r; nz2 /= r;
        double nx = (nx1+nx2)/2.0; double ny = (ny1+ny2)/2.0; double nz = (nz1+nz2)/2.0;
        r = Math.sqrt(nx*nx+ny*ny+nz*nz);
        nx /= r; ny /= r; nz /= r;
        if (pair[2] == null) {
            nx *= -1;  ny *= -1;  nz *= -1;
        }
        //System.out.println("Adjusted normal: " + nx + " " + ny + " " + nz);
        Atom a1 = pair[0].getAtom(ringMap.get(type1).get(0));
        Atom a2 = pair[1].getAtom(ringMap.get(type2).get(0));

        double lx = a1.getX()-a2.getX();
        double ly = a1.getY()-a2.getY();
        double lz = a1.getZ()-a2.getZ();
        avgx = a2.getX() + 0.5*(a1.getX()-a2.getX());
        avgy = a2.getY() + 0.5*(a1.getY()-a2.getY());
        avgz = a2.getZ() + 0.5*(a1.getZ()-a2.getZ());

        r = Math.sqrt(lx*lx+ly*ly+lz*lz);
        lx /= r;  ly /= r;  lz /= r;

        double sx = ly*nz-lz*ny;
        double sy = lz*nx-lx*nz;
        double sz = lx*ny-ly*nx;
        r = Math.sqrt(sx*sx+sy*sy+sz*sz);
        sx /= r;  sy /= r;  sz /= r;
        lx = ny*sz-nz*sy;
        ly = nz*sx-nx*sz;
        lz = nx*sy-ny*sx;

        INDArray P = Nd4j.create(new double[] {sx, lx, nx, avgx, sy, ly, ny, avgy, sz, lz, nz, avgz, 0, 0, 0, 1}, new int[] {4,4}, 'c');
        NDArrayStrings ns = new NDArrayStrings(5);
        //System.out.println(ns.format(P));

        // compute the base pairing parameters
        x1 = new double[6]; y1 = new double[6]; z1 = new double[6];
        avgx = 0; avgy = 0; avgz = 0;
        count = 0;
        for (String atomname : innerMap.get(type1)) {
            Atom a = pair[0].getAtom(atomname);
            if (a == null) {
                System.out.println("No atom " + atomname + " on " + pair[1].getPDBName());
                return null;
            }
            x1[count] = a.getX();
            y1[count] = a.getY();
            z1[count] = a.getZ();
            avgx += x1[count]; avgy += y1[count]; avgz += z1[count];
            count++;
        }
        avgx /= (double)count; avgy /= (double)count; avgz /= (double)count;
        sx = (x1[1]+x1[2])/2.0 - avgx; sy = (y1[1]+y1[2])/2.0 - avgy; sz = (z1[1]+z1[2])/2.0 - avgz;
        r = Math.sqrt(sx*sx+sy*sy+sz*sz);
        sx /= r; sy /= r; sz /= r;
        lx = x1[0] - avgx; ly = y1[0] - avgy; lz = z1[0] - avgz;
        r = Math.sqrt(lx*lx+ly*ly+lz*lz);
        lx /= r; ly /= r; lz /= r;

        nx = sy*lz-sz*ly;
        ny = sz*lx-sx*lz;
        nz = sx*ly-sy*lx;
        r = Math.sqrt(nx*nx+ny*ny+nz*nz);
        nx /= r; ny /= r; nz /= r;
        lx = (ny*sz-nz*sy);
        ly = (nz*sx-nx*sz);
        lz = (nx*sy-ny*sx);
        nx *= -1; ny *= -1; nz *= -1;

        INDArray B1 = Nd4j.create(new double[] {sx, nx, lx, avgx, sy, ny, ly, avgy, sz, nz, lz, avgz, 0, 0, 0, 1}, new int[] {4,4}, 'c');
        // compute the base pairing parameters for second base
        x1 = new double[6]; y1 = new double[6]; z1 = new double[6];
        avgx = 0; avgy = 0; avgz = 0;
        count = 0;
        for (String atomname : innerMap.get(type2)) {
            Atom a = pair[1].getAtom(atomname);
            if (a == null) {
                System.out.println("No atom " + atomname + " on " + pair[1].getPDBName());
                return null;
            }
            x1[count] = a.getX();
            y1[count] = a.getY();
            z1[count] = a.getZ();
            avgx += x1[count]; avgy += y1[count]; avgz += z1[count];
            count++;
        }
        avgx /= 6.0; avgy /= 6.0; avgz /= 6.0;
        sx = (x1[1]+x1[2])/2.0 - avgx; sy = (y1[1]+y1[2])/2.0 - avgy; sz = (z1[1]+z1[2])/2.0 - avgz;
        r = Math.sqrt(sx*sx+sy*sy+sz*sz);
        sx /= r; sy /= r; sz /= r;
        lx = x1[0] - avgx; ly = y1[0] - avgy; lz = z1[0] - avgz;
        r = Math.sqrt(lx*lx+ly*ly+lz*lz);
        lx /= -r; ly /= -r; lz /= -r;
        nx = (sy*lz-sz*ly);
        ny = (sz*lx-sx*lz);
        nz = (sx*ly-sy*lx);
        r = Math.sqrt(nx*nx+ny*ny+nz*nz);
        nx /= r; ny /= r; nz /= r;
        lx = (ny*sz-nz*sy);
        ly = (nz*sx-nx*sz);
        lz = (nx*sy-ny*sx);
        r = Math.sqrt(lx*lx+ly*ly+lz*lz);
        lx /= r; ly /= r; lz /= r;
        nx *= -1; ny *= -1; nz *= -1;

        INDArray B2 = Nd4j.create(new double[] {sx, nx, lx, avgx, sy, ny, ly, avgy, sz, nz, lz, avgz, 0, 0, 0, 1}, new int[] {4,4}, 'c');
        //System.out.println("\n\n"+ns.format(B1));
        //System.out.println(ns.format(B2));
        INDArray result = NDDNA.calculatetp(InvertMatrix.invert(B2, false).mmul(B1));
        //System.out.println(ns.format(result)+"\n\n");

        return new INDArray[] {P, result};
    }

    public double[][] getStepParameters() {
        List<Group[]> groups = findPairs(getNucleicChains(true));
        INDArray[] lastPair = null, currentPair = null;
        double[][] result = new double[groups.size()][12];
        for (int i = 0; i < groups.size(); i++) {
            if (i == 0) {
                currentPair = basePairParameters(groups.get(i));
                for (int j = 0; j < 6; j++) {
                    result[i][6+j] = 0;
                    result[i][j] = currentPair[1].getDouble(j);
                }
            } else {
                lastPair = currentPair;
                currentPair = basePairParameters(groups.get(i));
                INDArray stepParameters = NDDNA.calculatetp(InvertMatrix.invert(lastPair[0], false)
                    .mmul(currentPair[0]));
                for (int j = 0; j < 6; j++) {
                    result[i][j] = currentPair[1].getDouble(j);
                    result[i][6+j] = stepParameters.getDouble(j);
                }
            }
        }
        return result;
    }


    public Structure getStructure() {
        return structure;
    }

    public String getPairSequence() {
        return pairSequence;
    }

    public static char complementBase(char base, boolean RNA) {
        if (base == 'A' && RNA) return 'U';
        if (base == 'A') return 'T';
        if (base == 'T') return 'A';
        if (base == 'U') return 'A';
        if (base == 'C') return 'G';
        if (base == 'G') return 'C';
        return ' ';
    }

    public static String complement(String sequence, boolean RNA) {
        String result = "";
        for (int i = sequence.length() - 1; i >= 0; i--) {
            result += complementBase(sequence.charAt(i), RNA);
        }
        return result;
    }


    public static String longestCommonSubstring(String s1, String s2) {
        int start = 0;
        int max = 0;
        for (int i = 0; i < s1.length(); i++) {
            for (int j = 0; j < s2.length(); j++) {
                int x = 0;
                while (s1.charAt(i + x) == s2.charAt(j + x)) {
                    x++;
                    if (((i + x) >= s1.length()) || ((j + x) >= s2.length())) break;
                }
                if (x > max) {
                    max = x;
                    start = i;
                }
            }
        }
        return s1.substring(start, (start + max));
    }

}
