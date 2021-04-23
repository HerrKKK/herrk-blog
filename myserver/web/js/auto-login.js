javascript:

function autoLogin() {
    var token = localStorage.getItem("token");
    if (null == token) {
        $.ajax({
            type: 'POST',
            url: 'usermanager',
            data: {
                action: 'notoken'
            },
            // dataType: 'JSON',
            async: 'true',
            error : function() {
                console.log('no token ajax fail');
            },
            success: function(result) {
                if (result == "failure") {
                    return;
                }
                // json string-->json object
                result = eval('(' + result + ')');
                $(".log").hide();
                $(".manager").show();
                localStorage.setItem("token", result.token);
            }
        });

        return;
    }
    $.ajax({
        type: 'POST',
        url: 'usermanager',
        data: {
            "action": 'token',
            "token":  token
        },
        dataType: 'json',
        async: 'true',
        error : function() {
            console.log('no token ajax fail');
        },
        success: function(result) {
            if (result.userId <= 0) {
                return;
            }
            $(".log").hide();
            $(".manager").show();
        }
    });
}

autoLogin();