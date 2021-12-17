package edu.dnatools.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.calculate.NDDNA;
import edu.dnatools.model.PDBinput;
import edu.dnatools.model.RCSBAnalysis;
import edu.dnatools.service.RCSBService;
import org.apache.commons.io.FileUtils;
import org.biojava.nbio.structure.*;
import edu.dnatools.basepairs.BasePairParameters;
import org.biojava.nbio.structure.ecod.EcodDatabase;
import org.biojava.nbio.structure.ecod.EcodDomain;
import org.biojava.nbio.structure.ecod.EcodFactory;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dimensionalityreduction.PCA;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
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
    //private final List<String> tetramerList = Arrays.asList(tetramers.split(" "));

    private final Map<String, List<INDArray>> data = new HashMap<>();
    private final Map<String, List<INDArray>> tetramerdata = new HashMap<>();
    private final Map<String, StructuralData> sd = new TreeMap<>();
    private final HashMap<StepData, StructuralData> nmap = new HashMap<>();
    private final HashMap<StepData, String> cmap = new HashMap<>();
    private boolean RNA = false, nonredundant = true, done = false;
    private String pdblist;
    private static final String rootfolder = "ff/";
    private String code;
    private final PDBinput input;

    NDArrayStrings ns = new NDArrayStrings(4);

    // to-do come up with more complex non-redudancy checks
    public ForceField(PDBinput pin, RCSBService database) {
        code = Processor.randomWord(5);
        input = pin;

        new Thread(() -> {

            new File(rootfolder + code).mkdir();

            pdblist = input.getPdbList();
            RNA = input.isRNA();
            nonredundant = input.isNonredundant();
            if (RNA) {
                WCsteps = WCsteps.replace("T", "U");
                tetramers = tetramers.replace("T", "U");
            } else {
                WCsteps = WCsteps.replace("U", "T");
                tetramers = tetramers.replace("U", "T");
            }
            String[] pdb = pdblist.split("\\s+");
            Nd4j.setDataType(DataBuffer.Type.DOUBLE);

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
                if (input.getAfterDate() != null && s.getPDBHeader().getDepDate() != null && s.getPDBHeader().getDepDate().before(input.getAfterDate())) {
                    log.info("After, Skipping " + pdb[i]);
                    continue;
                }

                if (input.getBeforeDate() != null && s.getPDBHeader().getDepDate() != null && s.getPDBHeader().getDepDate().after(input.getBeforeDate())) {
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
                                        if (!Objects.equals(d.ecodDomains.get(j).getHGroup(), info.ecodDomains.get(j).getHGroup())) {
                                            eq = false;
                                            break;
                                        }
                                        if (!Objects.equals(d.ecodDomains.get(j).getFGroup(), info.ecodDomains.get(j).getFGroup())) {
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

                //List<Pair<Group>> pairs = entry.findPairs(nucleic);
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
                        String ctx = "";
                        String ctxt = "";
                        if (counter >= entry.getStepParameters().length) break;
                        String step = "" + region.charAt(k - 1) + region.charAt(k);
                        if (k == 1) ctxt += "..";
                        else if (k == 2) ctxt += "." + region.charAt(0);    //  k - 2
                        else ctxt += "" + region.charAt(k-3) + region.charAt(k-2);
                        if (k == 1) ctx += ".";
                        else ctx += "" + region.charAt(k-2);
                        ctx += step;
                        ctxt += step;
                        if (k == region.length()-1) ctx += ".";
                        else ctx += "" + region.charAt(k+1);
                        if (k == region.length()-1) ctxt += "..";
                        else if (k == region.length()-2) ctx += "" + region.charAt(k+1) + ".";
                        else ctxt += "" + region.charAt(k+1) + region.charAt(k+2);
                        double[] pW = entry.get30Coordinates(counter);
                        double[] pC = BasePairParameters.reversePacking(entry.get30Coordinates(counter));
                        INDArray parameters = Nd4j.create(1, 30);
                        if (stepSet.contains(step)) {
                            for (int v = 0; v < 30; v++)
                                parameters.getColumn(v).assign(pW[v]);
                            if (!data.containsKey(step)) {
                                data.put(step, new Vector<>());
                            }
                            List<INDArray> stepdata = data.get(step);
                            stepdata.add(parameters);
                            StepData ds = new StepData(parameters);
                            nmap.put(ds, info);
                            cmap.put(ds, ctxt);
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
                                    cmap.put(ds, ctxt);
                                }
                            }
                        } else {
                            step = BasePairParameters.complement(step, RNA);
                            if (!data.containsKey(step)) {
                                data.put(step, new Vector<>());
                            }
                            List<INDArray> stepdata = data.get(step);
                            ctx = complementContext(ctx);
                            for (int v = 0; v < 30; v++)
                                parameters.getColumn(v).assign(pC[v]);
                            stepdata.add(parameters);
                            StepData ds = new StepData(parameters);
                            nmap.put(ds, info);
                            cmap.put(ds, ctxt);
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
            sb.append(s).append("\n").append(sd.get(s));
        }
        try {
            FileUtils.write(new File(rootfolder + code + "/structures.txt"), sb.toString(), Charset.defaultCharset(), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public INDArray cullEigen(INDArray dataset, double ndevs, StringBuilder discardedData) {
        INDArray reshaped = Nd4j.zeros(dataset.rows(), 18);
        for (int i = 0; i < dataset.rows(); i++) {
            for (int j = 0; j < 18; j++) {
                if (j < 6) reshaped.getRow(i).getColumn(j).assign(dataset.getRow(i).getDouble(j));
                else if (j < 12) reshaped.getRow(i).getColumn(j).assign(dataset.getRow(i).getDouble(6+j));
                else reshaped.getRow(i).getColumn(j).assign(dataset.getRow(i).getDouble(12+j));
            }
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
        int discarded = 0;
        for (int i = 0; i < dataset.rows(); i++) {
            INDArray components = myPCA.convertToComponents(reshaped.getRow(i));
            for (int j = 0; j < 18; j++) {
                if (Math.abs(components.getDouble(j)) > ndevs / Math.sqrt(factors.getDouble(j))) {
                    discardedData.append((dataset.getRow(i)));
                    StepData st = new StepData(dataset.getRow(i));
                    StructuralData sd = nmap.get(st);
                    if (sd != null) {
                        String context = cmap.get(st);
                        discardedData.append(" ").append(sd.pdbId).append(" ").append(context);
                    }
                    discardedData.append("\n");
                    discarded++;
                    break;
                }
                if (j == 17) culledArray.putRow(count++, dataset.getRow(i));
            }
        }
        assert count == total;
        log.info("Discarded {} items", discarded);
        return culledArray;
    }


    public INDArray cullStd(INDArray dataset, double ndevs, StringBuilder discardedData) {
        INDArray reshaped = Nd4j.zeros(dataset.rows(), 18);
        for (int i = 0; i < dataset.rows(); i++) {
            for (int j = 0; j < 18; j++) {
                if (j < 6) reshaped.getRow(i).getColumn(j).assign(dataset.getRow(i).getDouble(j));
                else if (j < 12) reshaped.getRow(i).getColumn(j).assign(dataset.getRow(i).getDouble(6+j));
                else reshaped.getRow(i).getColumn(j).assign(dataset.getRow(i).getDouble(12+j));
            }
        }
        INDArray mean = Nd4j.create(1, 18);
        for (int i = 0; i < reshaped.rows(); i++) {
            mean.addi(reshaped.getRow(i));
        }
        mean.divi(reshaped.rows());
        INDArray stddev = Nd4j.create(1, 18);
        for (int i = 0; i < reshaped.rows(); i++) {
            stddev.addi(Transforms.pow(reshaped.getRow(i).sub(mean), 2.0, true));
        }
        stddev.divi(reshaped.rows());
        Transforms.pow(stddev, 0.5, false);

        int count = 0;
        for (int i = 0; i < reshaped.rows(); i++) {
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
        int discarded = 0;
        for (int i = 0; i < dataset.rows(); i++) {
            INDArray dev = reshaped.getRow(i).sub(mean);
            //log.info("{} with stddev {}", dev.toString(), stddev);
            for (int j = 0; j < 18; j++) {
                if (Math.abs(dev.getDouble(j)) > ndevs * stddev.getDouble(j)) {
                    discardedData.append(dataset.getRow(i));
                    StepData st = new StepData(dataset.getRow(i));
                    StructuralData sd = nmap.get(st);
                    if (sd != null) {
                        String context = cmap.get(st);
                        discardedData.append(" ").append(sd.pdbId).append(" ").append(context).append("\n");
                    }
                    discarded++;
                    break;
                }
                if (j == 17) culledArray.putRow(count++, dataset.getRow(i));
            }
        }
        assert count == total;
        log.info("Discarded {} items", discarded);
        return culledArray;
    }


    public void createModel() {

        StringBuilder sb = new StringBuilder(5000);
        StringBuilder sb2 = new StringBuilder(1000000);
        StringBuilder jm = new StringBuilder(1000000);
        StringBuilder jm2 = new StringBuilder(1000000);
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
                    INDArray params = data.get(key).get(i);
                    double[] d = new double[30];
                    for (int k = 0; k < 30; k++) d[k] = params.getDouble(k);
                    double[] rev = BasePairParameters.reversePacking(d);
                    params = Nd4j.create(1, 30);
                    for (int k = 0; k < 30; k++) params.getColumn(k).assign(rev[k]);
                    totals.putRow(i + N, params);
                }
            }

            PCA myPCA = new PCA(totals);
            log.info("Mean {}: {}", key, ns.format(myPCA.getMean()));

            //log.info(ns.format(myPCA.getCovarianceMatrix()));
            //log.info(ns.format(InvertMatrix.invert(myPCA.getCovarianceMatrix(), false)));
            log.info("REDUCING DATASET\n");

            INDArray culledArray = null;
            if (input.getCullEigen() != null && input.getCullEigen()) {
                log.info("USING EIGENVECTOR/VALUE DEVIATION REDUCTION FOR SYSTEM");
                culledArray = cullEigen(totals, 3.0, ds);
                for (int j = 0; j < 9; j++)
                    culledArray = cullEigen(culledArray, 3.0, ds);
            }
            if (input.getCullStandard() != null && input.getCullStandard()) {
                log.info("USING STANDARD DEVIATION REDUCTION FOR SYSTEM");
                if (culledArray == null) culledArray = cullStd(totals, 3.0, ds);
                else culledArray = cullStd(culledArray, 3.0, ds);
                for (int j = 0; j < 9; j++)
                    culledArray = cullStd(culledArray, 3.0, ds);
            }
            if (input.getCullStandard() != null && input.getCullEigen() != null && !input.getCullStandard() && !input.getCullEigen())
                culledArray = totals.dup();

            PCA myPCA2 = new PCA(culledArray);
            jfp.addData(x++, InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false), myPCA2.getMean());
            sb.append(key).append("\n");
            sb.append(ns.format(myPCA2.getMean())).append("\n");
            sb.append(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false))).append("\n");
            sb.append(ns.format(myPCA2.getCovarianceMatrix())).append("\n\n");
            log.info("{} = {}", key, ns.format(myPCA2.getMean()));
            //log.info(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false)));

            int nelements = 0;

            sb2.append(key).append("\n");
            jm.append(key).append("\n");
            for (int i = 0; i < culledArray.rows(); i++) {
                sb2.append(ns.format(culledArray.getRow(i)));
                StepData st = new StepData(culledArray.getRow(i));
                jm.append(ns.format(st.ic30())).append("\n");
                StructuralData sd = nmap.get(st);
                if (sd != null) {
                    sb2.append(" ").append(sd.pdbId).append(" ").append(cmap.get(st));
                }
                sb2.append("\n");
                nelements++;
            }
            sb2.append(nelements).append("\n\n");
            jm.append(key).append(" ").append(nelements).append("\n");
            for (int i = 0; i < culledArray.rows(); i++) {
                StepData st = new StepData(culledArray.getRow(i));
                jm.append(ns.format(st.ic30())).append("\n");
            }
            jm.append("\n");
        }

        Gson gson2 = new GsonBuilder().setPrettyPrinting().create();

        try {
            FileUtils.write(new File(rootfolder + code + "/steps.txt"), sb2.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/forcefield.txt"), sb.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/pdna.txt"), gson2.toJson(jfp), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/discarded.txt"), ds.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/jmdimers.txt"), jm.toString(), Charset.defaultCharset(), false);
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
                    INDArray params = tetramerdata.get(key).get(i);
                    double[] d = new double[30];
                    for (int k = 0; k < 30; k++) d[k] = params.getDouble(k);
                    double[] rev = BasePairParameters.reversePacking(d);
                    params = Nd4j.create(1, 30);
                    for (int k = 0; k < 30; k++) params.getColumn(k).assign(rev[k]);
                    totals.putRow(i + N, params);
                }
            }

            PCA myPCA = new PCA(totals);
            log.info("{} = {}", key, ns.format(myPCA.getMean()));

            //log.info(ns.format(myPCA.getCovarianceMatrix()));
            //log.info(ns.format(InvertMatrix.invert(myPCA.getCovarianceMatrix(), false)));
            log.info("REDUCING DATASET\n");

            INDArray culledArray;
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
            sb.append(key).append("\n");
            sb.append(ns.format(myPCA2.getMean())).append("\n");
            sb.append(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false))).append("\n");
            sb.append(ns.format(myPCA2.getCovarianceMatrix())).append("\n\n");
            log.info("{} = {}", key, ns.format(myPCA2.getMean()));
            //log.info(ns.format(InvertMatrix.invert(myPCA2.getCovarianceMatrix(), false)));

            int nelements = 0;

            sb2.append(key).append("\n");
            for (int i = 0; i < culledArray.rows(); i++) {
                //log.info(data.get(key).get(i));
                sb2.append(ns.format(culledArray.getRow(i)));
                StepData st = new StepData(culledArray.getRow(i));
                StructuralData sd = nmap.get(st);
                if (sd != null) {
                    sb2.append(" ").append(sd.pdbId).append(" ").append(cmap.get(st));
                }
                sb2.append("\n");
                nelements++;
            }
            sb2.append(nelements).append("\n\n");
            jm2.append(key).append(" ").append(nelements).append("\n");
            for (int i = 0; i < culledArray.rows(); i++) {
                StepData st = new StepData(culledArray.getRow(i));
                jm2.append(ns.format(st.ic30())).append("\n");
            }
            jm2.append("\n");
        }

        try {
            FileUtils.write(new File(rootfolder + code + "/tetramersteps.txt"), sb2.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/tetramerforcefield.txt"), sb.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/tetramer.txt"), gson2.toJson(jftp), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/tetramerdiscarded.txt"), ds.toString(), Charset.defaultCharset(), false);
            FileUtils.write(new File(rootfolder + code + "/jmtetramers.txt"), jm2.toString(), Charset.defaultCharset(), false);
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

        for (int i = ctx.length()-1; i >= 0; i--) {
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

        // frames from the steps from Euler angles with 3DNA using JM frames
        public INDArray[] frames() {
            INDArray[] result = new INDArray[8];
            result[0] = Nd4j.eye(4); // bp #1
            INDArray pair1 = value.get(NDArrayIndex.interval(0,6)).dup().reshape(1,6);
            INDArray midbasis = NDDNA.calculateM(pair1.dup());
            INDArray baseC = result[0].dup().mmul(InvertMatrix.invert(midbasis, false));
            result[1] = baseC.dup();        // Crick base #1
            result[2] = baseC.dup().mmul(NDDNA.calculateA(pair1));        // Watson base #1
            result[3] = result[2].mmul(NDDNA.calculateA(value.get(NDArrayIndex.interval(6,12)).dup().reshape(1,6)));   // phosphate #1
            result[4] = result[0].mmul(NDDNA.calculateA(value.get(NDArrayIndex.interval(12,18)).dup().reshape(1,6)));  // bp plane #2
            INDArray pair2 = value.get(NDArrayIndex.interval(24, 30)).dup().reshape(1,6);
            midbasis = NDDNA.calculateM(pair2.dup());
            INDArray baseC2 = result[4].dup().mmul(InvertMatrix.invert(midbasis, false));
            result[5] = baseC2.dup();           // Crick base #2
            result[6] = result[5].mmul(NDDNA.calculateA(pair2.dup()));            // Watson base #2
            result[7] = baseC2.mmul(NDDNA.calculateA(value.get(NDArrayIndex.interval(18, 24)).dup().reshape(1,6)));   // phosphate #2
            return result;
        }

        // John Maddocks lab internal coordinate values
        public INDArray ic30() {
            INDArray[] frames = frames();
            INDArray pair1 = NDDNA.calculateIc(InvertMatrix.invert(frames[1], false).mmul(frames[2]));
            INDArray pho1 = NDDNA.calculateIcPho(InvertMatrix.invert(frames[2], false).mmul(frames[3]));
            INDArray bp = NDDNA.calculateIc(InvertMatrix.invert(frames[0], false).mmul(frames[4]));
            INDArray pho2 = NDDNA.calculateIcPho(InvertMatrix.invert(frames[5], false).mmul(frames[7]));
            INDArray pair2 = NDDNA.calculateIc(InvertMatrix.invert(frames[5], false).mmul(frames[6]));
            INDArray result = Nd4j.create(1, 30);
            for (int i = 0; i < 6; i++) {
                result.getColumn(i).assign(pair1.getDouble(i));
                result.getColumn(6+i).assign(pho1.getDouble(i));
                result.getColumn(12+i).assign(bp.getDouble(i));
                result.getColumn(18+i).assign(pho2.getDouble(i));
                result.getColumn(24+i).assign(pair2.getDouble(i));
            }
            return result;
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

    private final double[][][] forceconstants;
    private final double[][] stepparameters;

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

    private final double[][][] forceconstants;
    private final double[][] stepparameters;

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

