var simdata;
var workerThread = null;
var simresult;
var seq;
let nruns = 10000;
// numbers for randn() function, we'll keep them in this script's scope
let iset = 0;
let gset;
let simchartlist = [];

function simplot(vec, chartname, title, pmf) {
  let ctx = document.getElementById(chartname).getContext("2d");
  let data = [];
  //console.log(vec[0][0]);

  let start = 0;
  let end = vec.length-1;
  for (let i = 0; i < vec.length; i++) {
    if (vec[i][1] != 0.0) break;
    start++;
  }
  for (let i = vec.length-1; i >= 0; i--) {
    if (vec[i][1] != 0.0) break;
    end--;
  }

  for (let i = start; i <= end; i++) {
    let point = {x : vec[i][0], y: vec[i][1]};
    data.push(point);
  }

  let chartformat = {
        type: 'scatter',
        data: {
            datasets: [{
              backgroundColor: 'rgba(0, 0, 255, 0.4)',
              label: title,
              data: data
            }]
        },
        options: {
            legend: {
              labels: {
                fontSize: fontsize
              }
            },
            scales: {
              xAxes: [{
                       scaleLabel: {
                          display: true,
                          fontSize: fontsize,
                          fontFamily: "Verdana",
                          labelString: "end-to-end distance (Å)"
                       },
                       ticks: {
                          fontSize: fontsize,
                          fontFamily: "AndaleMono"
                       },
                       type: 'linear',
                       position: 'bottom',
                       scaleFontSize: 32
              }],
              yAxes: [{
                       scaleLabel: {
                          display: true,
                          fontSize: fontsize,
                          fontFamily: "Verdana",
                          labelString: "Data points"
                       },
                       ticks: {
                            min:0,
                            fontSize: fontsize,
                            fontFamily: "AndaleMono"
                       },
                       type: 'linear',
                       position: 'left',
                       scaleFontSize: 32
              }]
            }
        }
  };
  if (pmf !== undefined) {
    chartformat.options.scales.yAxes[0].ticks.max = 0;
    delete chartformat.options.scales.yAxes[0].ticks.min;
  }
  let scatterChart = new Chart(ctx, chartformat);
  simchartlist.push(scatterChart);

}



function complement(s, rna) {
    let result = "";
    if (s.charAt(1) == 'C')  result = "G";
    if (s.charAt(1) == 'T' || s.charAt(1) == 'U') result = "A";
    if (s.charAt(1) == 'G')  result = "C";
    if (s.charAt(1) == 'A') {
      if (rna === undefined || rna == false) result = "T"; else result = 'U';
    }
    if (s.charAt(0) == 'C')  result += "G";
    if (s.charAt(0) == 'T' || s.charAt(0) == 'U') result += "A";
    if (s.charAt(0) == 'G')  result += "C";
    if (s.charAt(0) == 'A') {
        if (rna === undefined || rna == false) result += "T";
        else result += "U";
    }
    return result;
}

function addExtraSteps(rna) {
    for (let i = 0; i < 10; i++) {
        let complementstep = complement(steps[i], rna);
        let loc = -1;
        for (let j = 0; j < 10; j++) {
            if (steps[j] == complementstep) loc = j;
        }
        if (loc == -1) {
            steps.push(complementstep);
            let sp = numeric.clone(stepparameters[i]);
            sp[0] = -sp[0];  sp[3] = -sp[3];
            let Fc = [];
            for (let j = 0; j < 6; j++) {
              let row = [];
              for (let k = 0; k < 6; k++) {
                if ((k == 0 && j != 3 && j != 0) || (k == 3 && j != 0 && j != 3) ||
                    (j == 0 && k != 3 && k != 0) || (j == 3 && k != 0 && k != 3)) row.push(-forceconstants[i][j][k]);
                else row.push(forceconstants[i][j][k]);
              }
              Fc.push(row);
            }
            stepparameters.push(sp);
            forceconstants.push(Fc);
        }
    }
    //console.log(JSON.stringify(steps));
    //console.log(JSON.stringify(stepparameters));
}

function removeExtraSteps() {
    forceconstants = forceconstants.slice(0, 19);
    stepparameters = stepparameters.slice(0, 19);
    steps = steps.slice(0, 19);
}

function calculateEndA(simd) {
    let A = [[1.0,0,0,0],[0,1.0,0,0], [0,0,1.0,0], [0,0,0,1.0]];
    for (let i = 0; i < simd.length; i++) {
        A = numeric.dot(A, calculateA(simd[i]));
    }
    return A;
}

function calculateAlist(simd) {
    let A = new Array(simd.length+1);
    A[0] = [[1.0,0,0,0],[0,1.0,0,0], [0,0,1.0,0], [0,0,0,1.0]];
    for (let i = 0; i < simd.length; i++) {
        A[i+1] = numeric.dot(A[i], calculateA(simd[i]));
    }
    return A;
}


function calculatePersistenceLength(rna) {
    seq = $("#sequence").val().toUpperCase();
    if (seq.length < 2) {
        alert("No sequence entered");
        return;
    }
    let scalingfactor = $("#sfactor").val();
    saveSelectedStep();
    addExtraSteps(rna);
    let stepindex = [];
    for (let i = 0; i < seq.length-1; i++) {
        let stp = seq.charAt(i) + seq.charAt(i+1);
        //console.log(stp);
        for (let j = 0; j < steps.length; j++) {
            if (stp == steps[j]) stepindex.push(j);
        }
    }
    //console.log(JSON.stringify(stepindex));

    let ns = stepparameters.length;

    let eigen = new Array(ns);
    let factor = new Array(ns);
    for (let i = 0; i < ns; i++) {
        eigen[i] = jeigen(forceconstants[i]);
        let s = [];
        for (let j = 0; j < 6; j++) {
            s.push(1.0/Math.sqrt(scalingfactor*eigen[i].eigenvalues[j]));
        }
        factor[i] = s;
    }

    simdata = new Array(stepindex.length);
    for (let i = 0; i < stepindex.length; i++) {
        let vals = [];
        for (let j = 0; j < 6; j++) {
            vals.push(0.0);
        }
        simdata[i] = vals;
    }
    let average = [[0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0],[0.0,0.0,0.0,0.0]];
    for (let runs = 0; runs < nruns; runs++) {
      for (let i = 0; i < stepindex.length; i++) {
        let s = stepindex[i];
        for (let j = 0; j < 6; j++) {
            simdata[i][j] = randn()*factor[s][j];
        }
        simdata[i] = numeric.add(numeric.dot(eigen[s].eigenvectors, simdata[i]), stepparameters[s]);
    }
    average = numeric.add(average, calculateEndA(simdata));
  }
  let Alist = calculateAlist(simdata);
  buildStructure(seq, Alist);
  average = numeric.mul(average, 1.0/nruns);
  for (let i = 0; i < 2000; i++) {
    average = numeric.dot(average, average);
  }
  console.log(average[0][3] + " " + average[1][3] + " " + average[2][3]);
  let plength = Math.sqrt(average[0][3]*average[0][3]+average[1][3]*average[1][3]+average[2][3]*average[2][3]);
  $("#plength").html("Persistence length = " + plength + " Å");
  removeExtraSteps();
}


function sendSimulation() {
    saveSelectedStep();
    addExtraSteps();

    let jobdata = {
        "description": $("#jobdescription").val(),
        "sequence": $("#sequence").val().toUpperCase(),
        "forceConstants": JSON.stringify(forceconstants),
        "stepParameters": JSON.stringify(stepparameters),
        "stepList": JSON.stringify(steps),
        "nChains": $("#nChains").val(),
        "rBounds": $("#rBounds").val(),
        "gBounds": $("#gBounds").val(),
        "twBounds": $("#twBounds").val(),
        "suppressImages": $("#suppressImages").is(":checked"),
        "useLargeBins": $("#useLargeBins").is(":checked"),
        "hasFixedProteins": true,
    };
    let fixedProteins = [];
    for (let i = 0; i <= fixedn; i++) {
       let data = [];
       data.push(parseInt($("#fixed"+i).val()));
       data.push(parseInt($("#fixedpos"+i).val()));
       fixedProteins.push(data);
    }
    jobdata.fixedProteins = JSON.stringify(fixedProteins);
    console.log(JSON.stringify(fixedProteins));
    let req = {
        headers: {
            'Content-Type': 'application/json'
        },
        url: "/calculate",
        method: "PUT",
        data: JSON.stringify(jobdata)
    };

    console.log(jobdata);

    $.ajax(req).done(function(data) {
        console.log("done");
        removeExtraSteps();
    }).error(function(data) { removeExtraSteps(); });

}



function drawEquilibrium() {
    seq = $("#sequence").val().toUpperCase();
    if (seq.length < 2) return;
    saveSelectedStep();
    addExtraSteps();
    let stepindex = [];
    for (let i = 0; i < seq.length-1; i++) {
        let stp = seq.charAt(i) + seq.charAt(i+1);
        //console.log(stp);
        for (let j = 0; j < steps.length; j++) {
            if (stp == steps[j]) stepindex.push(j);
        }
    }
    simdata = new Array(stepindex.length);
    for (let i = 0; i < stepindex.length; i++) {
        let s = stepindex[i];
        let parms = [];
        for (let j = 0; j < 6; j++) {
            parms.push(stepparameters[s][j]);
        }
        simdata[i] = parms;
    }
    let Alist = calculateAlist(simdata);
    buildStructure(seq, Alist);
    console.log(Alist[simdata.length][0][3] + " " + Alist[simdata.length][1][3]
                + " " + Alist[simdata.length][2][3]);
    removeExtraSteps();
}



function randn() {

  let v1, v2, fac, rsq;

  if (iset == 0) {
    do {
      v1 = 2.0*Math.random() - 1.0;
      v2 = 2.0*Math.random() - 1.0;
      rsq = v1*v1+v2*v2;
    } while ((rsq >= 1.0) || (rsq == 0));
    fac = Math.sqrt(-2.0*Math.log(rsq)/rsq);
    gset = v1*fac;
    iset = 1;
    return v2*fac;
  } else {
    iset = 0;
    return gset;
  }

}


function launchSim(rna) {
    saveSelectedStep();
    addExtraSteps(rna);
    let scalingfactor = parseFloat($("#sfactor").val());
    let bias = parseFloat($("#biasenergy").val());
    seq = $("#sequence").val().toUpperCase();
    let binfunction = $("#codeeval").val();
    if (seq.length < 2 || !(scalingfactor > 0) || !(bias >= 0)) {
        alert("You did not enter a sequence, or your parameters are invalid");
        return;
    }
    let stepindex = [];

    for (let i = 0; i < seq.length-1; i++) {
        let stp = seq.charAt(i) + seq.charAt(i+1);
        //console.log(stp);
        for (let j = 0; j < steps.length; j++) {
            if (stp == steps[j]) stepindex.push(j);
        }
    }
    if (workerThread != null) {
        workerThread.terminate();
    }
    workerThread = new Worker('js/wlmc.js');
    workerThread.postMessage({ "args": [forceconstants, stepparameters, seq, stepindex, bias, scalingfactor, binfunction]});
    workerThread.addEventListener("message", function(e) {
        for (let i = 0; i < simchartlist.length; i++) {
            simchartlist[i].destroy();
        }
        simchartlist = [];
        simresult = e.data.args[0];
        buildStructure(seq, e.data.args[1]);
        if (bias == 0) simplot(simresult, "MyChart10", "Radial Histogram (number of configurations)");
        else {
          let pmf = numeric.clone(simresult);
          for (let i = 0; i < simresult.length; i++) pmf[i][1] *= -1.0;
          simplot(pmf, "MyChart10", "Radial PMF (kT energy units)", 'usePMF');
        }
    });
    $("#launchSimButton").hide();
    $("#stopbutton").show();
    $()
    removeExtraSteps();
}

function stopSim() {
    if (workerThread != null) workerThread.postMessage({"cmd": 'stop'});
    $("#stopbutton").hide();
    workerThread.terminate();
    workerThread = null;
    $("#launchSimButton").show();
}