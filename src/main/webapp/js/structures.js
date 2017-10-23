
let simmodel, simmodel2, viewer, viewer2;

let nucleic = ['A', 'T', 'G', 'C', 'U', 'DA', 'DT', 'DG', 'DC']
let nucleicmodel = {cartoon: {}};
let boundprotein = {cartoon: {color: 'magenta'}};
let extraprotein = {cartoon: {color: 'red', thickness: '0.1'}};
let extranucleic = {stick: {colorscheme: 'nucleic'}};
let currentSteps = "";


function getSteps() {
    let job = $("#myjobs2").val();
    let str = $("#snumber").val();
    let bound = $("#sbound").val();
    $.ajax({method: "GET",
            headers: {'Content-Type': 'application/json'},
            url: "/steps/"+job+"/"+str+"/"+bound
            }).done(function(result) {
                if (result.length < 5) {
                    alert("No data found");
                    return;
                }
                data = JSON.parse(result);
                let demoseq = "A";
                for (let i = 0; i < data.length; i++) {
                    demoseq = demoseq + "A";
                }
                currentSteps = data;
                buildStructure(demoseq, calculateAlist(data), "yes");
                $.ajax({method: "GET",
                        headers: {'Content-Type': 'application/json'},
                        url: "/proteins/"+job+"/"+str+"/"+bound
                       }).done(function(data) {
                        if (data.length > 20) draw2append(data, "yes");
                      })
            });
}

function getStructures() {

    let job = $("#myjobs2").val();
    if ($("#extvis").is(':checked')) {
        job = $("#extcode2").val();
    }

    $.ajax({method:"GET", url:"/liststructures/"+job}).done(function (result) {
       console.log(result);
       if (result == null) {
         alert("No structures available");
       }
       $("#structuredata").html("<pre>"+result+"</pre>");
    }).error(function() {
        alert("Something went wrong");
    });

}


function drawStructure() {

    let job = $("#myjobs2").val();
    let str = $("#snumber").val();
    let bound = $("#sbound").val();


    function draw(result) {
             if (result != null && result.length > 20) {

                   $('#glmolbox_src').val(result);
                   if (viewer === undefined) {
                       let element = $('#glmolbox');
                       let config = { backgroundColor: 'white' };
                       viewer = $3Dmol.createViewer( element, config );
                   } else {
                       viewer.removeAllModels();
                   }
                   let simmodel = viewer.addModel(result, "pdb");
                   //let frames = simmodel.getFrames();
                   //console.log(frames[0][0]);
                   viewer.setStyle({model: simmodel}, boundprotein);
                   viewer.setStyle({model: simmodel, resn: nucleic}, nucleicmodel);
                   let extramodel = [];
                   let c = 0;
                   $("#extraImages option").each(function () {
                        if ($(this).is(':selected')) {
                          $.ajax({method: "GET", url: $(this).val(), async:false}).done(function(data) {
                            extramodel.push(viewer.addModel(data, "pdb"));
                            viewer.setStyle({model: extramodel[c]}, extraprotein);
                            viewer.setStyle({model: extramodel[c], resn: ['A','T','G','C','U']}, extranucleic);
                            c++;
                          })
                        }
                    });

                   viewer.zoomTo();
                   viewer.render();
//                   viewer.zoom(1.2, 1000);
             }
    };

    if ($("#extvis").is(':checked')) {
        let token = $("#exttoken2").val();
        let code = $("#extcode2").val();
        $.ajax({method:"GET", url:"/structureshare/"+token+"/"+code+"/"+str+"/"+bound}).done(draw);
    } else $.ajax({method:"GET", url:"/structure/"+job+"/"+str+"/"+bound}).done(draw);

}



function draw2(pdbtext, winchoice) {
     let nucleicmodel = {cartoon: {}};

     if (winchoice === undefined) {

         if (viewer2 === undefined) {
           let element = $('#glmolbox2');
           let config = { backgroundColor: 'white' };
           viewer2 = $3Dmol.createViewer( element, config );
           simmodel2 = viewer2.addModel(pdbtext, "pdb");
           viewer2.setStyle({}, nucleicmodel);
           viewer2.zoomTo();                                      /* set camera */
           viewer2.render();                                      /* render scene */
         }
         else {
           viewer2.removeAllModels();
           simmodel2 = viewer2.addModel(pdbtext, "pdb");
           viewer2.setStyle({}, nucleicmodel);
           viewer2.zoomTo();
           viewer2.render();
         }
     } else {
         $('#glmolbox_src').val(pdbtext);
         if (viewer === undefined) {
           let element = $('#glmolbox');
           let config = { backgroundColor: 'white' };
           viewer = $3Dmol.createViewer( element, config );
           simmodel = viewer.addModel(pdbtext, "pdb");
           viewer.setStyle({}, nucleicmodel);
           viewer.zoomTo();                                      /* set camera */
           viewer.render();                                      /* render scene */
         }
         else {
           viewer.removeAllModels();
           simmodel = viewer.addModel(pdbtext, "pdb");
           viewer.setStyle({}, nucleicmodel);
           viewer.zoomTo();
           viewer.render();
         }
     }
}

function draw2append(pdbtext, winchoice) {
    if (winchoice === undefined) {
        if (viewer2 === undefined) return;
        simmodelappend = viewer2.addModel(pdbtext, "pdb");
        viewer2.setStyle({model: simmodelappend}, {cartoon: {color: 'magenta'}});
        viewer2.zoomTo();
        viewer2.render();
    } else {
       $('#glmolbox_src').val($('#glmolbox_src').val()+pdbtext);
       if (viewer === undefined) {
           let element = $('#glmolbox');
           let config = { backgroundColor: 'white' };
           viewer = $3Dmol.createViewer( element, config );
       }
       simmodelappend = viewer.addModel(pdbtext, "pdb");
          let extramodel = [];
          let c = 0;
          $("#extraImages option").each(function () {
               if ($(this).is(':selected')) {
                 $.ajax({method: "GET", url: $(this).val(), async:false}).done(function(data) {
                   $('#glmolbox_src').val($('#glmolbox_src').val()+data);
                   extramodel.push(viewer.addModel(data, "pdb"));
                   viewer.setStyle({model: extramodel[c]}, extraprotein);
                   viewer.setStyle({model: extramodel[c], resn: ['A','T','G','C','U']}, extranucleic);
                   c++;
                 })
               }
           });
       viewer.setStyle({model: simmodelappend}, boundprotein);
       viewer.zoomTo();
       viewer.render();
    }
}


function saveStructure() {
        let filename="structure.pdb";
        let blob = new Blob([$('#glmolbox_src').val()], {type:"text/plain;charset=utf-16"});
        saveAs(blob, filename);
}

function saveSteps() {
        let filename="steps.dat";
        let blob = new Blob([JSON.stringify(currentSteps)], {type:"text/plain;charset=utf-16"});
        saveAs(blob, filename);
}