
let loggedIn = false;
//var google;

function isLoggedIn() {
    return loggedIn;
}
function parseJwt(token) {
    let base64Url = token.split('.')[1];
    let base64 = base64Url.replace(/-/g, "+").replace("/_/g", "/");
    let jsonPayload = decodeURIComponent(atob(base64).split("").map(function(c) {
        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
}

function onSignIn(response) {

    let userInfo = parseJwt(response.credential);
    let googleUser = {
        getBasicProfile: function() {
            return {
                getId: function() {
                    return userInfo.sub;
                },
                getName: function() {
                    return userInfo.name;
                },
                getGivenName: function() {
                    return userInfo.given_name;
                },
                getFamilyName: function() {
                    return userInfo.family_name;
                },
                getImageUrl: function() {
                    return userInfo.picture;
                },
                getEmail: function() {
                    return userInfo.email;
                }
            };
        },
        getAuthResponse: function() {
            return {
                id_token: response.credential
            };
        }
    };
    let profile = googleUser.getBasicProfile();
    let google = googleUser;

    let user = {"email": profile.getEmail(),
        "firstName": profile.getGivenName(),
        "lastName": profile.getFamilyName(),
        "socialId": profile.getId(),
        "tokenId" : googleUser.getAuthResponse().id_token};

        $('#unauthed').hide();
        $('#authed').show();
        if (profile.getImageUrl() != null && profile.getImageUrl().length > 1) {
            $('#imgbox').html('<img src="' + profile.getImageUrl() + '" width=20 height=20></img>');
        }
        //console.log("email received");

        $.ajax({
            headers: {
                "Content-Type": "application/json"
            },
            method: 'POST',
            url: '/conf/user',
            data: JSON.stringify(user)
        }).done(function(data) {
            console.log(data);
            if (!loggedIn) refreshData();
            loggedIn = true;
        }).error(function(data) {
            console.log("error: " + JSON.stringify(data));
        });

}


function registered() {
    $.ajax({method: "GET",
            url: "/conf/user"}).done(function(result) {
         if (result.authenticated) {
              $('#unauthed').hide();
              $('#authed').show();
              refreshData();
              loggedIn = true;
         } else {
            loggedIn = false;
            $('#authed').hide();
            $('#unauthed').show();
         }
    });
}



function signout() {
     $.ajax({
         method: 'DELETE',
         url: "/conf/user",
         success: function(data) {
             console.log("logged out");
             if (google != null) google.accounts.id.disableAutoSelect();
             $('#authed').hide();
             $('#unauthed').show();
             loggedIn = false;
         }
     });
}