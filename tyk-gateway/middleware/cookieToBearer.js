var cookieToBearer = new TykJS.TykMiddleware.NewMiddleware({});

function findAccessToken(cookieHeaders) {
    for (var i = 0; i < cookieHeaders.length; i++) {
        var parts = cookieHeaders[i].split(";");
        for (var j = 0; j < parts.length; j++) {
            var part = parts[j].trim();
            if (part.indexOf("access_token=") === 0) {
                var value = part.substring("access_token=".length);
                if (value !== "") {
                    return value;
                }
            }
        }
    }
    return null;
}

cookieToBearer.NewProcessRequest(function (request, session, spec) {
    var existing = request.Headers["Authorization"];
    if (existing && existing.length > 0 && existing[0] !== "") {
        return cookieToBearer.ReturnData(request, {});
    }

    var cookieHeaders = request.Headers["Cookie"];
    if (!cookieHeaders) {
        return cookieToBearer.ReturnData(request, {});
    }

    var token = findAccessToken(cookieHeaders);
    if (token !== null) {
        request.SetHeaders["Authorization"] = "Bearer " + token;
    }

    return cookieToBearer.ReturnData(request, {});
});
