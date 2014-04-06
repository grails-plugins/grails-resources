package org.grails.plugin.resource

import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse

class LegacyResourceIntegrationSpec extends IntegrationSpec {
    
    ResourceProcessor grailsResourceProcessor
    GrailsApplication grailsApplication
    
    // GPRESOURCES-214
    def 'legacy resource with baseurl'() {
        grailsApplication.config.grails.resources.mappers.baseurl.enabled = true
        grailsApplication.config.grails.resources.mappers.baseurl.default = "http://cdn.domain.com/static"

        GrailsMockHttpServletRequest request = new GrailsMockHttpServletRequest()
        GrailsMockHttpServletResponse response = new GrailsMockHttpServletResponse()
        
        request.requestURI = "/images/springsource.png"
        
        when:
            grailsResourceProcessor.processLegacyResource(request, response)

        then:
            response.redirectedUrl == "http://cdn.domain.com/static/images/_springsource.png"
    }
    
}
