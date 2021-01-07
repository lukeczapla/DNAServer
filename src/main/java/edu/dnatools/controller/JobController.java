package edu.dnatools.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import edu.dnatools.calculate.MCDNA;
import edu.dnatools.calculate.NDDNA;
import edu.dnatools.model.*;
import edu.dnatools.repository.UserRepository;
import edu.dnatools.service.JobService;
import edu.dnatools.service.ProteinService;
import edu.dnatools.utils.Analysis;
import edu.dnatools.utils.Processor;
import edu.dnatools.utils.RefTools;
import edu.dnatools.utils.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;

/**
 * Created by luke on 6/9/17.
 */
@RestController
@Api("User login and registration")
public class JobController {

    public static String rootfolder = "jobs/";

    @Autowired
    UserRepository userRepository;

    @Autowired
    JobService jobService;

    @Autowired
    ProteinService proteinService;

    private static Logger log = LoggerFactory.getLogger(JobController.class);
    private static Gson gson = new GsonBuilder().create();

    @ApiOperation(value = "Test the Java simulation backend")
    @RequestMapping(value = "/calculate", method = RequestMethod.PUT)
    @JsonView({JsonViews.JobInput.class})
    public String testCalculation(@RequestBody JobInput input, Principal prince) {
        MCDNA mydna = new MCDNA(input.getSequence(), input.getStepList(), input.getForceConstants(),
                input.getStepParameters(), input.getFixedProteins(), proteinService);
        mydna.launch(100, 0.5f);
        return "Tested";
    }

    @ApiOperation(value = "Submit a calculation", response = Job.class, responseContainer = "List", notes = "Large data set sent")
    @RequestMapping(value = "/submit", method = RequestMethod.PUT)
    @JsonView({JsonViews.JobInput.class, JsonViews.Job.class})
    public ResponseEntity processJob(@RequestBody JobInput input, Principal prince) {
        log.debug(input.getForceConstants());
        log.debug(input.getStepParameters());
        log.debug(input.getSequence());
        input.fc = gson.fromJson(input.getForceConstants(), double[][][].class);
        input.tp0 = gson.fromJson(input.getStepParameters(), double[][].class);
        if (input.getBc() != null) input.bounds = gson.fromJson(input.getBc(), double[][].class);
        if (input.fc == null || input.tp0 == null || input.getSequence() == null || input.getSequence().length() < 2) {
            return new ResponseEntity("ERROR: BAD INPUT", HttpStatus.BAD_REQUEST);
        }
        if (input.getSeed() == null) {
            input.setSeed((long)(10000*Math.random()));
        }
        Processor processor = new Processor();
        String token = processor.createJob(input, proteinService);
        log.info("Returning token " + token);
        User user = userRepository.findByEmail(prince.getName());
        Job submittedJob = Job.createFromJobInput(token, input);
        submittedJob.setUser(user);
        submittedJob.setFixedProteins(processor.fixedProteins);
        submittedJob.setFixedPositions(processor.fixedPositions);

        if (jobService.add(submittedJob) != null) {
            Job result = jobService.getJobByToken(token);
            return new ResponseEntity("{\"id\": " + result.getId() + ", \"token\":\"" + token + "\"}", HttpStatus.OK);
        }
        else return new ResponseEntity("ERROR: Could not add", HttpStatus.BAD_REQUEST);

    }

    @ApiOperation(value = "Get job by id if it belongs to current user")
    @RequestMapping(value = "/myjobs/{id}")
    @JsonView(JsonViews.Job.class)
    public Job getMyJob(@PathVariable("id") Long id, Principal prince) {
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        Job job = jobService.getOne(id);
        if (job.getUser().equals(user)) return job;
        return null;
    }


    @ApiOperation(value = "Get jobs for the current user")
    @RequestMapping(value = "/myjobs", method = RequestMethod.GET)
    @JsonView(JsonViews.Job.class)
    public List<Job> getMyJobs(Principal prince) {
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        if (user == null) return null;
        return jobService.getJobsByUser(user);
    }

    @ApiOperation(value = "Delete one of the jobs for the current user")
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.DELETE)
    public ResponseEntity deleteJob(@PathVariable("id") Long id, Principal prince) {
        if (prince == null) return null;
        Job job = jobService.getOne(id);
        if (job == null) return new ResponseEntity("ERROR", HttpStatus.BAD_REQUEST);
        User user = userRepository.findByEmail(prince.getName());
        if (job.getUser().equals(user)) {
            SystemUtils.deleteFolder(rootfolder+job.getToken());
            jobService.delete(id);
            return new ResponseEntity("DONE", HttpStatus.OK);
        }
        return new ResponseEntity("ERROR", HttpStatus.FORBIDDEN);
    }

    @ApiOperation(value = "Analyze and return results for a job")
    @RequestMapping(value = "/analyze/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.Results.class)
    public Results analyzeJob(@PathVariable("id") Long id, Principal prince) {
        if (prince == null) return null;
        Job job = jobService.getOne(id);
        User user = userRepository.findByEmail(prince.getName());
        if (job.getUser().equals(user)) {
            return Analysis.analyzeJob(job);
        }
        return null;
    }

    @ApiOperation(value = "Return the raw data on all the structures for a job")
    @RequestMapping(value = "/liststructures/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.Results.class)
    public String analyzeJobStructures(@PathVariable("id") Long id, Principal prince) {
        if (prince == null) return null;
        Job job = jobService.getOne(id);
        User user = userRepository.findByEmail(prince.getName());
        if (job.getUser().equals(user)) {
            return Analysis.returnStructureData(job);
        }
        log.debug("Did not match");
        return null;
    }

    @ApiOperation(value = "Get the proteins PDB file for a structure")
    @RequestMapping(value = "/proteins/{id}/{sid}/{bid}", method = RequestMethod.GET)
    public String getProteinsPDB(@PathVariable("id") Long id, @PathVariable("sid") Long structureId,
                                @PathVariable("bid") Long boundId, Principal prince) throws IOException {
        if (prince == null) return null;
        Job job = jobService.getOne(id);
        User user = userRepository.findByEmail(prince.getName());
        if (job.getUser().equals(user)) {
            String filename = rootfolder + job.getToken()+"/proteins-"+(job.getSequence().length()-1)+"bp-ID"
                    + job.getSeed() + "-B" + boundId + "-" + structureId + ".pdb";
            File file = new File(filename);
            if (file.exists()) return FileUtils.readFileToString(file);
            else return null;
        }
        return null;
    }

    @ApiOperation(value = "Get the step parameters for a structure")
    @RequestMapping(value = "/steps/{id}/{sid}/{bid}", method = RequestMethod.GET)
    public String getStepParameters(@PathVariable("id") Long id, @PathVariable("sid") Long structureId,
                                    @PathVariable("bid") Long boundId, Principal prince) {
        if (prince == null) return null;
        Job job = jobService.getOne(id);
        User user = userRepository.findByEmail(prince.getName());
        if (job.getUser().equals(user)) {
            String filename = rootfolder + job.getToken()+"/structure-"+(job.getSequence().length()-1)+"bp-ID"
                    + job.getSeed() + "-B" + boundId + "-" + structureId + ".dat";
            return RefTools.readStepsToJSON(filename);
        }
        log.debug("Did not match");
        return null;
    }

    @ApiOperation(value = "Return the PDB data for a structure")
    @RequestMapping(value = "/structure/{id}/{sid}/{bid}", method = RequestMethod.GET)
    @JsonView(JsonViews.Results.class)
    public String analyzeJobStructures(@PathVariable("id") Long id, @PathVariable("sid") Long structureId,
                                       @PathVariable("bid") Long boundId, Principal prince) {
        if (prince == null) return null;
        Job job = jobService.getOne(id);
        User user = userRepository.findByEmail(prince.getName());
        if (job.getUser().equals(user)) {
            return Analysis.returnStructurePDB(job, structureId, boundId);
        }
        log.debug("Did not match");
        return null;
    }

    @ApiOperation(value = "Get a job analysis by token and code")
    @RequestMapping(value = "/analyzeshare/{token}/{code}", method = RequestMethod.GET)
    @JsonView(JsonViews.Results.class)
    public Results analyzeShared(@PathVariable("token")String token, @PathVariable("code")String code) {
        Job job = jobService.getJobByToken(token);
        if (job == null) return null;
        if (code.equals(job.getId()+"")) {
            return Analysis.analyzeJob(job);
        }
        return null;
    }


    @ApiOperation(value = "Return the PDB data for a job shared by another")
    @RequestMapping(value = "/structureshare/{token}/{code}/{sid}/{bid}")
    public String analyzeSharedStructures(@PathVariable("token")String token, @PathVariable("code")Long code, @PathVariable("sid") Long structureId,
                                          @PathVariable("bid") Long boundId) {
        Job job = jobService.getJobByToken(token);
        if (job == null) return null;
        if (code.equals(job.getId())) {
            return Analysis.returnStructurePDB(job, structureId, boundId);
        }
        return null;
    }

    @ApiOperation(value = "Get the data for a specified force field as a gzip archive")
    @RequestMapping(value = "/jobs/{id}.tar.gz", method = RequestMethod.GET) //, produces = "application/gzip")
    @ResponseBody
    public byte[] getJobZipData(@PathVariable("id") Long id, Principal prince) throws IOException {
        log.info("Attempting to send the gzip file");
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        if (user == null) return null;
        Job job = jobService.getOne(id);
        if (!job.getUser().equals(user)) return null;
        if (!(new File("jobs/"+job.getToken()+"/result.tar.gz").exists()))
            Analysis.getZipFile(job);
        InputStream in = new FileInputStream(new File("jobs/"+job.getToken()+"/result.tar.gz"));
        if (in == null) return null;
        return IOUtils.toByteArray(in);
    }


}
