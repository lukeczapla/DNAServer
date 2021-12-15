var refs, steppars;
let listAll = {};
let prepped = false;
//pdbData({})
//getFrames

function unPrep() {
    prepped = false;
    listAll = {};
}

function selectAllBP() {
   $('#steprefs option').prop('selected', true);
}

function unselectAllBP() {
    $('#steprefs option').prop('selected', false);
}

function getReferenceFrames() {
    let pdbinput = {};
    let pdbtext = $("#pdbref").val();
    pdbinput.pdbs = pdbtext;
    let req = {
                headers: {
                    'Content-Type': 'application/json'
                },
                url: "/getrefframes",
                method: "POST",
                data: JSON.stringify(pdbinput)
             };
    $.ajax(req).done(function(data) {
        //console.log(data);
        refs = JSON.parse(data);
        steppars = generateStepParameters();
        $("#steprefs").empty();
        function fixed(key, val) {
            return val.toFixed ? Number(val.toFixed(3)) : val;
        }
        for (let i = 0; i < refs.length; i++) {
            if (i > 0)
                $("#steprefs").append("<option value="+i+">Middle = (" + refs[i][0][3].toFixed(3) + ", " + refs[i][1][3].toFixed(3) + ", " + refs[i][2][3].toFixed(3) +
                    ") STEP = " + JSON.stringify(steppars[i-1], fixed) + "</option>");
            else $("#steprefs").append("<option value="+i+">Middle = (" + refs[i][0][3].toFixed(3) + ", " + refs[i][1][3].toFixed(3) + ", " + refs[i][2][3].toFixed(3) + ")</option>");
        }
    });
}

function generateStepParameters() {
    let result = [];
    for (let i = 0; i < refs.length-1; i++) {
        result.push(calculatetp(numeric.dot(numeric.inv(refs[i]), refs[i+1])))
    }
    return result;
}

function generateBC() {

    let index1 = -1; let index2 = -1;
    $("#steprefs option").each(function () {
        if ($(this).is(':selected')) {
            if (index1 == -1) index1 = $(this).val();
            else index2 = $(this).val();
        }
        if (index2 < index1) {
            let tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
    });

    let data = [];
    let ref1 = refs[index1];
    let ref2 = refs[index2];
    if ($("#reverse1").is(":checked")) {
        console.log(JSON.stringify(ref1));
        ref1[0][1] *= -1.0; ref1[1][1] *= -1.0; ref1[2][1] *= -1.0;
        ref1[0][2] *= -1.0; ref1[1][2] *= -1.0; ref1[2][2] *= -1.0;
        console.log(JSON.stringify(ref1));

    }
    if ($("#reverse2").is(":checked")) {
        ref2[0][1] *= -1.0; ref2[1][1] *= -1.0; ref2[2][1] *= -1.0;
        ref2[0][2] *= -1.0; ref2[1][2] *= -1.0; ref2[2][2] *= -1.0;
    }
    data.push(ref1);
    data.push(ref2);
    steps = calculatetp(numeric.dot(numeric.inv(data[0]), data[1]));

       $("#bctilt").val(steps[0]);
       $("#bcroll").val(steps[1]);
       $("#bctwist").val(steps[2]);
       $("#bcshift").val(steps[3]);
       $("#bcslide").val(steps[4]);
       $("#bcrise").val(steps[5]);

        // put these back to normal!!!!
       if ($("#reverse1").is(":checked")) {
           console.log(JSON.stringify(ref1));
           ref1[0][1] *= -1.0; ref1[1][1] *= -1.0; ref1[2][1] *= -1.0;
           ref1[0][2] *= -1.0; ref1[1][2] *= -1.0; ref1[2][2] *= -1.0;
           console.log(JSON.stringify(ref1));

       }
       if ($("#reverse2").is(":checked")) {
           ref2[0][1] *= -1.0; ref2[1][1] *= -1.0; ref2[2][1] *= -1.0;
           ref2[0][2] *= -1.0; ref2[1][2] *= -1.0; ref2[2][2] *= -1.0;
       }

}


function addBC() {
    console.log("Adding " + $("#bcname").val());
    let bcdata = {
        name: $("#bcname").val(),
        tilt: $("#bctilt").val(),
        roll: $("#bcroll").val(),
        twist: $("#bctwist").val(),
        shift: $("#bcshift").val(),
        slide: $("#bcslide").val(),
        rise: $("#bcrise").val()
    };
    let req = {
        headers: {
            'Content-Type': 'application/json'
        },
        url: "/addbc",
        method: "POST",
        data: JSON.stringify(bcdata),
        success: function(result) {
            if (result == "OK") {
                $("#bcname").val("");
                $("#bctilt").val("");
                $("#bcroll").val("");
                $("#bctwist").val("");
                $("#bcshift").val("");
                $("#bcslide").val("");
                $("#bcrise").val("");
                refreshData();
            } else {
                alert("An error occurred, make sure you have given a unique name and correct numbers");
            }
            console.log(result);
        }
    };

    $.ajax(req);
}


// calculating back-and-forth between step parameters and 4x4 SE(3) matrices

function calculateA(tp) {

    let M = [[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,1]];
    let PI = Math.PI;

    let t1 = tp[0]*PI/180.0;
    let t2 = tp[1]*PI/180.0;
    let t3 = tp[2]*PI/180.0;

    let gamma = Math.sqrt(t1*t1+t2*t2);
    let phi = Math.atan2(t1,t2);
    let omega = t3;

    let sp = Math.sin(omega/2.0+phi);
    let cp = Math.cos(omega/2.0+phi);
    let sm = Math.sin(omega/2.0-phi);
    let cm = Math.cos(omega/2.0-phi);
    let sg = Math.sin(gamma);
    let cg = Math.cos(gamma);

    M[0][0] = cm*cg*cp-sm*sp;
    M[0][1] = -cm*cg*sp-sm*cp;
    M[0][2] = cm*sg;
    M[1][0] = sm*cg*cp+cm*sp;
    M[1][1] = -sm*cg*sp+cm*cp;
    M[1][2] = sm*sg;
    M[2][0] = -sg*cp;
    M[2][1] = sg*sp;
    M[2][2] = cg;

    sp = Math.sin(phi); cp = Math.cos(phi); sg = Math.sin(gamma/2.0); cg = Math.cos(gamma/2.0);

    M[0][3] = tp[3]*(cm*cg*cp-sm*sp) + tp[4]*(-cm*cg*sp-sm*cp) + tp[5]*(cm*sg);
    M[1][3] = tp[3]*(sm*cg*cp+cm*sp) + tp[4]*(-sm*cg*sp+cm*cp) + tp[5]*(sm*sg);
    M[2][3] = tp[3]*(-sg*cp) + tp[4]*(sg*sp) + tp[5]*(cg);

    return M;

}


function calculatetp(A) {

    M = [0,0,0,0,0,0];
    let PI = Math.PI;

    let cosgamma, gamma, phi, omega, sgcp, omega2_minus_phi,
            sm, cm, sp, cp, sg, cg;

    cosgamma = A[2][2];
    if (cosgamma > 1.0) cosgamma = 1.0;
    else if (cosgamma < -1.0) cosgamma = -1.0;

    gamma = Math.acos(cosgamma);

    sgcp = A[1][1]*A[0][2]-A[0][1]*A[1][2];

    if (gamma == 0.0) omega = -Math.atan2(A[0][1],A[1][1]);
    else omega = Math.atan2(A[2][1]*A[0][2]+sgcp*A[1][2],sgcp*A[0][2]-A[2][1]*A[1][2]);

    omega2_minus_phi = Math.atan2(A[1][2],A[0][2]);

    phi = omega/2.0 - omega2_minus_phi;

    M[0] = gamma*Math.sin(phi)*180.0/PI;
    M[1] = gamma*Math.cos(phi)*180.0/PI;
    M[2] = omega*180.0/PI;

    sm = Math.sin(omega/2.0-phi);
    cm = Math.cos(omega/2.0-phi);
    sp = Math.sin(phi);
    cp = Math.cos(phi);
    sg = Math.sin(gamma/2.0);
    cg = Math.cos(gamma/2.0);

    M[3] = (cm*cg*cp-sm*sp)*A[0][3]+(sm*cg*cp+cm*sp)*A[1][3]-sg*cp*A[2][3];
    M[4] = (-cm*cg*sp-sm*cp)*A[0][3]+(-sm*cg*sp+cm*cp)*A[1][3]+sg*sp*A[2][3];
    M[5] = (cm*sg)*A[0][3]+(sm*sg)*A[1][3]+cg*A[2][3];

    return M;

}


function translateProtein(ref, delNA) {

      //console.log(ref);
      let inverse = numeric.inv(ref);
      let config = {};
      let element = $('#glmolbox2');
      let viewer = $3Dmol.createViewer(element, config);
      let simmodel = viewer.addModel($("#pdbref").val(), "pdb");
      let frames = simmodel.getFrames();
      console.log(frames[0][0]);
      for (let i = 0; i < frames[0].length; i++) {
        let val = [0,0,0,1];
        val[0] = frames[0][i].x;
        val[1] = frames[0][i].y;
        val[2] = frames[0][i].z;
        let newxyz = numeric.dot(inverse, val);
        let num =[]
        num[0] = newxyz[0].toFixed(3);
        num[1] = newxyz[1].toFixed(3);
        num[2] = newxyz[2].toFixed(3);
        for (let j = 0; j < 3; j++) {
          let l = num[j].length;
          for (let k = 0; k < 8-l; k++)
            num[j] = " " + num[j];
        }
        frames[0][i].pdbline = frames[0][i].pdbline.slice(0,30) + num[0] + num[1] + num[2] + frames[0][i].pdbline.slice(54);
        frames[0][i].x = newxyz[0];
        frames[0][i].y = newxyz[1];
        frames[0][i].z = newxyz[2];
      }
//      viewer = $3Dmol.createViewer( element, config );
//      var m = viewer.addModel();
//      m.addAtoms(frames[0]);
      return viewer.pdbData();

}


function buildStructure(sequence, A, visview) {
    if (!prepped) {
    if (useRNA == false) ['A','T','G','C'].forEach(function(element) {
        $.ajax({url: "pdb/d"+element+".pdb", async:false}).done(function(result) {
           let lines = result.split('\n');
           let chainA = [], chainB = [];
           for (let i = 0; i < lines.length; i++) {
             if (lines[i].charAt(21) == 'A') {
               chainA.push(lines[i]);
             }
             if (lines[i].charAt(21) == 'B') {
               chainB.push(lines[i]);
             }
           }
           listAll[element] = [chainA, chainB];
        });
    });
    else ['A','U','G','C'].forEach(function(element) {
                 $.ajax({url: "pdb/"+element+".pdb", async:false}).done(function(result) {
                    let lines = result.split('\n');
                    let chainA = [], chainB = [];
                    for (let i = 0; i < lines.length; i++) {
                      if (lines[i].charAt(21) == 'A') {
                        chainA.push(lines[i]);
                      }
                      if (lines[i].charAt(21) == 'B') {
                        chainB.push(lines[i]);
                      }
                    }
                    listAll[element] = [chainA, chainB];
                 });
             });
    prepped = true;
    }

    let chainA = []; let chainB = [];
    let countA = 1, countB = 1;
    let resA = 1, resB = 1;
    for (let i = 0; i < A.length; i++) {
        let Am = A[i];
        let type;
        if (listAll[sequence.charAt(i)] === undefined) {
            type = listAll['A'];
        } else type = listAll[sequence.charAt(i)];
        for (let j = 0; j < type[0].length; j++) {
            let text = type[0][j];
            let numv=countA+"";
            let numsize = numv.length;
            for (let k = 0; k < 6-numsize; k++) numv = " "+numv;
            let numr=resA+"";
            numsize = numr.length;
            for (let k = 0; k < 4-numsize; k++) numr = " "+numr;
            let val = [0,0,0,1];
            val[0] = parseFloat(text.slice(30, 38));
            val[1] = parseFloat(text.slice(38, 46));
            val[2] = parseFloat(text.slice(46, 54));
            //console.log(JSON.stringify(val));
            let newxyz = new Array(3);
            newxyz[0] = Am[0][3]+Am[0][0]*val[0]+Am[0][1]*val[1]+Am[0][2]*val[2];
            newxyz[1] = Am[1][3]+Am[1][0]*val[0]+Am[1][1]*val[1]+Am[1][2]*val[2];
            newxyz[2] = Am[2][3]+Am[2][0]*val[0]+Am[2][1]*val[1]+Am[2][2]*val[2];
            let num = new Array(3);
            num[0] = newxyz[0].toFixed(2);
            num[1] = newxyz[1].toFixed(2);
            num[2] = newxyz[2].toFixed(2);
            for (let k = 0; k < 3; k++) {
              let lena = num[k].length;
              for (let l = 0; l < 8-lena; l++)
                num[k] = " " + num[k];
              if (num[k].length > 8) num[k] = num[k].slice(0,8);
            }
            text = text.slice(0, 5) + numv + text.slice(11);
            text = text.slice(0, 22) + numr + text.slice(26);
            text = text.slice(0, 30) + num[0] + text.slice(38);
            text = text.slice(0, 38) + num[1] + text.slice(46);
            text = text.slice(0, 46) + num[2] + text.slice(54);
            chainA.push(text);
            countA++;
        }
        resA++;
        for (let j = 0; j < type[1].length; j++) {
            let text = type[1][j];
            let numv=countB+"";
            let numsize = numv.length;
            for (let k = 0; k < 6-numsize; k++) numv = " "+numv;
            let numr=resB+"";
            numsize = numr.length;
            for (let k = 0; k < 4-numsize; k++) numr = " "+numr;
            let val = [0,0,0,1];
            val[0] = parseFloat(text.slice(30, 38));
            val[1] = parseFloat(text.slice(38, 46));
            val[2] = parseFloat(text.slice(46, 54));
            //console.log(JSON.stringify(val));
            let newxyz = new Array(3);
            newxyz[0] = Am[0][3]+Am[0][0]*val[0]+Am[0][1]*val[1]+Am[0][2]*val[2];
            newxyz[1] = Am[1][3]+Am[1][0]*val[0]+Am[1][1]*val[1]+Am[1][2]*val[2];
            newxyz[2] = Am[2][3]+Am[2][0]*val[0]+Am[2][1]*val[1]+Am[2][2]*val[2];
            //console.log(newxyz);
            let num = new Array(3);
            num[0] = newxyz[0].toFixed(2);
            num[1] = newxyz[1].toFixed(2);
            num[2] = newxyz[2].toFixed(2);
            for (let k = 0; k < 3; k++) {
              let lena = num[k].length;
              for (let l = 0; l < 8-lena; l++)
                num[k] = " " + num[k];
              if (num[k].length > 8) num[k] = num[k].slice(0,8);
            }
            text = text.slice(0, 5) + numv + text.slice(11);
            text = text.slice(0, 22) + numr + text.slice(26);
            text = text.slice(0, 30) + num[0] + text.slice(38);
            text = text.slice(0, 38) + num[1] + text.slice(46);
            text = text.slice(0, 46) + num[2] + text.slice(54);
            chainB.push(text);
            countB++;
        }
        resB++;
    }
    pdbresult = "";
    for (let i = 0; i < chainA.length; i++) {
        pdbresult += chainA[i]+"\n";
    }
    for (let i = 0; i < chainB.length; i++) {
        pdbresult += chainB[i]+"\n";
    }

    //let array = [pdbresult];
    //let blob = new Blob(array, {type:"text/plain;charset=utf-16"});
    //saveAs(blob, "test.pdb");
    draw2(pdbresult, visview);

}

function mmultiply(a, b) {
            if (a == null || b == null) return null;
            if (a[0].length != b.length) return null;
            let c = new Array(a.length);
            for (let i = 0; i < a.length; i++) {
                let v = [];
                for (let j = 0; j < b[0].length; j++) v.push(0.0);
                c.push(v);
            }
            for (let i = 0; i < a.length; i++) {
                for (let j = 0; j < b[0].length; j++) {
                    c[i][j] = 0.0;
                    for (let k = 0; k < a[0].length; k++) {
                        c[i][j] += a[i][k] * b[k][j];
                    }
                }
            }
            return c;
}

function doTranslate(nosave) {
        let i = -1;
        $("#steprefs option").each(function () {
            if ($(this).is(':selected')) {
                if (i == -1) i = $(this).val();
            }
        });
        if (i == -1) return;
        if ($("#reverseshift").is(":checked")) {
            refs[i][0][1] *= -1.0; refs[i][1][1] *= -1.0; refs[i][2][1] *= -1.0;
            refs[i][0][2] *= -1.0; refs[i][1][2] *= -1.0; refs[i][2][2] *= -1.0;
        }
        let newpdb = translateProtein(refs[i]);
        if (nosave === undefined) {
            let filename="translated.pdb";
            let blob = new Blob([newpdb], {type:"text/plain;charset=utf-16"});
            saveAs(blob, filename);
        }
        if ($("#reverseshift").is(":checked")) {
            refs[i][0][1] *= -1.0; refs[i][1][1] *= -1.0; refs[i][2][1] *= -1.0;
            refs[i][0][2] *= -1.0; refs[i][1][2] *= -1.0; refs[i][2][2] *= -1.0;
        }
        let stepcount = 0;
        let stepdat = "";
        $("#steprefs option").each(function () {
            if ($(this).is(':selected') && $(this).val() != i) {
                stepcount++;
                let idx = $(this).val();
                let tpval = calculatetp(numeric.dot(numeric.inv(refs[idx-1]), refs[idx]));
                stepdat += tpval[0] + " " + tpval[1] + " " + tpval[2] + " " + tpval[3] + " " + tpval[4] + " " + tpval[5] + "\n";
            }
        });
        stepdat = "<pre>" + stepcount + "\n" + stepdat + "</pre>";
        $("#stepparameterview").html(stepdat);
}


function jeigen(a) {

  if (a.length != a[0].length) return null;

  let ip, iq, i, j;
  let tresh, theta, tau, t, sm, s, h, g, c;

  let x = [];
  let v = [];
  //let v = a.slice();
  for (i = 0; i < a.length; i++) {
    v1 = []; v2 = [];
    for (j = 0; j < a.length; j++) {
      if (i == j) v1.push(1.0); else v1.push(0.0);
      v2.push(a[i][j]);
    }
    x.push(v2);
    v.push(v1);
  }
  //let x = numeric.eye(a.length);

  let b = [];
  let z = [];
  let d = [];
  for (i = 0; i < a.length; i++) {
    b.push(0.0);
    z.push(0.0);
    d.push(0.0);
  }

  tresh = 0.0;

  for (ip = 0; ip < a.length; ip++) {
    d[ip]=x[ip][ip]
    b[ip]=d[ip];
    z[ip]=0.0;
  }

  for (i = 0; i < 500; i++) {
    //  x.writematrix(stdout);
    sm = 0.0;
    for(ip=0; ip < a.length-1; ip++)
      for (iq=ip+1; iq < a.length; iq++) sm += Math.abs(x[ip][iq]);
    if (sm == 0.0) {
       result = {eigenvectors: v, eigenvalues: d};
       //console.log(JSON.stringify(result));
       return result;
    }

    for (ip = 0; ip < a.length-1; ip++) {
      for (iq = ip+1; iq < a.length; iq++) {
        g = 100.0*Math.abs(x[ip][iq]);
        if (Math.abs(x[ip][iq]) > tresh) {

          h = d[iq]-d[ip];

          if ((Math.abs(h)+g) == Math.abs(h)) {
	      if (h != 0.0)
  	        t = (x[ip][iq])/h;
          else t = 0.0;
	      } else {
            if (x[ip][iq] != 0.0) theta = 0.5*h/x[ip][iq];
	        else theta = 0.0;
            t = 1.0/(Math.abs(theta)+Math.sqrt(1.0+theta*theta));
            if (theta < 0.0) t = -t;
	      }

          c = 1.0/Math.sqrt(1.0+t*t);
          s = t*c;
          tau = s/(1.0+c);
          h = t*x[ip][iq];
          z[ip] -= h;
          z[iq] += h;
          d[ip] -= h;
          d[iq] += h;

          x[ip][iq] = 0.0;
	  for (j = 0; j <= ip-1; j++) {
            g=x[j][ip];
            h=x[j][iq];
            x[j][ip]=g-s*(h+g*tau);
            x[j][iq]=h+s*(g-h*tau);
	  }
          for (j = ip+1; j <= iq-1; j++) {
            g=x[ip][j];
            h=x[j][iq];
            x[ip][j]=g-s*(h+g*tau);
            x[j][iq]=h+s*(g-h*tau);
          }
          for (j = iq+1; j < a.length; j++) {
            g=x[ip][j];
            h=x[iq][j];
            x[ip][j]=g-s*(h+g*tau);
            x[iq][j]=h+s*(g-h*tau);
	      }
          for (j = 0; j < a.length; j++) {
	        g=v[j][ip];
            h=v[j][iq];
            v[j][ip]=g-s*(h+g*tau);
            v[j][iq]=h+s*(g-h*tau);
	      }

        }
      }
      }
      for (ip=0; ip < a.length; ip++) {
	    b[ip] += z[ip];
        d[ip] = b[ip];
	    z[ip] = 0.0;
      }
  }

  console.log("could not solve eigenvectors of matrix in 500 iterations");
  return null;

}
