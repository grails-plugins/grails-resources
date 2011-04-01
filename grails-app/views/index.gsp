<html>
<head>
<g:set var="output">
<!-- icon tests -->
<r:resourceLink uri="/images/favicon.ico"/>
<r:resourceLink uri="/images/springsource.png" type="appleicon"/>

<r:script disposition="head">
    document.write("<title>This is a test</title>");
</r:script>

<!-- Here we pull in our smart links, and capture them just for sake of demo page output -->
<r:use modules="jquery-ui, jqueryGoogle"/>
<r:use module="app"/>
<!-- This tests resource duplication detection for explicit resourceLink -->
<r:resourceLink dir="css" file="main.css"/>
<!-- This tests resource tag enhancement for legacy usage -->
<r:resourceLink dir="css" file="legacy.css"/>
<!-- This tests ad hoc resource deferral -->
<r:resourceLink dir="js" file="core.js"/>
<r:layoutResources/>
</g:set>

    ${output}
</head>
<body>
    <div class="container">
        <div class="span-24 last">
            <h1>This is the resource test page</h1>
            <p>It produced the following output in the head section</p>
            <pre>
                ${output.encodeAsHTML()}
            </pre>
            <p>Grails logo using processing:<r:img uri="/images/grails_logo.png"/></p>
    
            <r:script>
                document.write("<p>This is going to come out in the footer<p>");
            </r:script>

            <p>Resource cache info:</p>
            <pre>${grailsApplication.mainContext.resourceService.dumpResources().encodeAsHTML()}</pre>

            <g:set var="deferred">
                <r:layoutResources/>
            </g:set>
            <p>It produced the following output in the footer</p>
            <pre>
                ${deferred.encodeAsHTML()}
            </pre>
        </div>
    </div>
    ${deferred}
</body>
</html>