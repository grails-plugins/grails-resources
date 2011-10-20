package org.grails.plugin.resource

import javax.servlet.*
import org.springframework.web.context.support.WebApplicationContextUtils
import grails.util.Environment

/**
 * This just traps any obvious mistakes the user has made and warns them in dev mode
 * 
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class DevModeSanityFilter implements Filter {
    def grailsResourceProcessor
    
    void init(FilterConfig config) throws ServletException {
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        grailsResourceProcessor = applicationContext.grailsResourceProcessor
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        println "In dev filter, chaining: ${request.requestURI}"
        chain.doFilter(request, response)
        println "In dev filter, back from chaining: ${request.requestURI}"


        println "In dev filter, checking: ${request.requestURI}"
        if (request.getAttribute('resources.need.layout')) {
            println "In dev filter: needed layout"
            def dispositionsLeftOver = grailsResourceProcessor.getRequestDispositionsRemaining(request)
            println "In dev filter remaining disp: ${dispositionsLeftOver}"
            if (dispositionsLeftOver) {
                def optionals = grailsResourceProcessor.optionalDispositions
                dispositionsLeftOver -= optionals
                println "In dev filter remaining disp minus optionals: ${dispositionsLeftOver}"
                if (dispositionsLeftOver) {
                    throw new RuntimeException("It looks like you are missing some calls to tag r:layoutResources. "+
                        "After rendering your view dispositions ${dispositionsLeftOver} are still pending.")
                }
            }
        }

    }
}