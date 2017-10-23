
let costchartlist = [];
let costChart;
let costslist = [];
let costmap = [];


function processProtein(data) {
    //console.log(data);
    let length = data.stepLength;
    let lines = data.dats.split(/\r?\n/);
    let prosteps = [];
    for (let i = 0; i < length; i++) {
        let result = [];
        let steps = lines[i+1].split(" ");
        for (let j = 0; j < 6; j++) result.push(parseFloat(steps[j]));
        prosteps.push(result);
    }
    let sequence = $("#sequence").val().toUpperCase();
    saveSelectedStep();
    addExtraSteps();

    let stepdata = [];
    let stepindex = [];
    for (let i = 0; i < sequence.length-1; i++) {
        let stp = sequence.charAt(i) + sequence.charAt(i+1);
        stepdata.push([0,0,0,0,0,0]);
        //console.log(stp);
        for (let j = 0; j < steps.length; j++) {
            if (stp == steps[j]) stepindex.push(j);
        }
    }
    //console.log(JSON.stringify(prosteps));

    let costs = [];
    let minenergy = 1e8;
    let minindex = 0;
    for (let i = 0; i < stepindex.length - length; i++) {
        for (let j = 0; j < length; j++) {
            stepdata[i+j] = prosteps[j];
        }
        let currentenergy = calculateEnergy(stepindex, stepdata, i, i+length);
        costs.push(currentenergy);
        if (costslist[i] === undefined) costslist.push(currentenergy);
        else if (currentenergy < costslist[i]) costslist[i] = currentenergy;
        if (currentenergy < minenergy) {
            minenergy = currentenergy;
            minindex = i;
        }
    }

    let bestseq = "";
    for (let i = minindex; i < minindex+length+1; i++) {
        bestseq += sequence.charAt(i);
    }

    console.log("Forward strand orientation:");
    console.log(minindex);
    console.log(minenergy);
    console.log(bestseq);

    minenergy = 1e8;
    minindex = 0;
    for (let i = 0; i < stepindex.length - length; i++) {
        for (let j = 0; j < length; j++) {
            stepdata[i+j] = numeric.clone(prosteps[length-j-1]);
            stepdata[i+j][0] = -stepdata[i+j][0];
            stepdata[i+j][3] = -stepdata[i+j][3];
        }
        let currentenergy = calculateEnergy(stepindex, stepdata, i, i+length);
        if (currentenergy < costslist[i]) costslist[i] = currentenergy;
        if (currentenergy < costs[i]) costs[i] = currentenergy;
        if (currentenergy < minenergy) {
            minenergy = currentenergy;
            minindex = i;
        }
    }
    bestseq = "";
    for (let i = minindex; i < minindex+length+1; i++) {
        bestseq += sequence.charAt(i);
    }
    console.log("Reverse strand orientation:");
    console.log(minindex);
    console.log(minenergy);
    console.log(bestseq);
    costmap.push(costs);

    removeExtraSteps();

}


function calculateEnergy(stepindex, stepdata, startindex, endindex) {
    let energy = 0.0;
    for (let i = startindex; i < endindex; i++) {
        let deltax = numeric.sub(stepdata[i], stepparameters[stepindex[i]]);
        let product1 = numeric.dot(deltax, forceconstants[stepindex[i]]);
        energy += numeric.dot(product1, deltax);
    }
    return 0.5*energy;
}


function scoreProtein() {
    let pid = $("#proteins2").val();
    for (let i = 0; i < costchartlist.length; i++) {
      costchartlist[i].destroy();
    }
    costchartlist = [];
    $.ajax({method: "GET",
           headers: {'Content-Type': 'application/json'},
           url: "/getprotein/"+pid
           }).done(processProtein);
}

function scoreProteins() {

    for (let i = 0; i < costchartlist.length; i++) {
      costchartlist[i].destroy();
    }
    costslist = [];
    costchartlist = [];
    costmap = [];
    $("#proteins2 option").each(function () {
        if ($(this).is(':selected')) {
                $.ajax({method: "GET",
                       headers: {'Content-Type': 'application/json'},
                       url: "/getprotein/"+$(this).val(), async:false
                       }).done(processProtein);
        }
    })
    plotCost(costslist, 'CostChart', "Cost of protein binding (kT energy)");
    plotMap();


}




let costLayout = {

  titlefont: {
        family: 'Arial',
        size:32
  },
  xaxis: {
    tickfont: {
          family: 'Arial',
          size: 20,
          color: 'black'
        }
  },
  yaxis: {
          tickfont: {
                family: 'Arial',
                size: 20,
                color: 'black'
              }
        },
  legend: {
               tickfont: {
                      family: 'Arial',
                      size: 20,
                      color: 'black'
                    }
        },
   margin: {
        b: 150,
        l: 150
   }
};

function plotCost(vec, chartname, title) {
  let ctx = document.getElementById(chartname).getContext("2d");
  let data = [];
  //console.log(vec[0][0]);


  for (let i = 0; i < vec.length; i++) {
    let point = {x: (i+1), y: vec[i]};
    data.push(point);
  }

  costChart = new Chart(ctx, {
    type: 'scatter',
    data: {
        datasets: [{
          //backgroundColor: 'rgba(0, 0, 255, 0.4)',
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
                   ticks: {
                      fontSize: fontsize
                   },
                   type: 'linear',
                   position: 'bottom',
                   scaleFontSize: 32
          }],
          yAxes: [{
                    ticks: {
                        // min: 0, max: 1200
                        fontSize: fontsize
                    },
                    type: 'linear',
                    position: 'left',
                    scaleFontSize: 32
                   }]
        }
    }
  });
  costchartlist.push(costChart);

}


function plotMap() {
    let ph = numeric.clone(costmap);
    for (let i = 0; i < ph.length; i++) {
        ph[i].splice(0,1);
    }

    for (let i = 0; i < ph.length; i++) {
        let max = 0.0;
        let min = 1e6;
        for (let j = 0; j < ph[i].length; j++) {
            if (ph[i][j] > max) max = ph[i][j];
            if (ph[i][j] < min) min = ph[i][j];
        }
        for (let j = 0; j < ph[i].length; j++) {
            ph[i][j] = (max-ph[i][j])/(max-min);
        }

    }
    let y = ["structure 1"];
    for (let i = 1; i < ph.length; i++) y.push("structure "+(i+1));
    let x = [];
    for (let i = 0; i < ph[0].length; i++) {
        x.push((i+1) + "");
    }

    let data = [{
        z: ph,
        x: x,
        y: y,
        type: 'heatmap',
    }];
    layout.title = "Optimal protein binding positions per crystal structure";

    Plotly.newPlot('scoringdiv', data, layout);
}