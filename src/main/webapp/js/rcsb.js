
function retrievePDB(pdbname) {
  $.ajax({method: "GET", url: "https://files.rcsb.org/download/" + pdbname + ".pdb"}).done(function(data) {
     $("#pdbref").val(data);
  });
}

function test() {
    console.log($("#dateBefore").val());
    console.log($("#dateAfter").val());
}

function getPDB(pdbname) {
  let result = "";
  $.ajax({method: "GET", url: "https://files.rcsb.org/download/" + pdbname + ".pdb", async: false}).done(function(data) {
     result = data;
  });
  return result;
}


function submitFF() {
    let request = { 'orgPdbQuery': {
                         'queryType': 'org.pdb.query.simple.ChainTypeQuery',
                         'description': 'Luke Czapla JSON to XML example',
                         'containsProtein': 'N',
                         'containsDna': 'N',
                         'containsRna': 'N',
                         'containsHybrid': 'N'
                     }};
     let opt = $("#structureType").val();
     console.log(opt);
     switch (parseInt(opt)) {
        case 1: request['orgPdbQuery']['containsProtein'] = 'Y';
                request['orgPdbQuery']['containsDna'] = 'Y'; break;
        case 2: request['orgPdbQuery']['containsProtein'] = 'N';
                request['orgPdbQuery']['containsDna'] = 'Y'; break;
        case 3: request['orgPdbQuery']['containsProtein'] = 'Y';
                request['orgPdbQuery']['containsRna'] = 'Y'; break;
        case 4: request['orgPdbQuery']['containsProtein'] = 'N';
                request['orgPdbQuery']['containsRna'] = 'Y'; break;
        default: request['orgPdbQuery']['containsDna'] = 'Y'; break;
     }
     let x2js = new X2JS();
     let xmlString = x2js.json2xml_str(request);
     console.log(xmlString);
     //let request = "<orgPdbQuery><queryType>org.pdb.query.simple.ChainTypeQuery</queryType><description>Luke Czapla for Wilma Olson lab</description><containsProtein>Y</containsProtein><containsDna>Y</containsDna><containsRna>N</containsRna></orgPdbQuery>";
     $.ajax({url: 'http://www.rcsb.org/pdb/rest/search/?sortfield=Resolution', method: 'POST', data: xmlString})
     .done(function(result) {
 //        console.log(result);

             let pdbdata = {pdbs: "", pdbList: result, RNA: false, cullEigen: $("#cullEigen").is(":checked"),
                cullStandard: $("#cullStandard").is(":checked")};
             console.log(pdbdata);

             if (parseInt(opt) == 3 || parseInt(opt) == 4) pdbdata.RNA = true;
             if ($("#specificPdbList").is(':checked')) pdbdata.pdbList = $("#pdbIdList").val();
             pdbdata.beforeDate = $("#dateBefore").val();
             pdbdata.afterDate = $("#dateAfter").val();
             pdbdata.description = $("#ffdescription").val();
             console.log(pdbdata.beforeDate + " " + pdbdata.afterDate);
             pdbdata.nonredundant = $("#removeRedundant").is(":checked");
             let req = {headers: {
                                 'Content-Type': 'application/json'
                             },
                             url: "/analyzePDBs",
                             method: "POST",
                             data: JSON.stringify(pdbdata)
                        };
             $.ajax(req).done(function(data) {
                if (data.length > 1) alert("Your model is being made and code is " + data);
                else alert("A problem occurred, check parameters and make sure you are logged in");
                refreshData();
             });

             //break;

     });

}


function downloadFF() {
    let jobId = $("#myffs").val();
    let url = '/forcefield/'+jobId+'.zip';
    window.location.href = url;
}


function loadForceField() {
    $.ajax({url: "/myforcefield/"+$("#myffs").val(),
             headers: {
                 'Content-Type': 'application/json'
             },
             method: "GET"
    }).done(function(result) {
        console.log(result);
        loadForceField2();
        loadTetranucleotideData(result.steps, result.contexts);
        $("#FFload").html("Force field is loaded");
    });
}

function loadForceField2() {
    $.ajax({url: "/myforcefield2/"+$("#myffs").val(),
             headers: {
                 'Content-Type': 'application/json'
             },
             method: "GET",
             async: false
    }).done(function(result) {
        console.log(result);
        loadDinucleotideData(result.steps, result.contexts);
        $("#FFload").html("Force field is loaded");
    });
}

