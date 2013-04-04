package org.grails.plugin.resource

import javax.servlet.FilterChain

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ProcessingFilterTests extends GroovyTestCase {
    void testResourceIsNotProcessedByBothFiltersIfHandledByFirst() {
        def filter = new ProcessingFilter()
        filter.adhoc = false
        filter.grailsResourceProcessor = [
            isDebugMode: { req -> false },
            processModernResource: { req, resp -> resp.committed = true }
        ]

        def rq = new MockHttpServletRequest()
        def rp = new MockHttpServletResponse()

        def fakeChain = [
            doFilter: { req, resp -> fail('Second filter instance was called') }
        ] as FilterChain

        filter.doFilter(rq, rp, fakeChain)
    }
}
