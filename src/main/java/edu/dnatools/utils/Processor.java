package edu.dnatools.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.calculate.NDDNA;
import edu.dnatools.model.JobInput;
import edu.dnatools.model.PDBinput;
import edu.dnatools.model.Protein;
import edu.dnatools.pdbanalysis.PDBInformation;
import edu.dnatools.service.ProteinService;
import org.apache.commons.io.FileUtils;
import org.biojava.nbio.structure.*;
import edu.dnatools.basepairs.BasePairParameters;
import org.biojava.nbio.structure.ecod.EcodDatabase;
import org.biojava.nbio.structure.ecod.EcodDomain;
import org.biojava.nbio.structure.ecod.EcodFactory;
import org.biojava.nbio.structure.io.PDBFileReader;
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
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * Created by luke on 6/9/17.
 */
public class Processor {

    private static Logger log = LoggerFactory.getLogger(Processor.class);
    private static Gson gson = new GsonBuilder().create();

    private static String steps = "CG CA TA AG GG AA GA AT AC GC ZZ ZA ZT ZG ZC AZ TZ GZ CZ";

    public String fixedProteins = "", fixedPositions = "";

    private static int[] getIndex(int n) {
        if (n == 0) return new int[] {0, 0};
        if (n == 1) return new int[] {1, 1};
        if (n == 2) return new int[] {2, 2};
        if (n == 3) return new int[] {3, 3};
        if (n == 4) return new int[] {4, 4};
        if (n == 5) return new int[] {5, 5};
        if (n == 6) return new int[] {0, 1};
        if (n == 7) return new int[] {0, 2};
        if (n == 8) return new int[] {1, 2};
        if (n == 9) return new int[] {3, 4};
        if (n == 10) return new int[] {3, 5};
        if (n == 11) return new int[] {4, 5};
        if (n == 12) return new int[] {0, 3};
        if (n == 13) return new int[] {1, 3};
        if (n == 14) return new int[] {2, 3};
        if (n == 15) return new int[] {0, 4};
        if (n == 16) return new int[] {1, 4};
        if (n == 17) return new int[] {2, 4};
        if (n == 18) return new int[] {0, 5};
        if (n == 19) return new int[] {1, 5};
        if (n == 20) return new int[] {2, 5};
        return null;
    }

    public static String randomWord(int n) {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String result = "";
        for (int i = 0; i < n; i++) {
            result += alphabet.charAt((int)(alphabet.length()*Math.random()));
        }
        return result;
    }


    /**
     * createJob() does the processing and launching of the job as a Thread
     * it does not report when the job has finished but there is an embedded anonymous Runnable.
     * @param input The JobInput sent as JSON from the client
     * @param proteinService Used to retrieve the protein data
     * @return Token for the job that was created
     */
    public String createJob(JobInput input, ProteinService proteinService) {

        String rootfolder = "jobs/";
        String token = "RF"+randomWord(8);

        try {

            new File(rootfolder + token).mkdir();
            String forceFN = rootfolder + token + "/FGH.dat";
            String tpFN = rootfolder + token + "/tp0.dat";
            String seqFN = rootfolder + token + "/seq";
            String proteinFN = rootfolder + token + "/proteins.dat";

            PrintWriter o = new PrintWriter(tpFN);
            o.println(steps);
            for (int i = 0; i < 6; i++) {
                for (int n = 0; n < 19; n++) {
                    o.print(input.tp0[n][i] + " ");
                }
                o.println();
            }
            o.flush();
            o.close();

            o = new PrintWriter(forceFN);
            o.println(steps);
            for (int i = 0; i < 21; i++) {
                int[] index = getIndex(i);
                for (int n = 0; n < 19; n++) {
                    o.print(input.fc[n][index[0]][index[1]] + " ");
                }
                o.println();
            }
            o.flush();
            o.close();

            o = new PrintWriter(seqFN);
            o.println(input.getSequence());
            o.flush();
            o.close();

            if (input.getHasProteins()) {
                input.setnProteins((byte)1);
                Protein p = proteinService.getOne(input.getProteinId());
                o = new PrintWriter(proteinFN);
                o.println("1 " + p.getNumber());
                o.println(p.getName() + " " + p.getNumber() + " " + p.getStepLength() + " " + input.getBindingProbability());
                String[] dats = p.getDats().split("END\n");
                String[] pdbs = p.getPdbs().split("END\n");
                for (int i = 0; i < p.getNumber(); i++) {
                    o.println(p.getName()+"-"+i);
                    String datname = rootfolder + token+"/"+p.getName()+"-"+i+".dat";
                    PrintWriter o2 = new PrintWriter(datname);
                    o2.println(dats[i]);
                    o2.flush();
                    o2.close();
                    String pdbname = rootfolder + token+"/"+p.getName()+"-"+i+".pdb";
                    o2 = new PrintWriter(pdbname);
                    log.debug(pdbs[i]);
                    log.debug(dats[i]);
                    o2.println(pdbs[i]);
                    o2.flush(); o2.close();
                }
                o.flush(); o.close();
            } else if (input.getHasFixedProteins()) {
                String fpdname = rootfolder + token+"/fixed-proteins.dat";
                o = new PrintWriter(fpdname);
                long[][] fixp = gson.fromJson(input.getFixedProteins(), long[][].class);
                o.println(fixp.length);
                for (int i = 0; i < fixp.length; i++) {
                    long proteinId = fixp[i][0];
                    long proteinPosition = fixp[i][1];
                    Protein p = proteinService.getOne(proteinId);
                    String[] dats = p.getDats().split("END\n");
                    String datname = rootfolder + token+"/"+p.getName()+".dat";
                    PrintWriter o2 = new PrintWriter(datname);
                    o2.println(dats[0]);
                    o2.flush(); o2.close();
                    String[] pdbs = p.getPdbs().split("END\n");
                    String pdbname = rootfolder + token+"/"+p.getName()+".pdb";
                    o2 = new PrintWriter(pdbname);
                    o2.println(pdbs[0]);
                    o2.flush(); o2.close();
                    fixedProteins += p.getName() + (i < fixp.length-1 ? ", ": "");
                    fixedPositions += proteinPosition + (i < fixp.length-1 ? ", ": "");
                    o.println(p.getName() + " " + p.getStepLength() + " " + (proteinPosition-1));
                }
                o.flush(); o.close();
            }
            List<String> extraArgs = new ArrayList<>();
            if (input.getUseLargeBins()) {
                extraArgs.add("-e");
                extraArgs.add("50.0");
                extraArgs.add("-overlap");
            }
            if (input.bounds.length > 0) {
                for (int i = 0; i < input.bounds.length; i++) {
                    extraArgs.add("-b");
                    for (int j = 0; j < 6; j++) extraArgs.add(""+input.bounds[i][j]);
                }
            }

            log.info(input.bounds.length + " extra boundaries");
            log.info(""+extraArgs);

            log.info(""+input.getnProteins());

            Thread job = new Thread(() -> {
                try {
                    List<String> args = new ArrayList<>();
                    args.addAll(Arrays.asList(input.getSuppressImages() ? "./script-noimages" : "./script", rootfolder+token, input.getnChains() + "", input.getrBounds() + "",
                            input.getgBounds() + "", input.getTwBounds() + "", input.getSeed()+"", input.getnProteins()+""));
                    args.addAll(extraArgs);
                    //log.info(args+"");
                    Process execute = new ProcessBuilder(args).start();
                    if (!execute.waitFor(48, TimeUnit.HOURS)) {
                        execute.destroy();
                        log.info(token + " ran out of time");
                        SystemUtils.deleteFolder(rootfolder+token);
                    }
                } catch (Exception e) {
                    log.info(e.getMessage());
                    SystemUtils.deleteFolder(rootfolder+token);
                }
                log.info(token + " job completed successfully");
            });
            job.start();

        } catch (IOException e) {
            log.info(e.getMessage());
            SystemUtils.deleteFolder(rootfolder+token);
            return null;
        }

        return token;

    }


}