package edu.dnatools.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.dnatools.model.RCSBAnalysis;
import edu.dnatools.model.StepLibrary;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by luke on 8/3/17.
 */
public class ForceFieldAnalysis {

    private static Logger log = LoggerFactory.getLogger(ForceFieldAnalysis.class);
    private static Gson gson = new GsonBuilder().create();

    public static void getZipFile(RCSBAnalysis rcsbAnalysis) {
        try {
            List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList("./zipff", "ff/"+rcsbAnalysis.getCode()));

            Process execute = new ProcessBuilder(args).start();
            if (!execute.waitFor(1, TimeUnit.MINUTES)) {
                execute.destroy();
                log.info("Zip ran out of time");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        log.info(rcsbAnalysis.getCode() + " zip completed successfully");
    }

    public static StepLibrary loadForceField(RCSBAnalysis rcsbAnalysis) throws IOException {
        List<Double[]> steps = new ArrayList<>();
        List<String> contexts = new ArrayList<>();
        String Tfilepath = "ff/" + rcsbAnalysis.getCode() + "/tetramersteps.txt";
        File Tfile = new File(Tfilepath);
        if (Tfile.exists()) {

            String content = FileUtils.readFileToString(Tfile);
            content = content.replace("[", "").replace("]","").replace(",", "");
            String[] lines = content.split("\\r?\\n");

            String current = "AAAA";
            for (int i = 0; i < lines.length; i++) {
                String context;
                String[] data = lines[i].split("\\s+");
                if (data.length < 6) continue;
                Double[] step = new Double[6];
                boolean broken = false;
                for (int j = 0; j < 6; j++) {
                    try {
                        step[j] = Double.parseDouble(data[j]);
                    } catch (NumberFormatException e) {
                        broken = true;
                        break;
                    }
                }
                if (broken) continue;
                if (data.length < 8) context = current;
                else context = data[7];
                steps.add(step);
                contexts.add(context);
                current = context;
            }

            log.info(steps.size() + " steps in database");
            assert steps.size() == contexts.size();
            StepLibrary sl = new StepLibrary();
            sl.setContexts(new String[steps.size()]);
            sl.setSteps(new Double[steps.size()][6]);
            for (int i = 0; i < steps.size(); i++) {
                sl.getSteps()[i] = steps.get(i);
                sl.getContexts()[i] = contexts.get(i);
            }

            return sl;

        }

        return null;

    }



    public static StepLibrary loadForceFieldDimers(RCSBAnalysis rcsbAnalysis) throws IOException {
        List<Double[]> steps = new ArrayList<>();
        List<String> contexts = new ArrayList<>();
        String Tfilepath = "ff/" + rcsbAnalysis.getCode() + "/steps.txt";
        File Tfile = new File(Tfilepath);
        if (Tfile.exists()) {

            String content = FileUtils.readFileToString(Tfile);
            content = content.replace("[", "").replace("]","").replace(",", "");
            String[] lines = content.split("\\r?\\n");

            String current = "CG";
            for (int i = 0; i < lines.length; i++) {
                String context;
                String[] data = lines[i].split("\\s+");
                if (data.length < 6) continue;
                Double[] step = new Double[6];
                boolean broken = false;
                for (int j = 0; j < 6; j++) {
                    try {
                        step[j] = Double.parseDouble(data[j]);
                    } catch (NumberFormatException e) {
                        broken = true;
                        break;
                    }
                }
                if (broken) continue;
                if (data.length < 8) context = current;
                else context = data[7].substring(1, 3);
                steps.add(step);
                contexts.add(context);
                current = context;
            }

            log.info(steps.size() + " steps in database");
            assert steps.size() == contexts.size();
            StepLibrary sl = new StepLibrary();
            sl.setContexts(new String[steps.size()]);
            sl.setSteps(new Double[steps.size()][6]);
            for (int i = 0; i < steps.size(); i++) {
                sl.getSteps()[i] = steps.get(i);
                sl.getContexts()[i] = contexts.get(i);
            }

            return sl;

        }

        return null;

    }


}
