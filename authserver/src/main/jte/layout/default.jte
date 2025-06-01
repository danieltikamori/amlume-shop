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
</head>
<body class="bg-gray-100">
${content}
</body>
</html>
