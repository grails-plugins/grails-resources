package org.grails.plugin.resource

import javax.servlet.*
import org.springframework.web.context.support.WebApplicationContextUtils

class ProcessingFilter implements Filter {
    def resourceService
    
    boolean adhoc
    
    void init(FilterConfig config) throws ServletException {
        adhoc = config.getInitParameter('adhoc') == 'true'
        
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        resourceService = applicationContext.resourceService
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        System.out.println "In filter ${request.requestURI}"
/*        if (log.debugEnabled) {
            log.debug "Filter handling static content for ${request.requestURI}"
        }
*/
        if (adhoc) {
            resourceService.processAdHocResource(request, response)
        } else {
            resourceService.processDeclaredResource(request, response)
        }
        return // we've done it, don't call any other filters
    }
}