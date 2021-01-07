package edu.dnatools.controller;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import edu.dnatools.model.*;
import edu.dnatools.repository.UserRepository;
import edu.dnatools.service.BoundaryConditionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import static edu.dnatools.utils.RefTools.*;

/**
 * Created by luke on 6/13/17.
 */
@Api("Get data about boundary conditions and create new ones")
@RestController
public class BoundaryController {

    public static Logger log = LoggerFactory.getLogger(BoundaryController.class);
    public static Gson gson = new GsonBuilder().create();

    @Autowired
    BoundaryConditionService boundaryConditionService;

    @Autowired
    UserRepository userRepository;

    @ApiOperation(value = "Get all BCs")
    @RequestMapping(value = "/bcs", method = RequestMethod.GET)
    public List<BoundaryCondition> getBoundaryConditions(Principal prince) {
        return boundaryConditionService.getAll();
    }

    @ApiOperation(value = "Add new BC")
    @RequestMapping(value = "/addbc", method = RequestMethod.PUT)
    @JsonView(JsonViews.BoundaryCondition.class)
    public ResponseEntity addNewBoundaryCondition(@RequestBody BoundaryCondition bc, Principal prince) {

        log.debug(bc.getTilt() + " " + bc.getRoll() + "... " + bc.getName());
        if (prince != null) {
            User user = userRepository.findByEmail(prince.getName());
            if (user != null) {
                bc.setUser(user);
                if (boundaryConditionService.add(bc) != null) {
                    return new ResponseEntity("OK", HttpStatus.OK);
                }
            }
        }
        return new ResponseEntity("ERROR", HttpStatus.BAD_REQUEST);
    }

    @ApiOperation("Return the base-pair reference frames of a job")
    @RequestMapping(value = "/getparameters", method = RequestMethod.POST)
    @JsonView(JsonViews.PDBinput.class)
    public String readParameters(@RequestBody PDBinput pdb) {
        if (pdb.getPdbs() == null) {
            log.debug("No data");
            return null;
        }
        log.debug("Analyzing PDB file of size " + pdb.getPdbs().length());
        return getParameters(pdb.getPdbs());
    }

    @ApiOperation("Return the base-pair reference frames of a job")
    @RequestMapping(value = "/getrefframes", method = RequestMethod.POST)
    @JsonView(JsonViews.PDBinput.class)
    public String readReferenceFrames(@RequestBody PDBinput pdb) {
        if (pdb.getPdbs() == null) {
            log.debug("No data");
            return null;
        }
        log.debug("Analyzing PDB file of size " + pdb.getPdbs().length());
        return analyzeReferenceFrames(pdb.getPdbs());
    }

    @ApiOperation("Calculate the step between two reference frames")
    @RequestMapping(value = "/calculateref", method = RequestMethod.PUT)
    public String calculateBoundaryCondition(@RequestBody String data) {

        double[][][] refs = gson.fromJson(data, double[][][].class);
        if (refs == null) {
            log.debug("Something wrong with matrix");
            return null;
        }

        log.debug(data);

        double[][] A1 = refs[0];
        double[][] A2 = refs[1];

        double[] result = calculatetp(multiply(invert(A1), A2));

        return gson.toJson(result);

    }


}
