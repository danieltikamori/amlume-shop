@import me.amlu.authserver.view.helper.CsrfHiddenInput

@param gg.jte.Content content
<%-- Add as an optional parameter, defaulting to null--%>
@param CsrfHiddenInput csrfHiddenInput = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Amlume Authn</title>
    <%--    <script src="https://cdn.tailwindcss.com"></script>--%>
    <link href="/css/authserver-output.css" rel="stylesheet">

    @if(csrfHiddenInput != null && csrfHiddenInput.getCsrfToken() != null && csrfHiddenInput.getCsrfToken().getHeaderName() != null)
        <meta name="_csrf_header" content="${csrfHiddenInput.getCsrfToken().getHeaderName()}">
    @endif

    <!-- === FAVICON AND APP ICONS START === -->
    <%--    @* If you chose Option A (direct placement in /static/): *@--%>
    <%--    <link rel="icon" type="image/png" href="/favicon-96x96.png" sizes="96x96" />--%>
    <%--    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />--%>
    <%--    <link rel="shortcut icon" href="/favicon.ico" />--%>
    <%--    <link rel="apple-touch-icon" sizes="180x180" href="/apple-touch-icon.png" />--%>
    <%--    <meta name="apple-mobile-web-app-title" content="Amlume" />--%>
    <%--    <link rel="manifest" href="/site.webmanifest" />--%>

    <%--    @* If you chose Option B (placement in /static/favicons/): *@--%>
    <%--    @*--%>
    <link rel="icon" type="image/png" href="/favicons/favicon-96x96.png" sizes="96x96"/>
    <link rel="icon" type="image/svg+xml" href="/favicons/favicon.svg"/>
    <link rel="shortcut icon" href="/favicons/favicon.ico"/>
    <link rel="apple-touch-icon" sizes="180x180" href="/favicons/apple-touch-icon.png"/>
    <meta name="apple-mobile-web-app-title" content="Amlume"/>
    <link rel="manifest" href="/favicons/site.webmanifest"/>
    <%--    *@--%>
    <!-- === FAVICON AND APP ICONS END === -->

</head>
<body class="bg-gray-100">
${content}
</body>
</html>
