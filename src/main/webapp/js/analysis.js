
let chartlist = [];
var fontsize = 32;
var chartwidth = 700;
var chartheight = 700;

let layout = {

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


function publicationMode() {
    $('main').width(4000)
    $('.mol-container').width(3000);
    $('.mol-container').height(3000);
    chartwidth = 3000;
    chartheight = 3000;
    fontsize = 80;
    $(window).trigger('resize');
}

function viewMode() {
    $('main').width(800);
    fontsize = 12;
    chartwidth = 600;
    chartheight = 600;
    $('.mol-container').width(600);
    $('.mol-container').height(600);
    $(window).trigger('resize');
}


function plotPositions(inp) {
    let ph = numeric.clone(inp);
    for (let i = 0; i < ph.length; i++) {
        ph[i].splice(0,1);
    }
    for (let i = 0; i < ph[0].length; i++) {
        let sum = 0.0;
        for (let j = 0; j < ph.length; j++) {
            sum += ph[j][i];
        }
        for (let j = 0; j < ph.length; j++) {
            ph[j][i] /= sum;
        }

    }
    let y = ["1 protein"];
    for (let i = 1; i < ph[0].length; i++) y.push((i+1) + " proteins");
    let x = [];
    for (let i = 0; i < ph.length; i++) {
        x.push(i+1 + " bp");
    }
    let data = [{
        z: numeric.transpose(ph),
        x: x,
        y: y,
        type: 'heatmap'
    }];
    layout.title = "Position of bound proteins on the DNA chain (fraction of results)";
    Plotly.newPlot('mypositiondiv', data, layout);
}


function plotDistances(inp) {
    let ph = numeric.clone(inp);
    for (let i = 0; i < ph.length; i++) {
        ph[i].splice(0,1);
    }

    for (let i = 0; i < ph[0].length; i++) {
        let sum = 0.0;
        for (let j = 0; j < ph.length; j++) {
            sum += ph[j][i];
        }
        for (let j = 0; j < ph.length; j++) {
            ph[j][i] /= sum;
        }

    }
    let y = ["2 proteins"];
    for (let i = 1; i < ph[0].length; i++) y.push((i+2) + " proteins");
    let x = [];
    for (let i = 0; i < ph.length; i++) {
        x.push(i + " bp");
    }

    let data = [{
        z: numeric.transpose(ph),
        x: x,
        y: y,
        type: 'heatmap',
    }];
    layout.title = "Distances between bound proteins on the DNA chain (fraction of results)";

    Plotly.newPlot('mydistancediv', data, layout);
}


function plot(vec, chartname, title) {
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

  let scatterChart = new Chart(ctx, {
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
                   ticks: {
                      fontSize: fontsize
                   },
                   type: 'linear',
                   position: 'bottom',
                   scaleFontSize: 32
          }],
          yAxes: [{
                    ticks: {
                        min:0,
                        fontSize: fontsize
                    },
                    type: 'linear',
                    position: 'left',
                    scaleFontSize: 32
                   }]
        }
    }
  });
  chartlist.push(scatterChart);

}


function plotBar(vec, chartname, title) {
  let ctx = document.getElementById(chartname).getContext("2d");
  let data = {};
  let colors = ['rgba(255, 0, 0, 0.5)', 'rgba(0, 255, 0, 0.5)', 'rgba(0, 0, 255, 0.5)'];
  data.labels = [];
  data.datasets = [{}];
  data.datasets[0].data = [];
  data.datasets[0].backgroundColor = [];
  data.datasets[0].label = title;

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
    data.labels.push(""+vec[i][0]);
    data.datasets[0].data.push(vec[i][1]);
    data.datasets[0].backgroundColor.push(colors[i % 3]);
  }

  let barChart = new Chart(ctx, {
      type: 'bar',
      data: data,
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
                            }
                        }],
                 yAxes: [{
                            ticks: {
                               min: 0,
                               fontSize: fontsize
                          }
                          }]
              }
      }
    });

    chartlist.push(barChart);

}




function analyze() {
    let jobInfo = [];
    let results = [];
    let total = 0;
    let boundid = $("#boundid").val();
    for (let i = 0; i < chartlist.length; i++) {
      chartlist[i].destroy();
    }
    //chartlist = [];
    $("#myjobs option").each(function () {
        total++;
        //console.log($(this).val());
        if ($(this).is(':selected')) {
            $.ajax({method: "GET", url: "/myjobs/"+$(this).val()}).done(function(info){
                jobInfo.push(info);
                console.log(info);
                if ($("#showparams").is(":checked")) {
                    $("#sparameter").html("Code: " + info.id + "<br/>token: " + info.token +
                        "<br/>Sequence " + info.sequence +
                        "<br/>" + (info.sequence.length-1) + " bp steps" +
                        "<br/>N half chains " + info.nChains +
                        "<br/>Radial bound " + info.rBounds +
                        "<br/>cos(gamma) bound (greater than) " + info.gBounds +
                        "<br/>|Twist bound| < " + info.twBounds +
                        "<br/>Random number seed " + info.seed +
                        "<br/>Has proteins " + info.hasProteins +
                        "<br/>Binding probability " + info.bindingProbability +
                        "<br/>Boundary conditions (if not circular) " + info.bounds +
                        "<br/>Protein ID " + info.proteinId
                     );
                }
            });
                $.ajax({method: "GET", url: "/analyze/"+$(this).val()}).done(function(response){
                if (response != null) {
                    $("#jfactors").html("<h4>Jfactors [M] by bound = " + response.Jfactor + "</h4>");
                    results.push(response);
                    console.log(response);
                    let twistHistogram = JSON.parse(response.twistHistogram);
                    let writheHistogram = JSON.parse(response.writheHistogram);
                    let linkHistogram = JSON.parse(response.linkHistogram);
                    let rgHistogram = JSON.parse(response.rgHistogram);
                    let numberHistogram = JSON.parse(response.numberHistogram);
                    let positionHistogram = JSON.parse(response.positionHistogram);
                    let distanceHistogram = JSON.parse(response.distanceHistogram);
                    if (twistHistogram != null) plot(twistHistogram[boundid], "MyChart0", "Twist Histogram");
                    if (writheHistogram != null) plot(writheHistogram[boundid], "MyChart1", "Writhe Histogram");
                    if (linkHistogram != null) plotBar(linkHistogram[boundid], "MyChart2", "Normalized Linking Number Histogram");
                    if (numberHistogram != null) plotBar(numberHistogram[boundid], "MyChart3", "Number of bound proteins data");
                    if (rgHistogram != null) plot(rgHistogram[boundid], "MyChart4", "Radius of Gyration (Å) Histogram");
                    if (positionHistogram != null) {
                        //console.log(JSON.stringify(positionHistogram[boundid]));
                        plotPositions(positionHistogram[boundid]);
                    }
                    if (distanceHistogram != null) {
                        plotDistances(distanceHistogram[boundid]);
                    }
                } else {
                    alert("Job is not yet finished");
                }
                }).error(function() {
                    alert("This job is not yet finished or returned no result");
                });
        }
    })

}


function analyzeExternal() {
    let token = $("#exttoken").val();
    let code = $("#extcode").val();
    let boundid = $("#boundid").val();
    for (let i = 0; i < chartlist.length; i++) {
        chartlist[i].destroy();
    }
    //chartlist = [];
    $.ajax({method: "GET", url: "/analyzeshare/"+token+"/"+code}).done(function(response) {
        if (response != null) {
                    $("#jfactors").html("<h4>Jfactors [M] by bound = " + response.Jfactor + "</h4>");
                    console.log(response);
                    let twistHistogram = JSON.parse(response.twistHistogram);
                    let writheHistogram = JSON.parse(response.writheHistogram);
                    let linkHistogram = JSON.parse(response.linkHistogram);
                    let rgHistogram = JSON.parse(response.rgHistogram);
                    let numberHistogram = JSON.parse(response.numberHistogram);
                    let positionHistogram = JSON.parse(response.positionHistogram);
                    let distanceHistogram = JSON.parse(response.distanceHistogram);
                    if (twistHistogram != null) plot(twistHistogram[boundid], "MyChart0", "Twist Histogram");
                    if (writheHistogram != null) plot(writheHistogram[boundid], "MyChart1", "Writhe Histogram");
                    if (linkHistogram != null) plotBar(linkHistogram[boundid], "MyChart2", "Normalized Linking Number Histogram");
                    if (numberHistogram != null) plotBar(numberHistogram[boundid], "MyChart3", "Number of bound proteins data");
                    if (rgHistogram != null) plot(rgHistogram[boundid], "MyChart4", "Radius of Gyration (Å) Histogram");
                    if (positionHistogram != null) {
                        plotPositions(positionHistogram[boundid]);
                    }
                    if (distanceHistogram != null) {
                        plotDistances(distanceHistogram[boundid]);
                    }
        } else {
            alert("Job is not yet finished");
        }
    });
}


function downloadJob() {
    $("#myjobs option").each(function () {
        if ($(this).is(':selected')) {
            let jobId = $(this).val();
            let url = '/jobs/'+jobId+'.tar.gz';
            window.location.href = url;
        }
    });
}
