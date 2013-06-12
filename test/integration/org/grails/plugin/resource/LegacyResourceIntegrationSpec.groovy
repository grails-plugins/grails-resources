package org.grails.plugin.resource

import grails.plugin.spock.IntegrationSpec

import org.codehaus.groovy.grails.plugins.testing.*

class LegacyResourceIntegrationSpec extends IntegrationSpec {
    
    def grailsResourceProcessor
    def grailsApplication
    
    // GPRESOURCES-214
    def 'legacy resource with baseurl'() {
        
        grailsApplication.config.grails.resources.mappers.baseurl.enabled = true
        grailsApplication.config.grails.resources.mappers.baseurl.default = "http://cdn.domain.com/static"
        
        def request = new GrailsMockHttpServletRequest()
        def response = new GrailsMockHttpServletResponse()
        
        request.setRequestURI("/images/springsource.png")
        
        when:
            grailsResourceProcessor.processLegacyResource(request, response)
        then:
            response.redirectedUrl == "http://cdn.domain.com/static/images/springsource.png"
    }
    
}
