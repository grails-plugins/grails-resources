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
    def resourceService
    
    void init(FilterConfig config) throws ServletException {
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        resourceService = applicationContext.resourceService
        println "In dev filter init"
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        chain.doFilter(request, response)

        if (request.getAttribute('resources.need.layout')) {
            def dispositionsLeftOver = resourceService.getRequestDispositionsRemaining(request)
            if (dispositionsLeftOver) {
                throw new RuntimeException("It looks like you are missing some calls to tag r:layoutResources. "+
                    "After rendering your view dispositions ${dispositionsLeftOver} are still pending.")
            }
        }
    }
}