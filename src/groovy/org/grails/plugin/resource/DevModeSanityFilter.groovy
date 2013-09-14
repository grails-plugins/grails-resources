package org.grails.plugin.resource
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.*
/**
 * This just traps any obvious mistakes the user has made and warns them in dev mode
 * 
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class DevModeSanityFilter implements Filter {
    static RELOADING_DOC = """
<html>
<head>
<meta http-equiv=\"refresh\" content="1"></meta>
<style type="text/css" media="screen">
    body {font-size:75%;color:#222;background:#fff;font-family:"Helvetica Neue", Arial, Helvetica, sans-serif; text-align: center;margin-top:200px}
    h1 {font-weight:normal;color:#111;}
</style>
</head>
<body>
<h1>Resources are being processed, please wait...</h1>
</body>
</html>"""
    
    def grailsResourceProcessor
    
    void init(FilterConfig config) throws ServletException {
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        grailsResourceProcessor = applicationContext.grailsResourceProcessor
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        boolean processReloading = false

        if (grailsResourceProcessor.reloading) {
            if (grailsResourceProcessor.isDebugMode(request) && request.getAttribute('resources.adhoc')) {
                processReloading = false //don't add reloading stub for adhoc resources in dev mode
            } else {
                processReloading = true
            }
        }

        if (processReloading) {
            response.contentType = "text/html"
            response.writer << RELOADING_DOC
        } else {
            chain.doFilter(request, response)

            if (request.getAttribute('resources.need.layout')) {
                def dispositionsLeftOver = grailsResourceProcessor.getRequestDispositionsRemaining(request)
                if (dispositionsLeftOver) {
                    def optionals = grailsResourceProcessor.optionalDispositions
                    dispositionsLeftOver -= optionals
                    if (dispositionsLeftOver) {
                        throw new RuntimeException("It looks like you are missing some calls to the r:layoutResources tag. "+
                            "After rendering your page the following have not been rendered: ${dispositionsLeftOver}")
                    }
                }
            }
        }
    }
}