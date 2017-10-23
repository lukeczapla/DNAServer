
function renameProtein() {
    let i = -1;
    $("#proteins3 option").each(function() {
        if ($(this).is(':selected') && i == -1) {
            i = $(this).val();
        }
    });
    if (i == -1) return;
    $.ajax({headers: {'Content-Type': 'text/plain'}, url: "/renameprotein/"+i, method: "PUT", data: $("#newproteinname").val()}).done(function(data) {
        console.log(data);
        console.log(data.name);
        refreshData();
    });
}

function deleteProteins() {
    $("#proteins3 option").each(function () {
        if ($(this).is(':selected')) {
            let conBox = confirm("Are you sure you want to delete: " + $(this).html());
            if (conBox) {
                $.ajax({url: "/deleteprotein/" + $(this).val(),
                    method: "DELETE"});
                $(this).remove();
            }
        }
    });
    refreshData();

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
        console.log(response);
        if (response == "OK") {
            alert("Success");
            $("#nameP").val("");
            $("#nP").val("");
            $("#stepsP").val("");
            $("#stepData").val("");
            $("#proteinPDB").val("");
            refreshData();
        }
    }).error(function(response) {
        alert("Check your protein name and data, response: " + response);
    });
}
