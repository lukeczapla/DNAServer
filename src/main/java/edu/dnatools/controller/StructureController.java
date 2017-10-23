package edu.dnatools.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import edu.dnatools.model.*;
import edu.dnatools.repository.UserRepository;
import edu.dnatools.service.RCSBService;
import edu.dnatools.utils.ForceField;
import edu.dnatools.utils.ForceFieldAnalysis;
import edu.dnatools.utils.Processor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 * Created by luke on 7/11/17.
 */
@Api("Get information about data in the RCSB Protein Databank")
@RestController
public class StructureController {

    @Autowired
    UserRepository userRepository;
    @Autowired
    RCSBService rcsbService;
    @Autowired
    ServletContext servletContext;


    @ApiOperation(value = "Analyze a list of PDB files on RCSB for creating a force field")
    @RequestMapping(value = "/analyzePDBs", method = RequestMethod.POST)
    @JsonView(JsonViews.PDBinput.class)
    public String runFullAnalysis(@RequestBody PDBinput pdb, Principal prince) {
        if (prince == null) return null;
        System.out.println(pdb.getPdbList());
        System.out.println(pdb.isNonredundant());
        System.out.println("Only considering dates after " + pdb.getAfterDate());
        System.out.println("Only considering dates before " + pdb.getBeforeDate());

        // create the model with data
        ForceField ff = new ForceField(pdb, rcsbService);

        RCSBAnalysis record = RCSBAnalysis.createFromPDBinput(pdb);
        User user = userRepository.findByEmail(prince.getName());
        record.setUser(user);
        record.setCode(ff.getCode());
        rcsbService.add(record);
        return ff.getCode();
    }

    @ApiOperation(value = "Get force field calculations for the current user")
    @RequestMapping(value = "/myforcefields", method = RequestMethod.GET)
    @JsonView(JsonViews.RCSBAnalysis.class)
    public List<RCSBAnalysis> getMyForceFields(Principal prince) {
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        if (user == null) return null;
        return rcsbService.getRCSBAnalysisByUser(user);
    }

    @ApiOperation(value = "Get the data for a specified force field as a zip archive")
    @RequestMapping(value = "/forcefield/{id}.zip", method = RequestMethod.GET, produces = "application/zip")
    @ResponseBody
    public byte[] getForceFieldData(@PathVariable("id") Long id, Principal prince) throws IOException {
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        if (user == null) return null;
        RCSBAnalysis rcsbAnalysis = rcsbService.getOne(id);
        if (!rcsbAnalysis.getUser().equals(user)) return null;
        ForceFieldAnalysis.getZipFile(rcsbAnalysis);
        InputStream in = new FileInputStream(new File("ff/"+rcsbAnalysis.getCode()+"/ff.zip"));
        if (in == null) return null;
        return IOUtils.toByteArray(in);
    }


    @ApiOperation(value = "Get force field calculations for the current user")
    @RequestMapping(value = "/myforcefield/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.StepLibrary.class)
    @ResponseBody
    public StepLibrary getForceFieldLibrary(@PathVariable("id") Long id, Principal prince) throws IOException {
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        if (user == null) return null;
        RCSBAnalysis rcsbAnalysis = rcsbService.getOne(id);
        if (rcsbAnalysis == null) return null;
        return ForceFieldAnalysis.loadForceField(rcsbAnalysis);
    }

    @ApiOperation(value = "Get force field calculations for the current user")
    @RequestMapping(value = "/myforcefield2/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.StepLibrary.class)
    @ResponseBody
    public StepLibrary getForceFieldLibraryDimers(@PathVariable("id") Long id, Principal prince) throws IOException {
        if (prince == null) return null;
        User user = userRepository.findByEmail(prince.getName());
        if (user == null) return null;
        RCSBAnalysis rcsbAnalysis = rcsbService.getOne(id);
        if (rcsbAnalysis == null) return null;
        return ForceFieldAnalysis.loadForceFieldDimers(rcsbAnalysis);
    }

}
