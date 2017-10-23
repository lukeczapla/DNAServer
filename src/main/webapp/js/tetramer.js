
let tetramers = "AAAA AAAC AAAG AAAT CAAA CAAC CAAG CAAT GAAA GAAC GAAG GAAT TAAA TAAC TAAG TAAT AACA AACC AACG AACT CACA CACC CACG CACT GACA GACC GACG GACT TACA TACC TACG TACT AAGA AAGC AAGG AAGT CAGA CAGC CAGG CAGT GAGA GAGC GAGG GAGT TAGA TAGC TAGG TAGT AATA AATC AATG AATT CATA CATG GATA GATC GATG TATA ACAA ACAC ACAG ACAT CCAA CCAC CCAG CCAT GCAA GCAC GCAG GCAT TCAA TCAC TCAG TCAT ACGA ACGC ACGG ACGT CCGA CCGG GCGA GCGC GCGG TCGA AGAA AGAC AGAG AGAT CGAA CGAC CGAG CGAT GGAA GGAC GGAG GGAT TGAA TGAC TGAG TGAT AGCA AGCC AGCG AGCT CGCA CGCG GGCA GGCC GGCG TGCA AGGA AGGC AGGG AGGT CGGA CGGC CGGG CGGT GGGA GGGC GGGG GGGT TGGA TGGC TGGG TGGT ATAA ATAC ATAG ATAT CTAA CTAG GTAA GTAC GTAG TTAA";
var tetramerSteps = tetramers.split(" ");
let stepnames = "tilt roll twist shift slide rise";
let unitnames = "° ° ° Å Å Å"
let steplists = stepnames.split(" ");
let unitlists = unitnames.split(" ");
let ntsteps = 136;
let isForceFieldLoaded = false;

var tetramerstepparameters = new Array(136);
var tetramerforceconstants = new Array(136);

var tsteps, tcontexts, dsteps, dcontexts;

String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
}

function plotStepGraph() {
    if (!isForceFieldLoaded) {
        alert("No data is currently loaded");
        return;
    }
    let v1 = parseInt($("#plot1").val());
    let v2 = parseInt($("#plot2").val());
    let tm = parseInt($("#tetramerList").val());
    let xdata = []; let ydata = [];
    console.log(v1 + " " + v2 + "   " + tm + " " + tetramerSteps[tm]);
    let taverages = [0, 0, 0, 0, 0, 0];
    let count = 0;
    let correlation = [[0,0],[0,0]];
    for (let i = 0; i < tsteps.length; i++) {
        if (tcontexts[i] === tetramerSteps[tm]) {
           xdata.push(tsteps[i][v1]);
           ydata.push(tsteps[i][v2]);
           taverages = numeric.add(taverages, tsteps[i]);
           count++;
        }
    }

    taverages = numeric.mul(taverages, 1.0/count);

    for (let i = 0; i < count; i++) {
        correlation[0][0] += (xdata[i]-taverages[v1])*(xdata[i]-taverages[v1]);
        correlation[1][0] += (ydata[i]-taverages[v2])*(xdata[i]-taverages[v1]);
        correlation[0][1] += (ydata[i]-taverages[v2])*(xdata[i]-taverages[v1]);
        correlation[1][1] += (ydata[i]-taverages[v2])*(ydata[i]-taverages[v2]);
    }
    correlation = numeric.mul(correlation, 1.0/count);
    let force = numeric.inv(correlation);
    let modes = numeric.eig(force);
    let theta = Math.atan2(modes.E.x[1][0], modes.E.x[0][0]);
    console.log(modes.E.x[0][0] + " " + modes.E.x[1][0]);
    console.log(modes.E.x[0][1] + " " + modes.E.x[1][1]);
    let sizes = modes.lambda.x;
    let ellipseCenter = [taverages[v1], taverages[v2]];

    let xe = [], ye = [];
    for (let ang = 0.0; ang < 6.31; ang += 0.1) {
        let xp = 3.0*Math.cos(ang)/Math.sqrt(sizes[0]);
        let yp = 3.0*Math.sin(ang)/Math.sqrt(sizes[1]);
        let xf = ellipseCenter[0]+(xp*Math.cos(theta)-yp*Math.sin(theta));
        let yf = ellipseCenter[1]+(xp*Math.sin(theta)+yp*Math.cos(theta));
        xe.push(xf);
        ye.push(yf);
    }

    console.log(xe + " " + ye);

    let layout = {
        title: steplists[v2].capitalize() + ' vs. ' + steplists[v1].capitalize(),
        titlefont: {
          size: fontsize
        },
        showlegend: false,
        xaxis: {
            title: steplists[v1] + " (" + unitlists[v1] + ")",
            titlefont: { size: fontsize },
            gridwidth: fontsize/10,
            tickfont: {
                size: fontsize/2
            }
        },
        yaxis: {
            title: steplists[v2] + " (" + unitlists[v2] + ")",
            titlefont: { size: fontsize },
            gridwidth: fontsize/10,
            tickfont: {
                size: fontsize/2
            }
        },

        width: chartwidth,
        height: chartheight,

        margin: {
            l: fontsize*2.5,
            r: fontsize*2.5,
            t: fontsize*2.5,
            b: fontsize*2.5,
            pad: fontsize/2
        }

    };

    let scattersetup = {
        x: xdata,
        y: ydata,
        mode: 'markers',
        type: 'scatter',
        marker: {
            size: chartwidth/80
        },
    }

    let scatter2setup = {
        x: xe,
        y: ye,
        mode: 'lines',
        type: 'scatter',
        marker: {
            size: chartwidth/80
        },
    }

    Plotly.newPlot('scatterplotdiv', [scattersetup, scatter2setup], layout);
}


function plotStepGraph2() {
    if (!isForceFieldLoaded) {
        alert("No data is currently loaded");
        return;
    }
    let v1 = parseInt($("#plot1").val());
    let v2 = parseInt($("#plot2").val());
    let tm = parseInt($("#dimerList").val());
    let xdata = []; let ydata = [];
    console.log(v1 + " " + v2 + "   " + tm + " " + steps[tm]);
    let taverages = [0, 0, 0, 0, 0, 0];
    let count = 0;
    let correlation = [[0,0],[0,0]];
    for (let i = 0; i < dsteps.length; i++) {
        if (dcontexts[i] === steps[tm]) {
           xdata.push(dsteps[i][v1]);
           ydata.push(dsteps[i][v2]);
           taverages = numeric.add(taverages, dsteps[i]);
           count++;
        }
    }

    taverages = numeric.mul(taverages, 1.0/count);

    for (let i = 0; i < count; i++) {
        correlation[0][0] += (xdata[i]-taverages[v1])*(xdata[i]-taverages[v1]);
        correlation[1][0] += (ydata[i]-taverages[v2])*(xdata[i]-taverages[v1]);
        correlation[0][1] += (ydata[i]-taverages[v2])*(xdata[i]-taverages[v1]);
        correlation[1][1] += (ydata[i]-taverages[v2])*(ydata[i]-taverages[v2]);
    }
    correlation = numeric.mul(correlation, 1.0/count);
    let force = numeric.inv(correlation);
    let modes = numeric.eig(force);
    let theta = Math.atan2(modes.E.x[1][0], modes.E.x[0][0]);
    console.log(modes.E.x[0][0] + " " + modes.E.x[1][0]);
    console.log(modes.E.x[0][1] + " " + modes.E.x[1][1]);
    let sizes = modes.lambda.x;
    let ellipseCenter = [taverages[v1], taverages[v2]];

    let xe = [], ye = [];
    for (let ang = 0.0; ang < 6.31; ang += 0.1) {
        let xp = 3.0*Math.cos(ang)/Math.sqrt(sizes[0]);
        let yp = 3.0*Math.sin(ang)/Math.sqrt(sizes[1]);
        let xf = ellipseCenter[0]+(xp*Math.cos(theta)-yp*Math.sin(theta));
        let yf = ellipseCenter[1]+(xp*Math.sin(theta)+yp*Math.cos(theta));
        xe.push(xf);
        ye.push(yf);
    }

    console.log(xe + " " + ye);

    let layout = {
        title: steplists[v2].capitalize() + ' vs. ' + steplists[v1].capitalize(),
        titlefont: {
          size: fontsize
        },
        showlegend: false,
        xaxis: {
            title: steplists[v1] + " (" + unitlists[v1] + ")",
            titlefont: { size: fontsize },
            gridwidth: fontsize/10,
            tickfont: {
                size: fontsize/2
            }
        },
        yaxis: {
            title: steplists[v2] + " (" + unitlists[v2] + ")",
            titlefont: { size: fontsize },
            gridwidth: fontsize/10,
            tickfont: {
                size: fontsize/2
            }
        },

        width: chartwidth,
        height: chartheight,

        margin: {
            l: fontsize*2.5,
            r: fontsize*2.5,
            t: fontsize*2.5,
            b: fontsize*2.5,
            pad: fontsize/2
        }

    };

    let scattersetup = {
        x: xdata,
        y: ydata,
        mode: 'markers',
        type: 'scatter',
        marker: {
            size: chartwidth/80
        },
    }

    let scatter2setup = {
        x: xe,
        y: ye,
        mode: 'lines',
        type: 'scatter',
        marker: {
            size: chartwidth/80
        },
    }

    Plotly.newPlot('scatterplotdiv', [scattersetup, scatter2setup], layout);
}


function loadDinucleotideList() {
    console.log("load dinucleotide data");
    for (let i = 0; i < 10; i++) {
         $('#dimerList').append('<option value=' + i + '>' + steps[i] + '</option>');
    }
}


function loadTetranucleotideList() {
    console.log("load tetranucleotide data");
    for (let i = 0; i < ntsteps; i++) {
         $('#tetramerList').append('<option value=' + i + '>' + tetramerSteps[i] + '</option>');
    }
}


function loadTetranucleotideData(s, c) {
    tsteps = s;
    tcontexts = c;
    isForceFieldLoaded = true;
}



function loadDinucleotideData(s, c) {
    dsteps = s;
    dcontexts = c;
    isForceFieldLoaded = true;
}
