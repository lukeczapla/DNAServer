
var boundaryConditions = {};
var stepparameters = new Array(19);
var forceconstants = new Array(19);
var steps = ["CG", "CA", "TA", "AG", "GG", "AA", "GA", "AT", "AC", "GC", "ZZ", "ZA", "ZT", "ZG", "ZC",
    "AZ", "TZ", "GZ", "CZ"];
var useRNA = false;

var dimerstepparameters;
var dimerforceconstants;

let invalidChars = /[^AaTtGgCcZz]/gi;

let step = 0;
let nproteins = 0;
let loadedList = false;

window.onload = function () {
    $.ajaxSetup({
        beforeSend: function(xhr, settings) {
            if (settings.url.indexOf('/nucleic') != 0 && settings.url.indexOf('http') != 0)
                settings.url = '/nucleic' + settings.url;
        }
    });

    google.accounts.id.initialize({
        client_id: "613395107842-nmr1dthih3c5ibcfcsrrkq61ef838ks8.apps.googleusercontent.com",
        callback: onSignIn
    });
    google.accounts.id.renderButton(
        this.document.getElementById("gButtonDiv"),
        { theme: "outline", size: "medium", text: "sign_in_with"}    
    );
    console.log("Starting up");
    readGrounds();

    for (let n = 0; n < 19; n++) {
    stepparameters[n] = new Array(6);
    stepparameters[n][0] = 0.0;
    stepparameters[n][1] = 0.0;
    stepparameters[n][2] = 34.286;
    stepparameters[n][3] = 0.0;
    stepparameters[n][4] = 0.0;
    stepparameters[n][5] = 3.4;

    forceconstants[n] = new Array(6);
    forceconstants[n][0] = [0.0427, 0.0, 0.0, 0.0, 0.0, 0.0];
    forceconstants[n][1] = [0.0, 0.0427, 0.0, 0.0, 0.0, 0.0];
    forceconstants[n][2] = [0.0, 0.0, 0.0597, 0.0, 0.0, 0.0];
    forceconstants[n][3] = [0.0, 0.0, 0.0, 50.0, 0.0, 0.0];
    forceconstants[n][4] = [0.0, 0.0, 0.0, 0.0, 50.0, 0.0];
    forceconstants[n][5] = [0.0, 0.0, 0.0, 0.0, 0.0, 50.0];

    }

    for (let n = 0; n < 136; n++) {
        tetramerstepparameters[n] = new Array(6);
        tetramerstepparameters[n][0] = 0.0;
        tetramerstepparameters[n][1] = 0.0;
        tetramerstepparameters[n][2] = 34.286;
        tetramerstepparameters[n][3] = 0.0;
        tetramerstepparameters[n][4] = 0.0;
        tetramerstepparameters[n][5] = 3.4;

        tetramerforceconstants[n] = new Array(6);
        tetramerforceconstants[n][0] = [0.0427, 0.0, 0.0, 0.0, 0.0, 0.0];
        tetramerforceconstants[n][1] = [0.0, 0.0427, 0.0, 0.0, 0.0, 0.0];
        tetramerforceconstants[n][2] = [0.0, 0.0, 0.0597, 0.0, 0.0, 0.0];
        tetramerforceconstants[n][3] = [0.0, 0.0, 0.0, 50.0, 0.0, 0.0];
        tetramerforceconstants[n][4] = [0.0, 0.0, 0.0, 0.0, 50.0, 0.0];
        tetramerforceconstants[n][5] = [0.0, 0.0, 0.0, 0.0, 0.0, 50.0];
    }

    $("#tp0").val(0.0);
    $("#tp1").val(0.0);
    $("#tp2").val(34.286);
    $("#tp3").val(0.0);
    $("#tp4").val(0.0);
    $("#tp5").val(3.4);

    $("#M00").val(0.0427);
    $("#M01").val(0.0);
    $("#M02").val(0.0);
    $("#M03").val(0.0);
    $("#M04").val(0.0);
    $("#M05").val(0.0);

    $("#M11").val(0.0427);
    $("#M12").val(0.0);
    $("#M13").val(0.0);
    $("#M14").val(0.0);
    $("#M15").val(0.0);

    $("#M22").val(0.0597);
    $("#M23").val(0.0);
    $("#M24").val(0.0);
    $("#M25").val(0.0);

    $("#M33").val(50.0);
    $("#M34").val(0.0);
    $("#M35").val(0.0);

    $("#M44").val(50.0);
    $("#M45").val(0.0);

    $("#M55").val(50.0);

    registered();
    console.log("Set step parameters and force constants");
}


function RNAmode() {
    $("#bpselect option").each(function() {
        $(this).html($(this).html().replace("T", "U"));
    });
    for (let index = 0; index < steps.length; index++) {
        steps[index] = steps[index].replace("T", "U");
    }
    for (let index = 0; index < tetramerSteps.length; index++) {
        tetramerSteps[index] = tetramerSteps[index].replace("T", "U");
    }
    invalidChars = /[^AaUuGgCcZz]/gi;
    $("#sequence").val($("#sequence").val().replace(/t/g, "U").replace(/T/g, "U"));
    useRNA = true;
    unPrep();
    $("#rnaoff").show();
    $("#rnaon").hide();
    refreshData();
}


function RNAmodeOff() {
    $("#bpselect option").each(function() {
        $(this).html($(this).html().replace("U", "T"));
    });
    for (let index = 0; index < steps.length; index++) {
        steps[index] = steps[index].replace("U", "T");
    }
    for (let index = 0; index < tetramerSteps.length; index++) {
        tetramerSteps[index] = tetramerSteps[index].replace("U", "T");
    }

    $("#sequence").val($("#sequence").val().replace(/u/g, "T").replace(/U/g, "T"));
    useRNA = false;
    unPrep();
    $("#rnaon").show();
    $("#rnaoff").hide();
    refreshData();
}


function refreshData() {
  $('#myffs').empty();
  $('#myjobs').empty();
  $('#myjobs2').empty();
  $('#custom0').empty(); $('#custom1').empty(); $('#custom2').empty(); $('#custom3').empty();
  $('#proteins').empty();
  $('#proteins').append('<option value=-1>no proteins</option>');
  $('#proteins2').empty();
  $('#proteins3').empty();
  $('#tetramerList').empty();
  $('#dimerList').empty();
  $('#custom0').append('<option value=-1>Custom</option>');
  $('#custom1').append('<option value=-1>Custom</option>');
  $('#custom2').append('<option value=-1>Custom</option>');
  $('#custom3').append('<option value=-1>Custom</option>');
  for (let i = 0; i <= fixedn; i++) {
    $('#fixed'+i).empty();
  }
  loadJobList();
  loadProteinList();
  loadBoundaryConditions();
  loadForceFieldList();
  loadTetranucleotideList();
  loadDinucleotideList();
  loadedList = true;
}

function isListLoaded() {
    return loadedList;
}

function tetramerOn() {
    usetetramer = true;
    dimerstepparameters = stepparameters;
    dimerforceconstants = forceconstants;
    stepparameters = tetramerstepparameters;
    forceconstants = tetramerforceconstants;
    $("#bpselect option").remove()
    for (let i = 0; i < forceconstants.length; i++) {
        $("#bpselect").append($("<option>", {"value": i, text: tetramerSteps[i] + " step"}));
    }
    step = 0;

      for (let i = 0; i < 6; i++) {
         $("#tp"+i).val(stepparameters[step][i].toFixed(5));
       }

       for (let i = 0; i < 6; i++)
          for (let j = i; j < 6; j++) {
             $("#M"+i+j).val(forceconstants[step][i][j].toFixed(5));
          }
    usetetramer = true;
        $("#teton").hide();
        $("#tetoff").show();

}


function tetramerOff() {
    tetramerstepparameters = stepparameters;
    tetramerforceconstants = forceconstants;
    stepparameters = dimerstepparameters;
    forceconstants = dimerforceconstants;
    $("#bpselect option").remove()
    for (let i = 0; i < forceconstants.length; i++) {
        $("#bpselect").append($("<option>", {"value": i, text: steps[i] + " step"}));
    }
    step = 0;
      for (let i = 0; i < 6; i++) {
         $("#tp"+i).val(stepparameters[step][i].toFixed(5));
       }

       for (let i = 0; i < 6; i++)
          for (let j = i; j < 6; j++) {
             $("#M"+i+j).val(forceconstants[step][i][j].toFixed(5));
          }
        usetetramer = false;
        $("#teton").show();
        $("#tetoff").hide();
}

function loadFile() {

    let input = document.getElementById('fileinput');
    if (!input.files[0]) {
      alert("Please select a file before clicking 'Load'");
      return;
    }
    file = input.files[0];
    fr = new FileReader();
    fr.onload = receivedText;
    fr.readAsText(file);

    function receivedText(e) {
      let loadedData = JSON.parse(e.target.result);
      console.log(loadedData);
      forceconstants = loadedData.forceconstants;
      stepparameters = loadedData.stepparameters;
      if (forceconstants.length == 136) {
        $("#bpselect option").remove()
        for (let i = 0; i < forceconstants.length; i++) {
            $("#bpselect").append($("<option>", {"value": i, text: tetramerSteps[i] + " step"}));
            step = 0;
        }
        usetetramer = true;
        setupTetramers();
      }
      for (let i = 0; i < 6; i++) {
         $("#tp"+i).val(stepparameters[step][i].toFixed(5));
       }

       for (let i = 0; i < 6; i++)
          for (let j = i; j < 6; j++) {
             $("#M"+i+j).val(forceconstants[step][i][j].toFixed(5));
          }
       }

}

function saveFile() {
    saveAndLoad();
    let scalingfactor = parseFloat($("#sfactor").val());
    for (let n = 0; n < 19; n++) {
        for (let m = 0; m < 6; m++) {
          stepparameters[n][m] = parseFloat(stepparameters[n][m].toFixed(3));
          for (let l = 0; l < 6; l++) {
             if ($("#exportscaled").is(":checked")) {
                forceconstants[n][m][l] = parseFloat((scalingfactor*forceconstants[n][m][l]).toFixed(4));
             } else forceconstants[n][m][l] = parseFloat(forceconstants[n][m][l].toFixed(4));
          }
        }
    }
    let filename = "parameters.txt";
    let data = JSON.stringify({
        "forceconstants": forceconstants,
        "stepparameters": stepparameters
    });
    let array = [data];
    let blob = new Blob(array, {type:"text/plain;charset=utf-8"});
    saveAs(blob, filename);
    if ($("#exportscaled").is(":checked")) {
            for (let i = 0; i < 6; i++) {
                for (let j = i; j < 6; j++) {
                    $("#M"+i+j).val(forceconstants[$("#bpselect").val()][i][j].toFixed(5));
                }
            }
            $("#sfactor").val(1.0);
    }
}


function saveAndLoad() {
    if (stepparameters == null || stepparameters.length == 0) {
        stepparameters = [0,0,34.28,0,0,3.4];
    }
    if (forceconstants == null || forceconstants.length == 0) {
        forceconstants = [[],[],[],[],[],[]];
        for (let i = 0; i < 6; i++) {
            forceconstants[i] = [0,0,0,0,0,0];
        }
    }
    for (let i = 0; i < 6; i++) {
        stepparameters[step][i] = parseFloat($("#tp"+i).val());
        $("#tp"+i).val(stepparameters[$("#bpselect").val()][i].toFixed(5));
    }

    for (let i = 0; i < 6; i++)
        for (let j = i; j < 6; j++) {
            forceconstants[step][i][j] = parseFloat($("#M"+i+j).val())
            forceconstants[step][j][i] = parseFloat($("#M"+i+j).val())
            $("#M"+i+j).val(forceconstants[$("#bpselect").val()][i][j].toFixed(5));
        }
    step=$("#bpselect").val();

}

function saveSelectedStep() {

    for (let i = 0; i < 6; i++) {
        stepparameters[step][i] = parseFloat($("#tp"+i).val());
    }
    for (let i = 0; i < 6; i++)
        for (let j = i; j < 6; j++) {
            forceconstants[step][i][j] = parseFloat($("#M"+i+j).val());
            forceconstants[step][j][i] = parseFloat($("#M"+i+j).val());
        }
}


function showEigen() {
    saveSelectedStep();
    result = jeigen(forceconstants[step]);
    for (let i = 0; i < 6; i++) {
        for (let j = 0; j < 6; j++) {
            $("#E"+i+j).html(result.eigenvectors[i][j].toFixed(5));
        }
        $("#EV"+i).html(result.eigenvalues[i].toFixed(5));
    }
}


function validateSeq(e) {
    if (invalidChars.test(e.value)) {
        e.value = e.value.replace(invalidChars,"");
    }
}

function validateList(e) {
    e.value = e.value.replace(","," ");
    let invalidvalues = /[^A-Za-z0-9 ]/g;
    e.value = e.value.replace(invalidvalues, "");
}


function loadForceFieldList() {
    console.log("load force fields");
    $.ajax({url: "/myforcefields", method: "GET"}).done(function(data) {
             for (let i = 0; i < data.length; i++) {
               if (data[i].description == null) {
                 data[i].description = "no description";
               }
               $('#myffs').append('<option value=' + data[i].id + '>' + data[i].code + " " + data[i].description + '</option>');
             }
    });
}


function loadJobList() {
    console.log("load calculations");
    $.ajax({url: "/myjobs", method: "GET"}).done(function(data) {
             for (let i = 0; i < data.length; i++) {
               if (data[i].description === undefined) data[i].description = "N.A.";
               $('#myjobs').append('<option value=' + data[i].id + '>' + data[i].token + ' ' + (data[i].sequence.length-1) + ' bp steps ' + data[i].description + '</option>');
               $('#myjobs2').append('<option value=' + data[i].id + '>' + data[i].token + ' ' + (data[i].sequence.length-1) + ' bp steps ' + data[i].description + '</option>');
             }
    });
    loadedList = true;
}

function loadProteinList(selector) {

    console.log("load proteins");
    $.ajax({url: "/getplist", method: "GET"}).done(function(data) {
            for (let i = 0; i < data.length; i++) {
              if (selector === undefined) {
                for (let j = 0; j <= fixedn; j++) $("#fixed"+j).append('<option value=' + data[i][0] + '>' + data[i][1] + '</option>');
                $('#proteins').append('<option value=' + data[i][0] + '>' + data[i][1] + '</option>');
                $('#proteins2').append('<option value=' + data[i][0] + '>' + data[i][1] + '</option>');
                $('#proteins3').append('<option value=' + data[i][0] + '>' + data[i][1] + '</option>');
              } else $(selector).append('<option value=' + data[i][0] + '>' + data[i][1] + '</option>');
            }
    }).error(function(data) {
        console.log(data);
    });

}

function loadBoundaryConditions() {
    console.log("load boundary conditions");
    $.ajax({url: "/bcs", method: "GET"}).done(function(data) {
        boundaryConditions = data;
        for (let i = 0; i < data.length; i++) {
            $('#custom0').append('<option value=' + data[i].id + '>' + data[i].name + '</option>');
            $('#custom1').append('<option value=' + data[i].id + '>' + data[i].name + '</option>');
            $('#custom2').append('<option value=' + data[i].id + '>' + data[i].name + '</option>');
            $('#custom3').append('<option value=' + data[i].id + '>' + data[i].name + '</option>');
        }
    }).error(function(data) {
        console.log(data);
    });
}

function deleteJobs() {

    $("#myjobs option").each(function () {
        if ($(this).is(':selected')) {
            let conBox = confirm("Are you sure you want to delete: " + $(this).html());
            if (conBox) {
                $.ajax({url: "/delete/" + $(this).val(),
                    method: "DELETE"})
                $(this).remove();
            }
        }
    })
}

function submitJob() {
    console.log("Submitting...")

    saveSelectedStep();
    let bounds=[];
    if ($("#sequence").val().length < 2) {
        alert("You need to enter a valid sequence in the Sequence tab");
        return;
    }
    let scalingfactor = parseFloat($("#sfactor").val());
    let forceconstantsjob = [];
    for (let n = 0; n < 19; n++) {
            forceconstantsjob.push(forceconstants[n]);
            for (let m = 0; m < 6; m++) {
              for (let l = 0; l < 6; l++) {
                 forceconstantsjob[n][m][l] = scalingfactor*forceconstantsjob[n][m][l];
              }
            }
     }

    if ($("#useBounds").is(':checked')) for (let i = 0; i < 4; i++) {
        if ($("#b"+i).is(':checked')) {
            if ($("#custom"+i).val() == -1) {
                let bc = [parseFloat($("#b"+i+0).val()), parseFloat($("#b"+i+1).val()), parseFloat($("#b"+i+2).val()),
                        parseFloat($("#b"+i+3).val()), parseFloat($("#b"+i+4).val()), parseFloat($("#b"+i+5).val())];
                bounds.push(bc);
            } else {
                for (let j = 0; j < boundaryConditions.length; j++) {
                    if ($("#custom"+i).val() == boundaryConditions[j].id) {
                        let bc = [boundaryConditions[j].tilt, boundaryConditions[j].roll,
                                    boundaryConditions[j].twist, boundaryConditions[j].shift,
                                    boundaryConditions[j].slide, boundaryConditions[j].rise];
                        bounds.push(bc);
                    }
                }
            }
        }
    }

    let jobdata = {
        "description": $("#jobdescription").val(),
        "sequence": $("#sequence").val().toUpperCase(),
        "forceConstants": JSON.stringify(forceconstantsjob),
        "stepParameters": JSON.stringify(stepparameters),
        "nChains": $("#nChains").val(),
        "rBounds": $("#rBounds").val(),
        "gBounds": $("#gBounds").val(),
        "twBounds": $("#twBounds").val(),
        "nProteins": nproteins,
        "bc": JSON.stringify(bounds),
        "suppressImages": $("#suppressImages").is(":checked"),
        "useLargeBins": $("#useLargeBins").is(":checked"),
        "hasFixedProteins": useFixed,
    };
    if (useFixed) {
        fixedProteins = [];
        for (let i = 0; i <= fixedn; i++) {
           let data = [];
           data.push(parseInt($("#fixed"+i).val()));
           data.push(parseInt($("#fixedpos"+i).val()));
           fixedProteins.push(data);
        }
        jobdata.fixedProteins = JSON.stringify(fixedProteins);
        console.log(JSON.stringify(fixedProteins));
    } else if ($("#proteins").val() > 0) {
        jobdata.hasProteins = true;
        jobdata.proteinId = $("#proteins").val();
        jobdata.bindingProbability = parseFloat($("#bindingP").val());
        if ($("#bindingP").val() == null || jobdata.bindingProbability <= 0 || jobdata.bindingProbability > 1) {
            alert("Binding probability not correctly set for non-specific proteins (0 > p > 1");
            return;
        }

    }


    let req = {
        headers: {
            'Content-Type': 'application/json'
        },
        url: "/submit",
        method: "PUT",
        data: JSON.stringify(jobdata)
    };
    $.ajax(req).done(function(response) {
        console.log(response);
        result = JSON.parse(response);
        if (result.token != null) {
            alert("Your job is submitted and the token is: " + result.token);
            $('#jobmessage').html('Current job is ' + result.token);
            refreshData();
        } else {
            console.log(response);
            alert("Error occurred");
        }
    });
}



function addProtein() {
    let proteinData = {
        name: $("#nameP").val(),
        number: $("#nP").val(),
        stepLength: $("#stepsP").val(),
        dats: $("#stepData").val(),
        pdbs: $("#proteinPDB").val()
    };

    console.log(proteinData);

    let req = {
                 headers: {
                     'Content-Type': 'application/json'
                 },
                 url: "/addprotein",
                 method: "PUT",
                 data: JSON.stringify(proteinData)
               };

    $.ajax(req).done(function(response) {
        console.log(response)
        if (response == "OK") {
            alert("Success");
            $("#nameP").val("");
            $("#nP").val("");
            $("#stepsP").val("");
            $("#stepData").val("");
            $("#proteinPDB").val("");
        }
    }).error(function(response) {
        alert("Check your protein name and data, response: " + response);
    });
}


function generate() {
  if ($("#nsd").val() < 1) return;
  let s = "Z";
  for (let i = 0; i < $("#nsd").val(); i++) s += 'Z';
  $("#sequence").val(s);
}

function randomBase() {
  let r = Math.floor(4*Math.random());
  switch (r) {
    case 0: return 'A';
    case 1: return 'G';
    case 2: if (useRNA == true) return 'U'; else return 'T';
    case 3: return 'C';
  }
}

function bnum(x) {
    switch (x) {
        case 'A': return 0;
        case 'C': return 1;
        case 'G': return 2;
        case 'T': return 3;
        case 'U': return 3;
    }
}


function generateRandom() {
  if ($("#nsd").val() < 1) return;
  let s = randomBase();
  for (let i = 0; i < $("#nsd").val(); i++) {
    s += randomBase();
  }
  $("#sequence").val(s);
}