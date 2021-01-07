package edu.dnatools.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.model.Job;
import edu.dnatools.model.Results;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by luke on 6/10/17.
 */
public class Analysis {
    private static Logger log = LoggerFactory.getLogger(Analysis.class);
    private static Gson gson = new GsonBuilder().create();
    private static String rootfolder = "jobs/";

    public static Results analyzeJob(Job job) {
        String token = job.getToken();
        Results result = new Results();
        if (!new File(rootfolder+token).exists()) {
            return null;
        }

        Long seed = job.getSeed();
        int nBounds = 1;
        double[][] bounds = gson.fromJson(job.getBounds(), double[][].class);
        if (bounds.length != 0) {
            nBounds = bounds.length;
        }

        double[][][] twistHistograms = null;
        double[][][] writheHistograms = null;
        double[][][] linkHistograms = null;
        double[][][] rgHistograms = null;
        int[][][] positionHistograms = null;
        int[][][] numberHistograms = null;
        int[][][] distanceHistograms = null;


        int steps = (job.getSequence().length() - 1);

        try {
            String Jfilepath = rootfolder+token + "/Jfactor-" + steps + "bp_ID" + job.getSeed();
            File Jfile = new File(Jfilepath);
            if (Jfile.exists()) {
                String content = FileUtils.readFileToString(Jfile);
                String txt = "J by boundary = ";
                int position = content.indexOf(txt) + txt.length();
                result.Jfactor = ("[" + content.substring(position, content.length()-2) + "]");
                for (int j = 0; j < nBounds - 1; j++) result.Jfactor = result.Jfactor.replaceFirst(" ", ",");
            } else return null;
            for (int i = 0; i < nBounds; i++) {
                double[][] twistHistogram = null;
                double[][] writheHistogram = null;
                double[][] linkHistogram = null;
                double[][] rgHistogram = null;

                int[][] positionHistogram = null;
                int[][] numberHistogram = null;
                int[][] distanceHistogram = null;

                String Tfilepath = rootfolder+token + "/" + "twist_distribution-" + steps + "bp-ID" + seed + "-B" + i;
                File Tfile = new File(Tfilepath);
                if (Tfile.exists()) {
                    twistHistogram = Files.lines(Paths.get(Tfilepath))
                            .map((l)->l.trim().split("\\s+"))
                            .map((sa) -> Stream.of(sa).mapToDouble(Double::parseDouble).toArray())
                            .toArray(double[][]::new);
                }
                String Wfilepath = rootfolder+token + "/" + "writhe_distribution-" + steps + "bp-ID" + seed + "-B" + i;
                File Wfile = new File(Wfilepath);
                if (Wfile.exists()) {
                    writheHistogram = Files.lines(Paths.get(Wfilepath))
                            .map((l)->l.trim().split("\\s+"))
                            .map((sa) -> Stream.of(sa).mapToDouble(Double::parseDouble).toArray())
                            .toArray(double[][]::new);
                }
                String Lfilepath = rootfolder+token + "/" + "link_distribution-" + steps + "bp-ID" + seed + "-B" + i;
                File Lfile = new File(Wfilepath);
                if (Lfile.exists()) {
                    linkHistogram = Files.lines(Paths.get(Lfilepath))
                            .map((l)->l.trim().split("\\s+"))
                            .map((sa) -> Stream.of(sa).mapToDouble(Double::parseDouble).toArray())
                            .toArray(double[][]::new);
                }
                String Rfilepath = rootfolder+token + "/" + "R_g_distribution-" + steps + "bp-ID" + seed + "-B" + i;
                File Rfile = new File(Rfilepath);
                if (Rfile.exists()) {
                    rgHistogram = Files.lines(Paths.get(Rfilepath))
                            .map((l)->l.trim().split("\\s+"))
                            .map((sa) -> Stream.of(sa).mapToDouble(Double::parseDouble).toArray())
                            .toArray(double[][]::new);
                }
                if (job.getHasProteins()) {
                    String pfilepath = rootfolder+token + "/protein_positions-" + steps + "bp-ID" + job.getSeed() + "-B"+i;
                    File pfile = new File(pfilepath);
                    if (pfile.exists()) {
                        positionHistogram = Files.lines(Paths.get(pfilepath))
                                .map((l)->l.trim().split("\\s+"))
                                .map((sa) -> Stream.of(sa).mapToInt(Integer::parseInt).toArray())
                                .toArray(int[][]::new);
                    }
                    pfilepath = rootfolder+token + "/protein_number-" + steps + "bp-ID" + job.getSeed() + "-B"+i;
                    pfile = new File(pfilepath);
                    if (pfile.exists()) {
                        numberHistogram = Files.lines(Paths.get(pfilepath))
                                .map((l)->l.trim().split("\\s+"))
                                .map((sa) -> Stream.of(sa).mapToInt(Integer::parseInt).toArray())
                                .toArray(int[][]::new);
                    }
                    pfilepath = rootfolder+token + "/protein_distances-" + steps + "bp-ID" + job.getSeed() + "-B"+i;
                    pfile = new File(pfilepath);
                    if (pfile.exists()) {
                        distanceHistogram = Files.lines(Paths.get(pfilepath))
                                .map((l)->l.trim().split("\\s+"))
                                .map((sa) -> Stream.of(sa).mapToInt(Integer::parseInt).toArray())
                                .toArray(int[][]::new);
                    }

                }
                if (i == 0) {
                    if (twistHistogram == null || writheHistogram == null || linkHistogram == null || rgHistogram == null) return result;
                    twistHistograms = new double[nBounds][twistHistogram.length][twistHistogram[0].length];
                    writheHistograms = new double[nBounds][writheHistogram.length][writheHistogram[0].length];
                    linkHistograms = new double[nBounds][linkHistogram.length][linkHistogram[0].length];
                    rgHistograms = new double[nBounds][rgHistogram.length][rgHistogram[0].length];
                    if (positionHistogram != null) {
                        positionHistograms = new int[nBounds][positionHistogram.length][positionHistogram[0].length];
                    }
                    if (distanceHistogram != null) {
                        distanceHistograms = new int[nBounds][distanceHistogram.length][distanceHistogram[0].length];
                    }
                    if (numberHistogram != null) {
                        numberHistograms = new int[nBounds][numberHistogram.length][numberHistogram[0].length];
                    }
                }
                twistHistograms[i] = twistHistogram;
                writheHistograms[i] = writheHistogram;
                linkHistograms[i] = linkHistogram;
                rgHistograms[i] = rgHistogram;
                if (job.getHasProteins() && positionHistograms != null) positionHistograms[i] = positionHistogram;
                if (job.getHasProteins() && numberHistograms != null) numberHistograms[i] = numberHistogram;

                if (job.getHasProteins() && distanceHistograms != null) distanceHistograms[i] = distanceHistogram;
            }
            log.info(result.Jfactor);
            if (twistHistograms != null) result.twistHistogram = gson.toJson(twistHistograms);
            if (writheHistograms != null) result.writheHistogram = gson.toJson(writheHistograms);
            if (linkHistograms != null) result.linkHistogram = gson.toJson(linkHistograms);
            if (rgHistograms != null) result.rgHistogram = gson.toJson(rgHistograms);
            if (positionHistograms != null) result.positionHistogram = gson.toJson(positionHistograms);
            if (numberHistograms != null) result.numberHistogram = gson.toJson(numberHistograms);
            if (distanceHistograms != null) result.distanceHistogram = gson.toJson(distanceHistograms);

            return result;
        } catch (IOException e) {
            log.info(e.getMessage());
            return null;
        }
    }


    public static String returnStructureData(Job job) {
        String filepath = rootfolder+job.getToken() + "/structures-" + (job.getSequence().length()-1)
                + "bp-ID" + job.getSeed() + ".info";
        File file = new File(filepath);
        try {
            if (file.exists()) {
                return FileUtils.readFileToString(file);
            }
            log.info("File not found");
            return null;
        } catch (IOException e) {
            log.debug(e.getMessage());
            return null;
        }
    }

    public static String returnStructurePDB(Job job, Long structureId, Long boundId) {
        int steps = job.getSequence().length()-1;
        List<String> args = Arrays.asList("./builder", rootfolder+job.getToken()+"", steps+"", job.getSeed()+"", structureId+"", boundId+"");
        try {
            Process process = new ProcessBuilder(args).start();
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                process.destroy();
                log.info("Rebuild ran out of time");
            }
            String filename = rootfolder+job.getToken()+"/out.pdb";
            File file = new File(filename);
            if (file.exists()) return FileUtils.readFileToString(file);
            return null;
        } catch (IOException|InterruptedException e) {
            log.info(e.getMessage());
            return null;
        }
    }


    public static void getZipFile(Job job) {
        log.info("Trying to zip job " + job.getToken());
        try {
            List<String> args = new ArrayList<>();
            log.info("Calling zip on jobs/" + job.getToken());
            args.addAll(Arrays.asList("./zipjob", "jobs/"+job.getToken()));

            Process execute = new ProcessBuilder(args).start();
            if (!execute.waitFor(3, TimeUnit.MINUTES)) {
                execute.destroy();
                log.info("Gzip ran out of time");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        log.info(job.getToken() + " gzip completed successfully");
    }

}
