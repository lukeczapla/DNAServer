package edu.dnatools.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.model.PDBinput;
import edu.dnatools.model.RCSBAnalysis;
import edu.dnatools.service.RCSBService;
import org.apache.commons.io.FileUtils;
import org.biojava.nbio.structure.*;
import edu.dnatools.basepairs.BasePairParameters;
import org.biojava.nbio.structure.contact.Pair;
import org.biojava.nbio.structure.ecod.EcodDatabase;
import org.biojava.nbio.structure.ecod.EcodDomain;
import org.biojava.nbio.structure.ecod.EcodFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dimensionalityreduction.PCA;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.inverse.InvertMatrix;
import org.nd4j.linalg.ops.transforms.Transforms;
import org.nd4j.linalg.string.NDArrayStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by luke on 7/22/17.
 */
public class ForceField {

    Logger log = LoggerFactory.getLogger(ForceField.class);
    private String WCsteps = "CG CA TA AG GG AA GA AT AC GC";
    private String tetramers = "AAAA AAAC AAAG AAAT CAAA CAAC CAAG CAAT GAAA GAAC GAAG GAAT TAAA TAAC TAAG TAAT AACA AACC AACG AACT CACA CACC CACG CACT GACA GACC GACG GACT TACA TACC TACG TACT AAGA AAGC AAGG AAGT CAGA CAGC CAGG CAGT GAGA GAGC GAGG GAGT TAGA TAGC TAGG TAGT AATA AATC AATG AATT CATA CATG GATA GATC GATG TATA ACAA ACAC ACAG ACAT CCAA CCAC CCAG CCAT GCAA GCAC GCAG GCAT TCAA TCAC TCAG TCAT ACGA ACGC ACGG ACGT CCGA CCGG GCGA GCGC GCGG TCGA AGAA AGAC AGAG AGAT CGAA CGAC CGAG CGAT GGAA GGAC GGAG GGAT TGAA TGAC TGAG TGAT AGCA AGCC AGCG AGCT CGCA CGCG GGCA GGCC GGCG TGCA AGGA AGGC AGGG AGGT CGGA CGGC CGGG CGGT GGGA GGGC GGGG GGGT TGGA TGGC TGGG TGGT ATAA ATAC ATAG ATAT CTAA CTAG GTAA GTAC GTAG TTAA";
    //private final String tetramerList = Arrays.asList(tetramers.split(" "));

    private Map<String, List<INDArray>> data = new HashMap<>();
    private Map<String, List<INDArray>> tetramerdata = new HashMap<>();
    private Map<String, StructuralData> sd = new TreeMap<>();
    private HashMap<StepData, StructuralData> nmap = new HashMap<>();
    private HashMap<StepData, String> cmap = new HashMap<>();
    private boolean RNA = false, nonredundant = true, done = false;
    private String pdblist;
    private static String rootfolder = "ff/";
    private String code = "demo";
    private PDBinput input;

    NDArrayStrings ns = new NDArrayStrings(4);

    // to-do come up with more complex non-redudancy checks
    public ForceField(PDBinput pin, RCSBService database) {
        code = Processor.randomWord(5);
        input = pin;

        new Thread(() -> {

            new File(rootfolder + code).mkdir();

            pdblist = pin.getPdbList();
            RNA = pin.isRNA();
            nonredundant = pin.isNonredundant();
            if (RNA) {
                WCsteps = WCsteps.replace("T", "U");
                tetramers = tetramers.replace("T", "U");
            } else {
                WCsteps = WCsteps.replace("U", "T");
                tetramers = tetramers.replace("U", "T");
            }
            String[] pdb = pdblist.split("\\s+");

            List<String> stepSet = Arrays.asList(WCsteps.split("\\s+"));
            List<String> tetramerSet = Arrays.asList(tetramers.split("\\s+"));
            log.info(""+stepSet.size());
            log.info(""+tetramerSet.size());
            EcodDatabase ecod = EcodFactory.getEcodDatabase("latest");
            for (int i = 0; i < pdb.length; i++) {
                // grab the record and get started!
                Structure s;
                log.info("Loading {}", pdb[i]);
                try {
                    s = StructureIO.getStructure(pdb[i]);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    continue;
                }

                if (s == null) continue;
                // skip data that's before the afterDate and that's after the beforeDate
                if (pin.getAfterDate() != null && s.getPDBHeader().getDepDate() != null && s.getPDBHeader().getDepDate().before(pin.getAfterDate())) {
                    log.info("After, Skipping " + pdb[i]);
                    continue;
                }

                if (pin.getBeforeDate() != null && s.getPDBHeader().getDepDate() != null && s.getPDBHeader().getDepDate().after(pin.getBeforeDate())) {
                    log.info("Before, Skipping " + pdb[i]);
                    continue;
                }

                // grab nucleic acid chains with built-in method
                BasePairParameters entry = new BasePairParameters(s, RNA, nonredundant).analyze();
                List<Chain> nucleic = entry.getNucleicChains(nonredundant);
                // look for ECOD domains
                List<EcodDomain> ecodList;
                try {
                    ecodList = ecod.getDomainsForPdb(pdb[i]);
                } catch (IOException e) {
                    ecodList = null;
                }

                StructuralData info = new StructuralData();
                info.ecodDomains = ecodList;
                info.pdbId = pdb[i];

                if (s.getChains() != null) for (Chain c : s.getChains()) {
                    if (c.isProtein()) info.aaSequences.add(c.getAtomSequence());
                }
                if (s.getChains() != null) for (Chain c : nucleic)
                    info.naSequences.add(c.getAtomSequence());

                boolean dup = false;
                if (nonredundant) {
                    if (info.naSequences.size() > 0) {
                        if (info.naSequences.get(0).length() > 50) {
                            for (String key : sd.keySet()) {
                                StructuralData d = sd.get(key);
                                if (d.naSequences.size() > 0 &&
                                        d.naSequences.get(0).equals(info.naSequences.get(0)) && d.aaSequences.size() == info.aaSequences.size()) {
                                    //sd.put(pdb[i], info);
                                    dup = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        for (String key : sd.keySet()) {
                            StructuralData d = sd.get(key);
                            if (d.naSequences.size() > 0 && info.naSequences.size() > 0 && (d.naSequences.get(0).equals(info.naSequences.get(0))
                                    || BasePairParameters.longestCommonSubstring(d.naSequences.get(0), info.naSequences.get(0)).length() > (int) (0.7 * d.naSequences.get(0).length()))) {
                                if (d.ecodDomains != null && info.ecodDomains != null && d.ecodDomains.size() > 0 && d.ecodDomains.size() == info.ecodDomains.size()) {
                                    boolean eq = true;
                                    for (int j = 0; j < d.ecodDomains.size(); j++) {
                                        if (d.ecodDomains.get(j).getHGroup() != d.ecodDomains.get(j).getHGroup()) {
                                            eq = false;
                                            break;
                                        }
                                        if (d.ecodDomains.get(j).getFGroup() != d.ecodDomains.get(j).getFGroup()) {
                                            eq = false;
                                            break;
                                        }
                                    }
                                    if (eq) {
                                        dup = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (dup) continue;
                sd.put(pdb[i], info);

                List<Pair<Group>> pairs = entry.findPairs(nucleic);
                String sequences = entry.getPairSequence();

                String[] regions = sequences.split("\\s+");
                int counter = 0;
                for (int j = 0; j < regions.length; j++) {
                    String region = regions[j];
                    for (int k = 0; k < region.length(); k++) {
                        if (k == 0) {
                            counter++;
                            continue;
                        }
                        if (counter >= entry.getStepParameters().length) break;
                        String step = "" + region.charAt(k - 1) + region.charAt(k);
                        String ctx = "";
                        if (k == 1) ctx += ".";
                        else ctx += region.charAt(k-2);
                        ctx += step;
                        if (k == region.length()-1) ctx += ".";
                        else ctx += region.charAt(k+1);
                        double[] pW = entry.get30Coordinates(counter);
                        double[] pC = entry.reversePacking(entry.get30Coordinates(counter));
                        INDArray parameters = Nd4j.create(1, 30);
                        for (int v = 0; v < 30; v++)
                            parameters.getColumn(v).assign(pW[v]);
                        if (stepSet.contains(step)) {
                            if (!data.containsKey(step)) {
                                data.put(step, new Vector<>());
                            }
                            List<INDArray> stepdata = data.get(step);
                            stepdata.add(parameters);
                            StepData ds = new StepData(parameters);
                            nmap.put(ds, info);
                            cmap.put(ds, ctx);
                            if (!ctx.contains(".")) {
                                if (tetramerSet.contains(ctx)) {
                                    if (!tetramerdata.containsKey(ctx)) {
                                        tetramerdata.put(ctx, new Vector<>());
                                    }
                                    stepdata = tetramerdata.get(ctx);
                                    stepdata.add(parameters);
                                } else {
                                    ctx = complementContext(ctx);
                                    if (!tetramerdata.containsKey(ctx)) {
                                        tetramerdata.put(ctx, new Vector<>());
                                    }
                                    stepdata = tetramerdata.get(ctx);
                                    INDArray parameters2 = Nd4j.create(1, 30);
                                    for (int v = 0; v < 30; v++)
                                        parameters2.getColumn(v).assign(pC[v]);
                                    stepdata.add(parameters2);
                                    ds = new StepData(parameters2);
                                    nmap.put(ds, info);
                                    cmap.put(ds, ctx);
                                }
                            }
                        } else {
                            step = BasePairParameters.complement(step, RNA);
                            if (!data.containsKey(step)) {
                                data.put(step, new Vector<>());
                            }
                            List<INDArray> stepdata = data.get(step);
                            ctx = complementContext(ctx);
                            parameters = Nd4j.create(1, 30);
                            for (int v = 0; v < 30; v++)
                                parameters.getColumn(v).assign(pC[v]);
                            stepdata.add(parameters);
                            StepData ds = new StepData(parameters);
                            nmap.put(ds, info);
                            cmap.put(ds, ctx);
                            if (!ctx.contains(".")) {
                                if (!tetramerdata.containsKey(ctx)) {
                                    tetramerdata.put(ctx, new Vector<>());
                                }
                                stepdata = tetramerdata.get(ctx);
                                stepdata.add(parameters);
                            }
                        }
                        counter++;
                    }
                }
            }

            createModel();
            printStructureAnalysis();

            done = true;
            if (database != null) {
                List<RCSBAnalysis> recs = database.getAll();
                for (int c = 0; c < recs.size(); c++) {
                    if (recs.get(c).getCode().equals(code)) {
                        recs.get(c).setDone(true);
                        database.update(recs.get(c).getId(), recs.get(c));
                        break;
                    }
                }
            }

        }).start();

    }


    public void printStructureAnalysis() {
        StringBuilder sb = new StringBuilder(1000000);
        for (String s : sd.keySet()) {
            sb.append(s + "\n" + sd.get(s));
        }
        try {
            FileUtils.write(new File(rootfolder + code + "/structures.txt"), sb.toString(), Charset.defaultCharset(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public INDArray cullEigen(INDArray dataset, double ndevs, StringBuilder discardedData) {
        INDArray reshaped = Nd4j.zeros(dataset.rows(), 18);
        for (int i = 0; i < 18; i++) {
            if (i < 6) reshaped.getColumn(i).setData(dataset.getColumn(i).data());
            else if (i < 12) reshaped.getColumn(i).setData(dataset.getColumn(6+i).data());
            else reshaped.getColumn(i).setData(dataset.getColumn(12+i).data());
        }
        PCA myPCA = new PCA(reshaped);
        INDArray factors = myPCA.getEigenvalues();
        int count = 0;
        for (int i = 0; i < dataset.rows(); i++) {
            INDArray components = myPCA.convertToComponents(reshaped.getRow(i));
            for (int j = 0; j < 18; j++) {
                if (Math.abs(components.getDouble(j)) > ndevs / Math.sqrt(factors.getDouble(j))) {
                    break;
                }
                if (j == 17) count++;
            }
        }
        INDArray culledArray = Nd4j.create(count, 30);
        int total = count;
        count = 0;
        for (int i = 0; i < dataset.rows(); i++) {
            INDArray components = myPCA.convertToComponents(reshaped.getRow(i));
            for (int j = 0; j < 18; j++) {
                if (Math.abs(components.getDouble(j)) > ndevs / Math.sqrt(factors.getDouble(j))) {
                    discardedData.append((dataset.getRow(i)));
                    StepData st = new StepData(dataset.getRow(i));
                    StructuralData sd = nmap.get(st);
                    if (sd != null) {
                        String context = cmap.get(st);
                        discardedData.append(" " + sd.pdbId + " " + context);
                    }
                    discardedData.append("\n");
                    break;
                }
                if (j == 17) culledArray.putRow(count++, dataset.getRow(i));
            }
        }
        assert count == total;
        return culledArray;
    }


    public INDArray cullStd(INDArray dataset, double ndevs, StringBuilder discardedData) {
        INDArray reshaped = Nd4j.zeros(dataset.rows(), 18);
        for (int i = 0; i < 18; i++) {
            if (i < 6) reshaped.getColumn(i).setData(dataset.getColumn(i).data());
            else if (i < 12) reshaped.getColumn(i).setData(dataset.getColumn(6+i).data());
            else reshaped.getColumn(i).setData(dataset.getColumn(12+i).data());
        }
        INDArray mean = Nd4j.create(1, 18);
        for (int i = 0; i < dataset.rows(); i++) {
            mean.addi(reshaped.getRow(i));
        }
        mean.divi(dataset.rows());
        INDArray stddev = Nd4j.create(1, 18);
        for (int i = 0; i < dataset.rows(); i++) {
            stddev.addi(Transforms.pow(reshaped.getRow(i).sub(mean), 2.0, true));
        }
        stddev.divi(dataset.rows());
        Transforms.pow(stddev, 0.5, false);

        int count = 0;
        for (int i = 0; i < dataset.rows(); i++) {
            INDArray dev = reshaped.getRow(i).sub(mean);
            for (int j = 0; j < 18; j++) {
                if (Math.abs(dev.getDouble(j)) > ndevs * stddev.getDouble(j)) {
                    break;
                }
                if (j == 17) count++;
            }
        }
        INDArray culledArray = Nd4j.create(count, 30);
        int total = count;
        count = 0;
        for (int i = 0; i < dataset.rows(); i++) {
            INDArray dev = reshaped.getRow(i).sub(mean);
            for (int j = 0; j < 18; j++) {
                if (Math.abs(dev.getDouble(j)) > ndevs * stddev.getDouble(j)) {
                    discardedData.append((dataset.getRow(i)));
                    StepData st = new StepData(dataset.getRow(i));
                    StructuralData sd = nmap.get(st);
                    if (sd != null) {
                        String context = cmap.get(st);
                        discardedData.append(" " + sd.pdbId + " " + context);
                    }
                    break;
                }
                if (j == 17) culledArray.putRow(count++, dataset.getRow(i));
            }
        }
        assert count == total;
        return culledArray;
    }


    public void createModel() {

        StringBuilder sb = new StringBuilder(5000);
        StringBuilder sb2 = new StringBuilder(1000000);
        StringBuilder ds = new StringBuilder(100000);
        JsonFormattedParameters jfp = new JsonFormattedParameters();
        JsonFormattedTetramerParameters jftp = new JsonFormattedTetramerParameters();

        int x = 0;
        for (String key : Arrays.copyOfRange(WCsteps.split("\\s+"), 0, 10)) {
            if (data.get(key) == null) continue;
            int N = data.get(key).size();
            boolean sc = BasePairParameters.complement(key, RNA).equals(key);
            INDArray totals = Nd4j.create((sc ? 2 * N : N), 30);
            for (int i = 0; i < data.get(key).size(); i++) {
                totals.putRow(i, data.get(key).get(i).reshape(1, 30).dup());
                if (sc) {
                    data.get(key).get(i).getColumn(0).negi();
                    data.get(key).get(i).getColumn(3).negi();
                    totals.putRow(i + N, data.get(key).get(i).reshape(1, 30).dup());
                    data.get(key).get(i).getColumn(0).negi();
                    data.get(key).get(i).getColumn(3).negi();
                }
            }

            PCA myPCA = new PCA(totals);
            log.info(key);
            log.info(ns.format(myPCA.getMean()));

            log.info(ns.format(myPCA.getCovarianceMatrix()));
            log.info(ns.format(InvertMatrix.invert(myPCA.getCovarianceMatrix(), false)));
            log.info("REDUCING DATASET\n");

            INDArray culledArray = null;
            if (input.getCullEigen() != null && input.getCullEigen()) {
                culledArray = cullEigen(totals, 3.0, ds);
                //for (int j = 0; j < 10; j++)
                //    culledArray = cullStd(culledArray, 3.0);
                for (int j = 0; j < 9; j++)
                    culledArray = cullEigen(culledArray, 3.0, ds);
            }
            if (input.getCullStandard() != null && input.getCullStandard()) {
                if (culledArray == null) culledArray = cullStd(totals, 3.0, ds);
                else culledArray = cullStd(culledArray, 3.0, ds);
                for (int j = 0; j < 9; j++)
                    culledArray = cullStd(culledArray, 3.0, ds);
            }
            if (input.getCullStandard() != null && input.getCullEigen() != null && !input.getCullStandard() && !input.getCullEigen())
                culledArray = totals.dup();

            PCA myPCA2 = new PCA(culledArray);
            jfp.addData(x++, InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false), myPCA2.getMean());
            sb.append(key + "\n");
            sb.append(ns.format(myPCA2.getMean()) + "\n");
            sb.append(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false)) + "\n\n");
            log.info(ns.format(myPCA2.getMean()));
            log.info(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false)));

            int nelements = 0;

            sb2.append(key + "\n");
            for (int i = 0; i < culledArray.rows(); i++) {
                //log.info(data.get(key).get(i));
                sb2.append(ns.format(culledArray.getRow(i)));
                StepData st = new StepData(culledArray.getRow(i));
                StructuralData sd = nmap.get(st);
                if (sd != null) {
                    sb2.append(" " + sd.pdbId + " " + cmap.get(st));
                }
                sb2.append("\n");
                nelements++;
            }
            sb2.append(nelements + "\n\n");
        }

        Gson gson2 = new GsonBuilder().setPrettyPrinting().create();

        try {
            FileUtils.write(new File(rootfolder + code + "/steps.txt"), sb2.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/forcefield.txt"), sb.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/pdna.txt"), gson2.toJson(jfp), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/discarded.txt"), ds.toString(), Charset.defaultCharset(), false);

        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(WCsteps + ", all done!");

        sb = new StringBuilder(5000);
        sb2 = new StringBuilder(1000000);
        ds = new StringBuilder(100000);

        x = 0;
        for (String key : Arrays.copyOfRange(tetramers.split("\\s+"), 0, 136)) {
            if (tetramerdata.get(key) == null || tetramerdata.get(key).size() < 1) continue;
            int N = tetramerdata.get(key).size();
            boolean sc = complementContext(key).equals(key);
            INDArray totals = Nd4j.create((sc ? 2 * N : N), 30);
            for (int i = 0; i < tetramerdata.get(key).size(); i++) {
                totals.putRow(i, tetramerdata.get(key).get(i).reshape(1, 30).dup());
                if (sc) {
                    tetramerdata.get(key).get(i).getColumn(0).negi();
                    tetramerdata.get(key).get(i).getColumn(3).negi();
                    totals.putRow(i + N, tetramerdata.get(key).get(i).reshape(1, 30).dup());
                    tetramerdata.get(key).get(i).getColumn(0).negi();
                    tetramerdata.get(key).get(i).getColumn(3).negi();
                }
            }

            PCA myPCA = new PCA(totals);
            log.info(key);
            log.info(ns.format(myPCA.getMean()));

            log.info(ns.format(myPCA.getCovarianceMatrix()));
            log.info(ns.format(InvertMatrix.invert(myPCA.getCovarianceMatrix(), false)));
            log.info("REDUCING DATASET\n");

            INDArray culledArray = null;
            if (totals.rows() > 10) {
                culledArray = totals.dup();
                if (input.getCullEigen() != null && input.getCullEigen()) {
                    for (int j = 0; j < 10; j++)
                        culledArray = cullEigen(culledArray, 3.0, ds);
                }
                if (input.getCullStandard() != null && input.getCullStandard()) {
                    for (int j = 0; j < 10; j++)
                        culledArray = cullStd(culledArray, 3.0, ds);
                }
            } else culledArray = totals;

            PCA myPCA2 = new PCA(culledArray);
            jftp.addData(x++, InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false), myPCA2.getMean());
            sb.append(key + "\n");
            sb.append(ns.format(myPCA2.getMean()) + "\n");
            sb.append(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false)) + "\n\n");
            log.info(ns.format(myPCA2.getMean()));
            log.info(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false)));

            int nelements = 0;

            sb2.append(key + "\n");
            for (int i = 0; i < culledArray.rows(); i++) {
                //log.info(data.get(key).get(i));
                sb2.append(ns.format(culledArray.getRow(i)));
                StepData st = new StepData(culledArray.getRow(i));
                StructuralData sd = nmap.get(st);
                if (sd != null) {
                    sb2.append(" " + sd.pdbId + " " + cmap.get(st));
                }
                sb2.append("\n");
                nelements++;
            }
            sb2.append(nelements + "\n\n");
        }

        try {
            FileUtils.write(new File(rootfolder + code + "/tetramersteps.txt"), sb2.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/tetramerforcefield.txt"), sb.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/tetramer.txt"), gson2.toJson(jftp), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/tetramerdiscarded.txt"), ds.toString(), Charset.defaultCharset(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.info(tetramers + ", all done!");

    }


    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String complementContext(String ctx) {
        String result = "";
        for (int i = 3; i >= 0; i--) {
            if (ctx.charAt(i) == '.') result += ".";
            else {
                result += BasePairParameters.complement(""+ctx.charAt(i), RNA);
            }
        }
        return result;
    }

    class StepData {
        INDArray value;

        public StepData(INDArray steps) {
            value = steps;
        }

        @Override
        public int hashCode() {
            return ns.format(value).hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StepData)) return false;
            StepData o = (StepData)other;
            return ns.format(this.value).equals(ns.format(o.value));
        }


        @Override
        public String toString() {
            return value.toString();
        }

    }



}


class JsonFormattedTetramerParameters {

    private double[][][] forceconstants;
    private double[][] stepparameters;

    public JsonFormattedTetramerParameters() {
        forceconstants = new double[136][30][30];
        stepparameters = new double[136][30];
    }

    public void addData(int v, INDArray fc, INDArray tp0) {
        for (int i = 0; i < 30; i++) {
            stepparameters[v][i] = tp0.getDouble(i);
            for (int j = 0; j < 30; j++) {
                forceconstants[v][i][j] = fc.getDouble(i, j);
            }
        }
    }

}


class JsonFormattedParameters {

    private double[][][] forceconstants;
    private double[][] stepparameters;

    public JsonFormattedParameters() {
        forceconstants = new double[10][30][30];
        stepparameters = new double[10][30];
    }

    public void addData(int v, INDArray fc, INDArray tp0) {
        for (int i = 0; i < 30; i++) {
            stepparameters[v][i] = tp0.getDouble(i);
            for (int j = 0; j < 30; j++) {
                forceconstants[v][i][j] = fc.getDouble(i, j);
            }
        }
    }

}


class StructuralData {

    List<EcodDomain> ecodDomains;
    List<String> naSequences = new ArrayList<>();
    List<String> aaSequences = new ArrayList<>();
    String pdbId = "";

    @Override
    public String toString() {
        String result = pdbId+"\n";
        for (String s : naSequences) {
            result += "Nucleic acid chain " + s + "\n";
        }
        for  (String s : aaSequences) {
            result += "Amino acid chain " + s + "\n";
        }
        if (ecodDomains != null) for (EcodDomain d : ecodDomains) {
            result += "Chain Classification ID (ECOD): " + d.getHGroupName()+"\n";
        }
        result += "\n";
        return result;
    }

}

