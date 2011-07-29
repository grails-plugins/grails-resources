package org.grails.plugin.resource

import grails.test.*
import javax.servlet.FilterChain

import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest

class ProcessingFilterTests extends GroovyTestCase {
    void testResourceIsNotProcessedByBothFiltersIfHandledByFirst() {
        def filter = new ProcessingFilter()
        filter.adhoc = false
        filter.resourceService = [
            isDebugMode: { req -> false },
            processDeclaredResource: { req, resp -> resp.committed = true }
        ]

        def rq = new MockHttpServletRequest()
        def rp = new MockHttpServletResponse()
        
        def fakeChain = [
            doFilter: { req, resp -> fail('Second filter instance was called') }
        ] as FilterChain
        
        filter.doFilter(rq, rp, fakeChain)
    }
/*
void doFilter(ServletRequest request, ServletResponse response,
    FilterChain chain) throws IOException, ServletException {

    def debugging = resourceService.isDebugMode(request)
    request['resources.debug'] = debugging
    if (!debugging) {
        if (adhoc) {
            resourceService.processAdHocResource(request, response)
        } else {
            resourceService.processDeclaredResource(request, response)
        }
    }

    if (!response.committed) {
        chain.doFilter(request, response)
    }
}
*/
}