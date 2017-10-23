importScripts("numeric-1.2.6.min.js");
importScripts("references.js");

var forceconstants;
var stepparameters;
var seq, stepindex, simdata;
var running = false;
let binfunction = "var binlength = parseInt(seq.length*3.4)+1; function calculateBin() { return parseInt(calculateR(A)); }";
let steppeddown = false;
var bins;

let scalingfactor = 1.0;
let simresult;
let nruns = 10000;
let nsteps;

let penalty = 0.001;

let iset = 0;
let gset;

let first = true;

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


function gaussiansample() {
    let ns = forceconstants.length;
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
    for (let i = 0; i < stepindex.length; i++) {
        let s = stepindex[i];
        for (let j = 0; j < 6; j++) {
            simdata[i][j] = randn()*factor[s][j];
        }
        simdata[i] = numeric.add(numeric.dot(eigen[s].eigenvectors, simdata[i]), stepparameters[s]);
    }
    console.log("Energy (kT) = " + calculateEnergy());
}

/*
    var binlength = 110;
    function calculateBin() {

       if (R > 10.0) return 50 + parseInt(Math.sqrt(A[0][3]*A[0][3]+A[1][3]*A[1][3]+A[2][3]*A[2][3])/5.0)-1;
       else if (A[2][2] < 0.95) {
          return 11 + parseInt(20*(1 - A[2][2]))
       } else {
          return parseInt(Math.abs(tp[2])/15.0);
       }

    }

    function reportJ() {
        let pT = 0.0;
        for (let i = 0; i < simresult.length; i++) {
           if (simresult[i][1] != 0.0) pT += Math.exp(simresult[i][1]);
        }
        let p = Math.exp(simresult[0][1])/pT;
        let Jfactor = 3.0 * p / (2.0*6.022e23 * 0.05 * Math.pow(10.0e-9, 3.0) * Math.PI/12.0)
        return Jfactor;
    }

*/

function calculateAlist(simd) {
    let A = new Array(simd+1);
    A[0] = [[1.0,0,0,0],[0,1.0,0,0], [0,0,1.0,0], [0,0,0,1.0]];
    for (let i = 0; i < simd.length; i++) {
        A[i+1] = numeric.dot(A[i], calculateA(simd[i]));
    }
    return A;
}


function calculateR(A) {
    return Math.sqrt(A[0][3]*A[0][3]+A[1][3]*A[1][3]+A[2][3]*A[2][3]);
}


function calculateEnd() {
    let result = [[1.0,0,0,0],[0,1.0,0,0],[0,0,1.0,0],[0,0,0,1.0]];
    for (let i = 0; i < simdata.length; i++) {
        result = numeric.dot(result, calculateA(simdata[i]));
    }
    //console.log(result);
    return result;
}

function calculateEnergy(debug) {
    let energy = 0;
    for (let i = 0; i < simdata.length; i++) {
        let deltax = numeric.sub(simdata[i], stepparameters[stepindex[i]]);
        let product1 = numeric.dot(deltax, forceconstants[stepindex[i]]);
        energy += numeric.dot(product1, deltax);
    }
    return 0.5*scalingfactor*energy;
}

function simulate() {
  console.log(binfunction);
  averageA = [[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]];

  let oldenergy = calculateEnergy();
  let Aold = calculateEnd();
  let A = numeric.clone(Aold);
  let tp = calculatetp(Aold);
  let R = calculateR(Aold);

  eval(binfunction);

  bins = new Array(binlength);
  for (let i = 0; i < binlength; i++) bins[i] = 0.0;
  simresult = new Array(binlength);
  for (let i = 0; i < binlength; i++) simresult[i] = [0.0, 0.0];

  let oldbin = calculateBin();
  console.log(oldbin);
  let Rold = R;
  let newenergy;
  let naccept = 0;
  for (let ir = 0; ir < 100000; ir++) {
  for (let i = 0; i < nruns; i++) {
    let olddata = numeric.clone(simdata);
    //console.log(oldstep);
    for (let k = 0; k < simdata.length; k++)
      for (let j = 0; j < 6; j++)
        if (j < 3) simdata[k][j] += (0.5*(Math.random()-0.5) / (Math.sqrt(nsteps)*forceconstants[stepindex[k]][j][j]));
        else simdata[k][j] += (0.05*(Math.random()-0.5) / (Math.sqrt(nsteps)*forceconstants[stepindex[k]][j][j]));
    A = calculateEnd();
    tp = calculatetp(A);
    R = calculateR(A);
    let newbin = calculateBin();
    newenergy = calculateEnergy()+penalty*bins[newbin];
    if (newenergy < oldenergy) {    // accept
        //console.log("dE = " + (newenergy-oldenergy));
        oldenergy = newenergy;
        Rold = R;
        Aold = A;
        oldbin = newbin;
        bins[newbin]++;
        naccept++;
    } else {
      let deltaE = newenergy - oldenergy;
      //console.log("dE = ", deltaE);
      if (Math.random() < Math.exp(-deltaE)) {   // accept
        oldenergy = newenergy;
        Rold = R;
        Aold = A;
        oldbin = newbin;
        bins[newbin]++;
        naccept++;
      } else {  // reject
        oldenergy += penalty;
        simdata = olddata;
        bins[oldbin]++;
      }
    }
    // for calculating persistence length
    averageA = numeric.add(averageA,Aold);
  }

    let Alist = calculateAlist(simdata);
    let Avg = numeric.div(averageA, (nruns*(ir+1)));
    for (let i = 0; i < 500; i++) {
        Avg = numeric.dot(Avg, Avg);
    }

    for (let i = 0; i < simresult.length; i++) {
      simresult[i][0] = i+0.5;
      if (penalty > 0) simresult[i][1] = bins[i]*penalty;
      else simresult[i][1] = bins[i];
    }
    // go down to a smaller penalty cost
    if (bins[0] >= 1000 && !steppeddown) {
        steppeddown = true;
        for (let i = 0; i < binlength; i++) bins[i] *= 10;
        penalty /= 10.0;
    }
    self.postMessage({args: [simresult, Alist]});
    console.log("persistence length (Å) = " + Avg[2][3]);
    console.log("acceptance rate = " + (naccept/(nruns*(ir+1))));
  }
}



self.addEventListener("message", function(e) {

  if (!running) {
    //console.log(e);
    let args = e.data.args;
    forceconstants = args[0];
    stepparameters = args[1];
    seq = args[2];
    stepindex = args[3];
    penalty = args[4];
    scalingfactor = args[5];
    if (args[6].length > 5) binfunction = args[6];
    simdata = new Array(stepindex.length);
    nsteps = stepindex.length;
    for (let i = 0; i < nsteps; i++) {
      let vals = [];
      for (let j = 0; j < 6; j++) {
         vals.push(0.0);
         // vals.push(stepparameters[stepindex[i]][j] + (j < 3 ? (0.5*(Math.random()-0.5) / forceconstants[stepindex[i]][j][j]) : 0.0));
      }
      simdata[i] = vals;
    }
    gaussiansample();
    running = true;
    simulate();
  } else {
    if (e.data.cmd == 'stop') {
        self.close();
    }
  }

}, false);

