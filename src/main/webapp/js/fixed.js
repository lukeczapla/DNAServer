var fixedn = 0;
var fixedSelected = false;
var useFixed = false;

function addMoreFixedProteins() {
    fixedn++;
    $('#fixedProteinTable tr:last').after(['<tr>',
                                 '<td>Select protein: </td>','<td><select id="fixed' + fixedn +'"></td>',
                                 '<td>placed at (position of first bp, starting with 1): <input id="fixedpos' + fixedn + '" type="number"></td>',
                                 '</tr>'].join('\n'));
    loadProteinList("#fixed"+fixedn);
}

function deleteLastFixedProtein() {
    if (fixedn > 0) {
        $('#fixedProteinTable tr:last').remove();
        fixedn--;
    }

}

function validateFixedData() {

}

function selectFixed() {
    useFixed = true;
}

function unselectFixed() {
    useFixed = false;
}


function evenlySpaced(dist) {
    let v = 1;
    for (let i = 0; i <= fixedn; i++) {
        $("#fixedpos"+i).val(v);
        v += dist;
    }
}