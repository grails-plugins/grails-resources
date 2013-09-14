package org.grails.plugin.resource
import org.springframework.web.context.support.WebApplicationContextUtils

import javax.servlet.*
/**
 * This is the servlet filter that handles all static resource requests and delegates to the service
 * to return them.
 * 
 * @author Marc Palmer (marc@grailsrocks.com)
 */
class ProcessingFilter implements Filter {
    def grailsResourceProcessor
    
    boolean adhoc
    
    void init(FilterConfig config) throws ServletException {
        adhoc = config.getInitParameter('adhoc') == 'true'
        
        def applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.servletContext)
        grailsResourceProcessor = applicationContext.grailsResourceProcessor
    }

    void destroy() {
    }

    void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        def debugging = grailsResourceProcessor.isDebugMode(request)
        if (debugging) {
            request.setAttribute('resources.debug', debugging)
            if (adhoc) request.setAttribute('resources.adhoc', true)
        }
        if (!debugging) {
            if (adhoc) {
                grailsResourceProcessor.processLegacyResource(request, response)
            } else {
                grailsResourceProcessor.processModernResource(request, response)
            }
        }

        if (!response.committed) {
            chain.doFilter(request, response)
        }
    }
}