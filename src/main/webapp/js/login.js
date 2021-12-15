let loggedIn = false;
var google;

function isLoggedIn() {
    return loggedIn;
}

function onSignIn(googleUser) {

    let profile = googleUser.getBasicProfile();
    google = googleUser;

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
         method: 'GET',
         url: "/conf/user/logout",
         success: function(data) {
             console.log("logged out");
             if (google != null) google.disconnect();
             $('#authed').hide();
             $('#unauthed').show();
             loggedIn = false;
         }
     });
}