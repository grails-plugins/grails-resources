package org.grails.plugin.resource;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Handles all static resource requests and delegates to the service to return them.
 *
 * @author Marc Palmer (marc@grailsrocks.com)
 */
public class ProcessingFilter extends OncePerRequestFilter {

    private ResourceProcessor grailsResourceProcessor;
    private boolean adhoc;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

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

    public void setGrailsResourceProcessor(ResourceProcessor grailsResourceProcessor) {
        this.grailsResourceProcessor = grailsResourceProcessor;
    }

    public void setAdhoc(boolean adhoc) {
        this.adhoc = adhoc;
    }
}
