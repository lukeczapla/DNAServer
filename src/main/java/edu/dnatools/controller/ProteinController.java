package edu.dnatools.controller;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import edu.dnatools.model.JsonViews;
import edu.dnatools.model.Protein;
import edu.dnatools.model.ProteinStructure;
import edu.dnatools.repository.ProteinRepository;
import edu.dnatools.repository.ProteinStructureRepository;
import edu.dnatools.service.ProteinService;
import edu.dnatools.service.ProteinStructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Created by luke on 6/10/17.
 */

@RestController
@Api("Get and set proteins in the database, also structures for visualization")
public class ProteinController {

    private static final Logger log = LoggerFactory.getLogger(ProteinController.class);

    @Autowired
    private ProteinService proteinService;
    @Autowired
    private ProteinStructureService proteinStructureService;


    @ApiOperation("Add a new protein to the database")
    @RequestMapping(value = "/addprotein", method = RequestMethod.POST)
    @JsonView(JsonViews.Protein.class)
    public ResponseEntity<String> addProtein(@RequestBody Protein protein, Principal prince) {
        Protein result = proteinService.add(protein);
        //log.info(protein.getDats());
        if (result != null) return new ResponseEntity<>("OK", HttpStatus.OK);
        return new ResponseEntity<>("Already exists", HttpStatus.BAD_REQUEST);
    }

    @ApiOperation("Delete a protein from the database")
    @RequestMapping(value = "/deleteprotein/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.Protein.class)
    public String deleteProtein(@PathVariable("id")Long Id, Principal prince) {
        if (prince != null) {
            proteinService.delete(Id);
            return "OK";
        } return "ERROR";
    }

    @ApiOperation("Update a protein in the database")
    @RequestMapping(value = "/renameprotein/{id}", method = RequestMethod.POST)
    @JsonView(JsonViews.Protein.class)
    public Protein updateProtein(@PathVariable("id")Long Id, @RequestBody String name, Principal prince) {
        if (prince != null) {
            Protein p = proteinService.getOne(Id);
            p.setName(name);
            proteinService.update(Id, p);
            return p;
        }
        return null;
    }

    @ApiOperation("Get the full data about a protein")
    @RequestMapping(value = "/getprotein/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.Protein.class)
    public Protein getProtein(@PathVariable("id")Long Id, Principal prince) {
        return proteinService.getOne(Id);
    }

    @ApiOperation("Get list of proteins, just name and Id")
    @RequestMapping(value = "/getplist", method = RequestMethod.GET)
    @JsonView(JsonViews.Protein.class)
    public List<Protein> getProteinList() {
        return proteinService.getIdAndNameOnly();
    }


    @ApiOperation("Get list of protein structures (for drawing), just name and id")
    @RequestMapping(value = "/structurelist", method = RequestMethod.GET)
    @JsonView(JsonViews.ProteinStructure.class)
    public List<ProteinStructure> getStructureList() {
        return proteinStructureService.getIdAndNameOnly();
    }

    @ApiOperation("Get a protein structure by id")
    @RequestMapping(value = "/getstructure/{id}", method = RequestMethod.GET)
    @JsonView(JsonViews.ProteinStructure.class)
    public ProteinStructure getProteinStructure(@PathVariable("id") Long id) {
        return proteinStructureService.getOne(id);
    }

    @ApiOperation("Add a new protein to the database")
    @RequestMapping(value = "/addstructure", method = RequestMethod.POST)
    @JsonView(JsonViews.ProteinStructure.class)
    public ResponseEntity<String> addProteinStructure(@RequestBody ProteinStructure protein, Principal prince) {
        ProteinStructure result = proteinStructureService.add(protein);
        //log.info(protein.getDats());
        if (result != null) return new ResponseEntity<>("OK", HttpStatus.OK);
        return new ResponseEntity<>("Already exists", HttpStatus.BAD_REQUEST);
    }

}
