var refs, steppars;
let listAll = {};
let prepped = false;
let Qhalf = numeric.identity(4);
let grounds = [];
let grounds2 = [];
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
        method: "PUT",
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
      let config = {backgroundColor: 'white'};
      let element = document.querySelector('#glmolbox2');
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
        $.ajax({url: "/nucleic/pdb/d"+element+".pdb", async:false}).done(function(result) {
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
                 $.ajax({url: "/nucleic/pdb/"+element+".pdb", async:false}).done(function(result) {
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
    let vt1 = []; vt2 = [];
    for (j = 0; j < a.length; j++) {
      if (i == j) vt1.push(1.0); else vt1.push(0.0);
      vt2.push(a[i][j]);
    }
    x.push(vt2);
    v.push(vt1);
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


function calculateQhalf(fra) {
    let uscale = 5.0;
    let trace = fra[0][0]+fra[1][1]+fra[2][2];
    let q = [[fra[0][3]], [fra[1][3]], [fra[2][3]]];
    let a = [[fra[2][1]-fra[1][2]], [fra[0][2]-fra[2][0]], [fra[1][0]-fra[0][1]]];
    let u = numeric.mul(a, uscale*(2.0/(trace+1.0)));
    u = numeric.mul(u, 0.5/uscale);
    let v1 = numeric.dot(numeric.transpose(u), u)[0];
    let uhalf = numeric.mul(u, uscale*2.0/(1.0+Math.sqrt(1.0+v1)));
    u = numeric.mul(uhalf, 0.5/uscale);
    let uvec = numeric.identity(3);
    uvec[0][0] = 0.0;  uvec[1][1] = 0.0; uvec[2][2] = 0.0;
    uvec[0][1] = -u[0][2]; uvec[0][2] = u[0][1]; uvec[1][2] = -u[0][0];
    uvec = numeric.sub(uvec, numeric.transpose(uvec));

    v1 = numeric.dot(numeric.transpose(u), u)[0];
    let upuu = numeric.add(uvec, numeric.dot(uvec, uvec));

    return numeric.add(numeric.identity(3), numeric.mul(upuu, 2.0/(1.0+v1)));
}


function calculateFrame(ic, isphosphate = false) {
  let uscale = 5.0;
  let u = [[ic[0], ic[1], ic[2]]];
  let v = [[ic[3], ic[4], ic[5]]];
  // scale the coordinates
  u = numeric.mul(u, 0.5/uscale);
  // calculate skew-symmetric matrix related to u
  let uvec = numeric.identity(3); uvec[0][0] = 0.0;  uvec[1][1] = 0.0; uvec[2][2] = 0.0;
  uvec[0][1] = -u[0][2]; uvec[0][2] = u[0][1]; uvec[1][2] = -u[0][0];
  uvec = numeric.sub(uvec, numeric.transpose(uvec));

//  console.log(uvec);

  let v1 = numeric.dot(u, numeric.transpose(u))[0][0];

  let upuu = numeric.add(uvec, numeric.dot(uvec, uvec));

  upuu = numeric.mul(upuu, 2.0/(1.0 + v1));
  // calculate the rotation matrix that goes with u
  let Q = numeric.add(numeric.identity(3), upuu);

  // assign it as the 3x3 result portion of 4x4 SE(3) matrix
  let result = numeric.identity(4);
  for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) result[i][j] = Q[i][j];

  let uhalf = numeric.mul(u, uscale*(2.0/(1.0+Math.sqrt(1.0 + parseFloat(numeric.dot(u, numeric.transpose(u))[0])))));
  u = numeric.mul(uhalf, 0.5/uscale);

  uvec = numeric.identity(3);
  uvec[0][0] = 0.0;  uvec[1][1] = 0.0; uvec[2][2] = 0.0;
  uvec[0][1] = -u[0][2]; uvec[0][2] = u[0][1]; uvec[1][2] = -u[0][0];
  uvec = numeric.sub(uvec, numeric.transpose(uvec));

//  console.log(numeric.dot(u, numeric.transpose(u)));
  v1 = numeric.dot(u, numeric.transpose(u))[0][0];
  upuu = numeric.mul(upuu, 2.0/(1.0 + v1));

  Qhalf = numeric.add(numeric.identity(3), upuu);

  if (isphosphate) {
    let p__rot = [[0.28880532, -0.40811277, -0.8659639, 0.0],
                           [-0.50008344, 0.70707284, -0.50010651, 0.0],
                           [0.81639941, 0.57748763, 0.0, 0.0],
                           [0.0, 0.0, 0.0, 1.0]];
    result = numeric.dot(result, numeric.inv(p__rot));
    result[0][3] = v[0][0];
    result[1][3] = v[0][1];
    result[2][3] = v[0][2];
//    console.log(ic);
//    console.log(result);
    return result;
  }

  let q = numeric.dot(Qhalf, numeric.transpose(v));
 // console.log(q[0][0]);
  result[0][3] = q[0][0];
  result[1][3] = q[1][0];
  result[2][3] = q[2][0];

  return result;


}



function calculateFrameMID(ic, isphosphate = false) {
  let uscale = 5.0;
  let u = [[ic[0], ic[1], ic[2]]];
  let v = [[ic[3], ic[4], ic[5]]];
  // scale the coordinates
  u = numeric.mul(u, 0.5/uscale);
  // calculate skew-symmetric matrix related to u
  let uvec = numeric.identity(3); uvec[0][0] = 0.0;  uvec[1][1] = 0.0; uvec[2][2] = 0.0;
  uvec[0][1] = -u[0][2]; uvec[0][2] = u[0][1]; uvec[1][2] = -u[0][0];
  uvec = numeric.sub(uvec, numeric.transpose(uvec));

  let v1 = numeric.dot(u, numeric.transpose(u))[0][0];
  console.log(v1);

  let upuu = numeric.add(uvec, numeric.dot(uvec, uvec));
  // calculate the rotation matrix that goes with u
  let Q = numeric.add(numeric.identity(3), numeric.mul(2.0/(1.0+v1), upuu));
  console.log("Q");
  console.log(Q);

  // assign it as the 3x3 result portion of 4x4 SE(3) matrix
  let result = numeric.identity(4);
  for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) result[i][j] = Q[i][j];

  let uhalf = numeric.mul(u, uscale*(2.0/(1.0+Math.sqrt(1.0 + parseFloat(numeric.dot(u, numeric.transpose(u))[0])))));
  u = numeric.mul(uhalf, 0.5/uscale);

  uvec = numeric.identity(3);
  uvec[0][0] = 0.0;  uvec[1][1] = 0.0; uvec[2][2] = 0.0;
  uvec[0][1] = -u[0][2]; uvec[0][2] = u[0][1]; uvec[1][2] = -u[0][0];
  uvec = numeric.sub(uvec, numeric.transpose(uvec));

//  console.log(numeric.dot(u, numeric.transpose(u)));
  v1 = numeric.dot(u, numeric.transpose(u))[0][0];
  Qhalf = numeric.add(numeric.identity(3), numeric.mul(upuu, 2.0/(1.0+v1)));

  if (isphosphate) {
    let p__rot = [[0.28880532, -0.40811277, -0.8659639, 0.0],
                           [-0.50008344, 0.70707284, -0.50010651, 0.0],
                           [0.81639941, 0.57748763, 0.0, 0.0],
                           [0.0, 0.0, 0.0, 1.0]];
    result = numeric.dot(result, numeric.inv(p__rot));
    result[0][3] = v[0][0];
    result[1][3] = v[0][1];
    result[2][3] = v[0][2];
//    console.log(ic);
//    console.log(result);
    return result;
  }

  let q = numeric.dot(Qhalf, numeric.transpose(v));
 // console.log(q[0][0]);
  result[0][3] = q[0][0];
  result[1][3] = q[1][0];
  result[2][3] = q[2][0];

  return result;


}


function drawHelix() {
    get30PDB([$("#vs1").val(), $("#vs2").val(), $("#vs3").val(), $("#vs4").val(), $("#vs5").val(), $("#vs6").val(),
              $("#vs7").val(), $("#vs8").val(), $("#vs9").val(), $("#vs10").val(), $("#vs11").val(), $("#vs12").val(),
              $("#vs13").val(), $("#vs14").val(), $("#vs15").val(), $("#vs16").val(), $("#vs17").val(), $("#vs18").val(),
              $("#vs19").val(), $("#vs20").val(), $("#vs21").val(), $("#vs22").val(), $("#vs23").val(), $("#vs24").val(),
              $("#vs25").val(), $("#vs26").val(), $("#vs27").val(), $("#vs28").val(), $("#vs29").val(), $("#vs30").val()],
              $("#vsseq").val());
}


function get30PDB(ic, step, Ai) {

//    if (Ai === undefined)
//      let A = numeric.identity(4);
//    else
//      let A = Ai;
    let A = numeric.identity(4);
    let bfra = calculateFrame(ic.slice(0, 6));
    bfra[0][3] = bfra[0][3] / 2.0;
    bfra[1][3] = bfra[1][3] / 2.0;
    bfra[2][3] = bfra[2][3] / 2.0;

//console.log(calculateQhalf(calculateFrame(ic.slice(0,6))));
    for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
      bfra[i][j] = Qhalf[i][j];
    }

    let watson = numeric.dot(A, bfra)

 //   bfra = calculateFrame(ic.slice(0, 6));
//    bfra[0][3] = bfra[0][3] / 2.0;
//    bfra[1][3] = bfra[1][3] / 2.0;
//    bfra[2][3] = bfra[2][3] / 2.0;
    let crick = numeric.dot(A, numeric.inv(bfra));

    crick[0][1] *= -1; crick[1][1] *= -1; crick[2][1] *= -1; crick[0][2] *= -1; crick[1][2] *= -1; crick[2][2] *= -1;
    let phoC = numeric.dot(crick, calculateFrame(ic.slice(6, 12), true));

    A = numeric.dot(A, calculateFrameMID(ic.slice(12, 18)));
    console.log(ic.slice(12, 18));
    console.log(calculateFrame(ic.slice(12, 18)));

    bfra = calculateFrame(ic.slice(24, 30));
    bfra[0][3] = bfra[0][3] / 2.0;
    bfra[1][3] = bfra[1][3] / 2.0;
    bfra[2][3] = bfra[2][3] / 2.0;
    for (let i = 0; i < 3; i++) for (let j = 0; j < 3; j++) {
      bfra[i][j] = Qhalf[i][j];
    }
    let watson2 = numeric.dot(A, bfra);

//    bfra = calculateFrame(ic.slice(24, 30));
//    bfra[0][3] = bfra[0][3] / 2.0;
//    bfra[1][3] = bfra[1][3] / 2.0;
//    bfra[2][3] = bfra[2][3] / 2.0;

    let crick2 = numeric.dot(A, numeric.inv(bfra));

//    watson2[0][1] *= -1; watson2[1][1] *= -1; watson2[2][1] *= -1; watson2[0][2] *= -1; watson2[1][2] *= -1; watson2[2][2] *= -1;
    let phoW = numeric.dot(watson2, calculateFrame(ic.slice(18, 24), true));

// watson2[0][1] *= -1; watson2[1][1] *= -1; watson2[2][1] *= -1; watson2[0][2] *= -1; watson2[1][2] *= -1; watson2[2][2] *= -1;
        crick2[0][1] *= -1; crick2[1][1] *= -1; crick2[2][1] *= -1; crick2[0][2] *= -1; crick2[1][2] *= -1; crick2[2][2] *= -1;


//    console.log(watson);
//    console.log(crick);
//    console.log(A);
//    console.log(watson2);
//    console.log(crick2);
//    console.log(phoC);
//    console.log(phoW);

    let strW1 = step[0];
    let strW2 = step[1];

    let strC1 = complement(step[0], false);
    let strC2 = complement(step[1], false);

    let W1 = [];
    let W2 = [];
    let C1 = [];
    let C2 = [];
    let P1 = [];
    let P2 = [];

    console.log(strW1+strC1+ " " + strW2 + strC2);

    let pdb = "";

    $.ajax({url: "/pdb/" + strW1 +"b.pdb", async:false}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < lines.length; i++) W1.push(lines[i]);
    });
    $.ajax({url: "/pdb/" + strW2 +"b.pdb", async:false}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < lines.length; i++) W2.push(lines[i]);
    });
    $.ajax({url: "/pdb/" + strC1 +"b.pdb", async:false}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < lines.length; i++) C1.push(lines[i]);
    });
    $.ajax({url: "/pdb/" + strC2 +"b.pdb", async:false}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < lines.length; i++) C2.push(lines[i]);
    });
    $.ajax({url: "/pdb/pho.pdb", async:false}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < lines.length; i++) {
            P1.push(lines[i]);
            P2.push(lines[i]);
       }
    });

    let current = 1;
    let atomN = 1;
    [W1, C1, W2, C2, P1, P2].forEach(function(element) {
        let ref = [];
        if (current == 1) ref = watson;
        if (current == 2) ref = crick;
        if (current == 3) ref = watson2;
        if (current == 4) ref = crick2;
        if (current == 5) ref = phoC;
        if (current == 6) ref = phoW;
        for (let i = 0; i < element.length-1; i++) {
            let text = element[i];
            let numv=atomN+"";
            let numsize = numv.length;
            for (let k = 0; k < 6-numsize; k++) numv = " "+numv;
            let numr=current+"";
            numsize = numr.length;
            for (let k = 0; k < 4-numsize; k++) numr = " "+numr;
            let val = [0,0,0,1];
            val[0] = parseFloat(text.slice(30, 38));
            val[1] = parseFloat(text.slice(38, 46));
            val[2] = parseFloat(text.slice(46, 54));
            //console.log(JSON.stringify(val));
            let newxyz = new Array(3);
            newxyz[0] = ref[0][3]+ref[0][0]*val[0]+ref[0][1]*val[1]+ref[0][2]*val[2];
            newxyz[1] = ref[1][3]+ref[1][0]*val[0]+ref[1][1]*val[1]+ref[1][2]*val[2];
            newxyz[2] = ref[2][3]+ref[2][0]*val[0]+ref[2][1]*val[1]+ref[2][2]*val[2];
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
            pdb = pdb + text +"\n";
            atomN++
        }
        current++;
    });

    console.log(pdb);

    draw3(pdb);

}

function readGrounds() {

    $.ajax({url: "/t.txt"}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < 136; i++) {
         grounds.push(JSON.parse(lines[i]));
         console.log(tetramerSteps[i] + " " + grounds[i]);
         $("#tetramergrounds").append("<option value="+i+" onchange=\"drawGroundState("+i+")\">"+tetramerSteps[i]+"</option>");
       }
    });
    $.ajax({url: "/t2.txt"}).done(function(result) {
       let lines = result.split('\n');
       for (let i = 0; i < 136; i++) {
         grounds2.push(JSON.parse(lines[i]));
         console.log(tetramerSteps[i] + " " + grounds2[i]);
         $("#tetramergrounds2").append("<option value="+i+" onchange=\"drawGroundState2("+i+")\">"+tetramerSteps[i]+"</option>");
       }
    });
}

function drawGroundState(q) {

    let i = 0;
    if (q === undefined) i = $("#tetramergrounds").val();
    else i = q;
    let mv = grounds[i];
    get30PDB([mv[0], mv[1], mv[2], mv[3], mv[4], mv[5], mv[6], mv[7], mv[8], mv[9], mv[10], mv[11], mv[12], mv[13], mv[14],
    mv[15], mv[16], mv[17], mv[18], mv[19], mv[20], mv[21], mv[22], mv[23], mv[24], mv[25], mv[26], mv[27], mv[28], mv[28]],
    tetramerSteps[i][1]+tetramerSteps[i][2]);

}

function drawGroundState2(q) {

    let i = 0;
    if (q === undefined) i = $("#tetramergrounds2").val();
    else i = q;
    let mv = grounds2[i];
    get30PDB([mv[0], mv[1], mv[2], mv[3], mv[4], mv[5], mv[6], mv[7], mv[8], mv[9], mv[10], mv[11], mv[12], mv[13], mv[14],
    mv[15], mv[16], mv[17], mv[18], mv[19], mv[20], mv[21], mv[22], mv[23], mv[24], mv[25], mv[26], mv[27], mv[28], mv[28]],
    tetramerSteps[i][1]+tetramerSteps[i][2]);

}
