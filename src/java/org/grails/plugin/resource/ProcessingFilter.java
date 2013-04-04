package org.grails.plugin.resource;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Handles all static resource requests and delegates to the service to return them.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 */
public class ProcessingFilter implements Filter {

    private ResourceProcessor grailsResourceProcessor;
    private boolean adhoc;

    public void init(FilterConfig config) throws ServletException {
        adhoc = config.getInitParameter("adhoc").equals("true");

        WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        grailsResourceProcessor = (ResourceProcessor) applicationContext.getBean("grailsResourceProcessor");
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        boolean debugging = grailsResourceProcessor.isDebugMode(request);
        if (debugging) {
            request.setAttribute("resources.debug", debugging);
        }

        if (!debugging) {
            if (adhoc) {
                grailsResourceProcessor.processLegacyResource(request, response);
            } else {
                grailsResourceProcessor.processModernResource(request, response);
            }
        }

        if (!response.isCommitted()) {
            chain.doFilter(request, response);
        }
    }
}
