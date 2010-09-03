<html>
<head>
<g:set var="output">
<r:module name="jquery-ui"/>
<r:module name="blueprint"/>
<r:module name="main"/>
</g:set>

    ${output}
</head>
<body>
    <h1>This is the resource test page</h1>
    <p>It produced the following output in the head section</p>
    <pre>
        ${output.encodeAsHTML()}
    </pre>
</body>
</html>